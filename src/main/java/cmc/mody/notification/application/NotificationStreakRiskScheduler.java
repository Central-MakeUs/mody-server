package cmc.mody.notification.application;

import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.streak-risk", name = "enabled", havingValue = "true")
public class NotificationStreakRiskScheduler {
    private static final String FALLBACK_NICKNAME = "버디";

    private final NotificationStreakRiskProperties streakRiskProperties;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationRequestService notificationRequestService;

    @Scheduled(cron = "${notification.streak-risk.cron:0 0 21 * * *}")
    public void sendStreakRiskNotifications() {
        sendStreakRiskNotifications(LocalDate.now());
    }

    void sendStreakRiskNotifications(LocalDate date) {
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupMemberStatusAndDeletedAtIsNull(
            GroupMemberStatus.JOINED,
            PageRequest.of(0, streakRiskProperties.getBatchSize())
        );
        groupMembers.stream()
            .filter(this::isStreakNotificationEnabled)
            .filter(groupMember -> !hasRecord(groupMember, date))
            .forEach(groupMember -> notificationRequestService.requestGroupRecordStreakRisk(
                groupMember.getGroupId(),
                groupMember.getMemberId(),
                resolveNickname(groupMember),
                date.toString()
            ));
    }

    private boolean isStreakNotificationEnabled(GroupMember groupMember) {
        return notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(groupMember.getMemberId())
            .map(NotificationSetting::isStreakNotificationEnabled)
            .orElse(true);
    }

    private boolean hasRecord(GroupMember groupMember, LocalDate date) {
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();
        return activityRecordRepository.existsActiveRecordByMemberIdAndGroupIdBetween(
            groupMember.getMemberId(),
            groupMember.getGroupId(),
            startAt,
            endAt
        );
    }

    private String resolveNickname(GroupMember groupMember) {
        String nickname = groupMember.getDisplayNickname();
        if (nickname == null || nickname.isBlank()) {
            return FALLBACK_NICKNAME;
        }
        return nickname;
    }
}
