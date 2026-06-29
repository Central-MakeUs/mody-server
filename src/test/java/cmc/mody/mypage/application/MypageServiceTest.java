package cmc.mody.mypage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.member.domain.LoginType;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.member.domain.WeightRecord;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.SocialAccountRepository;
import cmc.mody.member.infrastructure.repository.WeightRecordRepository;
import cmc.mody.mypage.application.MypageService.ProfileResult;
import cmc.mody.mypage.application.MypageService.ProfileUpdateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MypageServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private WeightRecordRepository weightRecordRepository;

    @Captor
    private ArgumentCaptor<WeightRecord> weightRecordCaptor;

    @Test
    @DisplayName("프로필 조회 시 회원과 소셜 로그인 타입을 반환한다.")
    void getProfile() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(socialAccountRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
            .willReturn(Optional.of(new SocialAccount(10L, 1L, LoginType.KAKAO, "provider-id")));

        ProfileResult result = service.getProfile(1L);

        assertThat(result.loginType()).isEqualTo("KAKAO");
        assertThat(result.name()).isEqualTo("민석");
        assertThat(result.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    @DisplayName("소셜 계정이 없으면 프로필을 조회할 수 없다.")
    void getProfileWithoutSocialAccount() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(socialAccountRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(1L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.MYPAGE_SOCIAL_ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("프로필 수정 시 닉네임과 생년월일을 변경한다.")
    void updateProfile() {
        MypageService service = service();
        Member member = member();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        service.updateProfile(1L, new ProfileUpdateCommand("수정", LocalDate.of(1999, 12, 31)));

        assertThat(member.getNickname()).isEqualTo("수정");
        assertThat(member.getBirthDate()).isEqualTo(LocalDate.of(1999, 12, 31));
    }

    @Test
    @DisplayName("체중 목록은 최신 기록 순서로 반환한다.")
    void getWeightHistory() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(List.of(
                new WeightRecord(11L, 1L, LocalDate.of(2026, 6, 28), new BigDecimal("72.50"), new BigDecimal("-0.50")),
                new WeightRecord(10L, 1L, LocalDate.of(2026, 6, 27), new BigDecimal("73.00"), BigDecimal.ZERO)
            ));

        MypageService.WeightHistoryResult result = service.getWeightHistory(1L);

        assertThat(result.weights()).hasSize(2);
        assertThat(result.weights().get(0).weightRecordId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("체중 추가 시 이전 기록 대비 증감을 저장한다.")
    void createWeight() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.of(
                new WeightRecord(10L, 1L, LocalDate.now().minusDays(1), new BigDecimal("73.00"), BigDecimal.ZERO)
            ));
        given(idGenerator.nextId()).willReturn(11L);
        given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

        WeightCreateResult result = service.createWeight(1L, new WeightCreateCommand(new BigDecimal("72.50")));

        assertThat(result.changeFromPreviousKg()).isEqualByComparingTo(new BigDecimal("-0.50"));
        then(weightRecordRepository).should().save(weightRecordCaptor.capture());
        assertThat(weightRecordCaptor.getValue().getChangeFromPreviousKg()).isEqualByComparingTo("-0.50");
    }

    @Test
    @DisplayName("이전 체중 기록이 없으면 증감 값은 0이다.")
    void createWeightWithoutPreviousRecord() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(10L);
        given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

        WeightCreateResult result = service.createWeight(1L, new WeightCreateCommand(new BigDecimal("72.50")));

        assertThat(result.changeFromPreviousKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private MypageService service() {
        return new MypageService(idGenerator, memberRepository, socialAccountRepository, weightRecordRepository);
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }
}
