package cmc.mody.auth.application.oauth;

import cmc.mody.auth.application.oauth.dto.OAuthMemberResult;
import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthMemberProcessor {
    private static final int FALLBACK_NICKNAME_SUFFIX_LENGTH = 10;

    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public OAuthMemberResult ensure(OAuthProfile profile) {
        validate(profile);
        return socialAccountRepository.findByLoginTypeAndProviderUserIdAndDeletedAtIsNull(
                profile.loginType(),
                profile.providerUserId()
            )
            .map(account -> buildExistingMemberResult(account.getMemberId(), profile))
            .orElseGet(() -> createMember(profile));
    }

    private OAuthMemberResult buildExistingMemberResult(Long memberId, OAuthProfile profile) {
        Member member = getMember(memberId);
        member.updateProfileImage(profile.profileImageUrl());
        boolean personalInfoCompleted = member.isPersonalInfoCompleted();
        boolean mainAccessible = personalInfoCompleted && hasJoinedGroup(memberId);
        return new OAuthMemberResult(
            memberId,
            personalInfoCompleted,
            mainAccessible,
            member.isGroupOnboardingCompleted()
        );
    }

    private OAuthMemberResult createMember(OAuthProfile profile) {
        Long memberId = idGenerator.nextId();
        Member member = Member.oauthMember(
            memberId,
            resolveNickname(profile, memberId),
            profile.profileImageUrl()
        );
        memberRepository.save(member);
        socialAccountRepository.save(new SocialAccount(
            idGenerator.nextId(),
            memberId,
            profile.loginType(),
            profile.providerUserId()
        ));
        return new OAuthMemberResult(memberId, false, false, false);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private boolean hasJoinedGroup(Long memberId) {
        return groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            GroupMemberStatus.JOINED
        ) > 0;
    }

    private String resolveNickname(OAuthProfile profile, Long memberId) {
        if (profile.nickname() != null && !profile.nickname().isBlank()) {
            return profile.nickname().length() > Member.MAX_NICKNAME_LENGTH
                ? profile.nickname().substring(0, Member.MAX_NICKNAME_LENGTH)
                : profile.nickname();
        }
        String suffix = String.valueOf(memberId);
        if (suffix.length() > FALLBACK_NICKNAME_SUFFIX_LENGTH) {
            suffix = suffix.substring(suffix.length() - FALLBACK_NICKNAME_SUFFIX_LENGTH);
        }
        return "mody" + suffix;
    }

    private void validate(OAuthProfile profile) {
        if (profile == null || profile.loginType() == null || profile.providerUserId() == null
            || profile.providerUserId().isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE);
        }
    }
}
