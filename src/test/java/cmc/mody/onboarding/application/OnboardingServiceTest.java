package cmc.mody.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.member.domain.HealthConnectionStatus;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.WeightRecord;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.WeightRecordRepository;
import cmc.mody.notification.application.NotificationPreferenceService;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.onboarding.application.OnboardingService.ExerciseScheduleCommand;
import cmc.mody.onboarding.application.OnboardingService.HealthConnectionCommand;
import cmc.mody.onboarding.application.OnboardingService.MealScheduleCommand;
import cmc.mody.onboarding.application.OnboardingService.NotificationSetupCommand;
import cmc.mody.onboarding.application.OnboardingService.ProfileSetupCommand;
import cmc.mody.onboarding.application.OnboardingService.ProfileSetupResult;
import cmc.mody.onboarding.application.OnboardingService.WeightSetupCommand;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private WeightRecordRepository weightRecordRepository;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @Test
    void setupProfile() {
        OnboardingService service = service();
        Member member = Member.oauthMember(1L, "temp", null);
        ProfileSetupCommand command = command();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(idGenerator.nextId()).willReturn(10L);
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.empty());
        given(weightRecordRepository.save(any(WeightRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(notificationPreferenceService.saveInitialNotificationSetting(eq(1L), eq(true), any(), eq(true), eq(null)))
            .willReturn(new NotificationSetting(11L, 1L));

        ProfileSetupResult result = service.setupProfile(1L, command);

        assertThat(result).isEqualTo(new ProfileSetupResult(1L, 10L, true));
        assertThat(member.getNickname()).isEqualTo("민석");
        assertThat(member.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(member.getTargetWeightKg()).isEqualByComparingTo("68.0");

        ArgumentCaptor<WeightRecord> weightCaptor = ArgumentCaptor.forClass(WeightRecord.class);
        then(weightRecordRepository).should().save(weightCaptor.capture());
        assertThat(weightCaptor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(weightCaptor.getValue().getWeightKg()).isEqualByComparingTo("72.5");
        assertThat(weightCaptor.getValue().getChangeFromPreviousKg()).isEqualByComparingTo(BigDecimal.ZERO);

        then(notificationPreferenceService).should()
            .saveInitialNotificationSetting(eq(1L), eq(true), any(), eq(true), eq(null));
        then(notificationPreferenceService).should()
            .saveInitialExerciseSchedules(eq(1L), any());
    }

    @Test
    void setupProfileWithPreviousWeight() {
        OnboardingService service = service();
        Member member = Member.oauthMember(1L, "temp", null);
        WeightRecord previous = new WeightRecord(
            9L,
            1L,
            LocalDate.now().minusDays(1),
            BigDecimal.valueOf(73.2),
            BigDecimal.ZERO
        );

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(idGenerator.nextId()).willReturn(10L);
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.of(previous));
        given(weightRecordRepository.save(any(WeightRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(notificationPreferenceService.saveInitialNotificationSetting(eq(1L), eq(true), any(), eq(true), eq(null)))
            .willReturn(new NotificationSetting(11L, 1L));

        service.setupProfile(1L, command());

        ArgumentCaptor<WeightRecord> weightCaptor = ArgumentCaptor.forClass(WeightRecord.class);
        then(weightRecordRepository).should().save(weightCaptor.capture());
        assertThat(weightCaptor.getValue().getChangeFromPreviousKg()).isEqualByComparingTo("-0.7");
    }

    @Test
    void setupWeight() {
        OnboardingService service = service();
        Member member = Member.oauthMember(1L, "temp", null);
        WeightRecord previous = new WeightRecord(
            9L,
            1L,
            LocalDate.now().minusDays(1),
            BigDecimal.valueOf(73.2),
            BigDecimal.ZERO
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.of(previous));
        given(idGenerator.nextId()).willReturn(10L);
        given(weightRecordRepository.save(any(WeightRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        OnboardingService.WeightSetupResult result = service.setupWeight(
            1L,
            new WeightSetupCommand(BigDecimal.valueOf(72.5), BigDecimal.valueOf(68.0))
        );

        assertThat(result.weightRecordId()).isEqualTo(10L);
        assertThat(result.weightKg()).isEqualByComparingTo("72.5");
        assertThat(result.changeFromPreviousKg()).isEqualByComparingTo("-0.7");
        assertThat(member.getTargetWeightKg()).isEqualByComparingTo("68.0");
    }

    @Test
    void setupNotifications() {
        OnboardingService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(Member.oauthMember(1L, "temp", null)));
        given(notificationPreferenceService.saveInitialNotificationSetting(eq(1L), eq(true), any(), eq(true), any()))
            .willReturn(new NotificationSetting(20L, 1L));

        OnboardingService.NotificationSetupResult result = service.setupNotifications(
            1L,
            new NotificationSetupCommand(
                true,
                List.of(
                    new MealScheduleCommand(MealType.BREAKFAST, LocalTime.of(8, 0), false),
                    new MealScheduleCommand(MealType.LUNCH, null, true),
                    new MealScheduleCommand(MealType.DINNER, LocalTime.of(18, 0), false)
                ),
                true,
                LocalTime.of(20, 0)
            )
        );

        assertThat(result).isEqualTo(new OnboardingService.NotificationSetupResult(20L, true));
        then(notificationPreferenceService).should()
            .saveInitialNotificationSetting(eq(1L), eq(true), any(), eq(true), eq(LocalTime.of(20, 0)));
    }

    @Test
    void updateHealthConnection() {
        OnboardingService service = service();
        Member member = Member.oauthMember(1L, "temp", null);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        OnboardingService.HealthConnectionResult result = service.updateHealthConnection(
            1L,
            new HealthConnectionCommand(true)
        );

        assertThat(result.connected()).isTrue();
        assertThat(member.getHealthConnectionStatus()).isEqualTo(HealthConnectionStatus.CONNECTED);
    }

    @Test
    void throwMemberNotFound() {
        OnboardingService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setupProfile(1L, command()))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.MEMBER_NOT_FOUND);
    }

    @Test
    void throwAlreadyCompleted() {
        OnboardingService service = service();
        Member member = new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> service.setupProfile(1L, command()))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.MEMBER_PROFILE_ALREADY_COMPLETED);
    }

    private OnboardingService service() {
        return new OnboardingService(
            idGenerator,
            memberRepository,
            weightRecordRepository,
            notificationPreferenceService
        );
    }

    private ProfileSetupCommand command() {
        return new ProfileSetupCommand(
            "민석",
            LocalDate.of(2000, 1, 1),
            BigDecimal.valueOf(72.5),
            BigDecimal.valueOf(68.0),
            List.of(
                new MealScheduleCommand(MealType.BREAKFAST, LocalTime.of(8, 0), false),
                new MealScheduleCommand(MealType.LUNCH, null, true),
                new MealScheduleCommand(MealType.DINNER, LocalTime.of(18, 0), false)
            ),
            List.of(
                new ExerciseScheduleCommand(DayOfWeek.MONDAY, LocalTime.of(7, 30)),
                new ExerciseScheduleCommand(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0)),
                new ExerciseScheduleCommand(DayOfWeek.FRIDAY, LocalTime.of(9, 0))
            )
        );
    }
}
