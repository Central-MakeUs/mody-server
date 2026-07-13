package cmc.mody.notification.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationStreakRiskSchedulerTest {
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private NotificationRequestService notificationRequestService;

    @Test
    @DisplayName("오늘 그룹 기록이 없는 회원에게만 연속 기록 위험 알림 이벤트를 생성한다.")
    void sendStreakRiskNotifications() {
        NotificationStreakRiskScheduler scheduler = scheduler();
        LocalDate date = LocalDate.of(2026, 7, 4);
        GroupMember unrecordedMember = groupMember(1L, 10L, "민석");
        GroupMember recordedMember = groupMember(2L, 10L, "친구");
        given(groupMemberRepository.findByGroupMemberStatusAndDeletedAtIsNull(
            org.mockito.ArgumentMatchers.eq(GroupMemberStatus.JOINED),
            any(Pageable.class)
        )).willReturn(List.of(unrecordedMember, recordedMember));
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(2L)).willReturn(Optional.empty());
        given(activityRecordRepository.existsActiveRecordByMemberIdAndGroupIdBetween(
            1L,
            10L,
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay()
        )).willReturn(false);
        given(activityRecordRepository.existsActiveRecordByMemberIdAndGroupIdBetween(
            2L,
            10L,
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay()
        )).willReturn(true);

        scheduler.sendStreakRiskNotifications(date);

        verify(notificationRequestService).requestGroupRecordStreakRisk(10L, 1L, "민석", "2026-07-04");
        verify(notificationRequestService, never()).requestGroupRecordStreakRisk(10L, 2L, "친구", "2026-07-04");
    }

    private NotificationStreakRiskScheduler scheduler() {
        NotificationStreakRiskProperties properties = new NotificationStreakRiskProperties();
        properties.setBatchSize(500);
        return new NotificationStreakRiskScheduler(
            properties,
            groupMemberRepository,
            activityRecordRepository,
            notificationSettingRepository,
            notificationRequestService
        );
    }

    private GroupMember groupMember(Long memberId, Long groupId, String nickname) {
        return new GroupMember(100L + memberId, memberId, groupId, nickname, null, LocalDateTime.now());
    }
}
