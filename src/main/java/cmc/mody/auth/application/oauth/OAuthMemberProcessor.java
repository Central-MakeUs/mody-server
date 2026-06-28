package cmc.mody.auth.application.oauth;

import cmc.mody.auth.application.oauth.dto.OAuthMemberResult;
import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
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

    @Transactional
    public OAuthMemberResult ensure(OAuthProfile profile) {
        validate(profile);
        return socialAccountRepository.findByLoginTypeAndProviderUserId(
                profile.loginType(),
                profile.providerUserId()
            )
            .map(account -> new OAuthMemberResult(
                account.getMemberId(),
                isPersonalInfoCompleted(account.getMemberId())
            ))
            .orElseGet(() -> createMember(profile));
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
        return new OAuthMemberResult(memberId, false);
    }

    private boolean isPersonalInfoCompleted(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        return member.isPersonalInfoCompleted();
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
