package cmc.mody.auth.application.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.application.oauth.dto.OAuthMemberResult;
import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.member.domain.LoginType;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.SocialAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthMemberProcessorTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    @Captor
    private ArgumentCaptor<SocialAccount> socialAccountCaptor;

    @Test
    @DisplayName("메인 진입 조건을 만족하면 가능 상태를 반환한다.")
    void ensureExistingMember() {
        OAuthMemberProcessor processor = processor();
        OAuthProfile profile = new OAuthProfile(LoginType.KAKAO, "provider-1", null, "민석", null);
        given(socialAccountRepository.findByLoginTypeAndProviderUserId(LoginType.KAKAO, "provider-1"))
            .willReturn(Optional.of(new SocialAccount(10L, 1L, LoginType.KAKAO, "provider-1")));
        given(memberRepository.findById(1L))
            .willReturn(Optional.of(new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0))));
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(1L);

        OAuthMemberResult result = processor.ensure(profile);

        assertThat(result).isEqualTo(new OAuthMemberResult(1L, true, true));
        then(memberRepository).should().findById(1L);
    }

    @Test
    @DisplayName("참여 그룹이 없으면 메인 진입 불가 상태를 반환한다.")
    void ensureExistingMemberWithoutJoinedGroup() {
        OAuthMemberProcessor processor = processor();
        OAuthProfile profile = new OAuthProfile(LoginType.KAKAO, "provider-1", null, "민석", null);
        given(socialAccountRepository.findByLoginTypeAndProviderUserId(LoginType.KAKAO, "provider-1"))
            .willReturn(Optional.of(new SocialAccount(10L, 1L, LoginType.KAKAO, "provider-1")));
        given(memberRepository.findById(1L))
            .willReturn(Optional.of(new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0))));
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(0L);

        OAuthMemberResult result = processor.ensure(profile);

        assertThat(result).isEqualTo(new OAuthMemberResult(1L, true, false));
    }

    @Test
    @DisplayName("신규 소셜 계정이면 회원과 소셜 계정을 함께 생성한다.")
    void ensureNewMember() {
        OAuthMemberProcessor processor = processor();
        OAuthProfile profile = new OAuthProfile(
            LoginType.GOOGLE,
            "provider-2",
            "a@b.com",
            "긴닉네임입니다긴닉네임입니다",
            "image"
        );

        given(socialAccountRepository.findByLoginTypeAndProviderUserId(LoginType.GOOGLE, "provider-2"))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(1L, 2L);
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(socialAccountRepository.save(any(SocialAccount.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        OAuthMemberResult result = processor.ensure(profile);

        assertThat(result).isEqualTo(new OAuthMemberResult(1L, false, false));
        then(memberRepository).should().save(memberCaptor.capture());
        then(socialAccountRepository).should().save(socialAccountCaptor.capture());
        assertThat(memberCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(memberCaptor.getValue().getNickname()).hasSize(Member.MAX_NICKNAME_LENGTH);
        assertThat(socialAccountCaptor.getValue().getId()).isEqualTo(2L);
        assertThat(socialAccountCaptor.getValue().getMemberId()).isEqualTo(1L);
    }

    private OAuthMemberProcessor processor() {
        return new OAuthMemberProcessor(idGenerator, memberRepository, socialAccountRepository, groupMemberRepository);
    }
}
