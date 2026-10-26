package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.notification.application.event.NotificationRequestedEvent;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientResolverTest {
    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("댓글 작성자가 기록 작성자이면 댓글 알림 수신자를 만들지 않는다.")
    void resolveCommentAuthorSelf() {
        NotificationRecipientResolver resolver = resolver();
        given(activityRecordRepository.findById(10L)).willReturn(Optional.of(record(10L, 1L)));

        List<Long> receivers = resolver.resolve(NotificationRequestedEvent.immediate(
            NotificationType.COMMENT_CREATED,
            Map.of(
                "recordId", 10L,
                "commenterMemberId", 1L
            ),
            "RECORD",
            10L
        ));

        assertThat(receivers).isEmpty();
    }

    @Test
    @DisplayName("댓글 알림은 기록 작성자를 수신자로 결정한다.")
    void resolveCommentReceiver() {
        NotificationRecipientResolver resolver = resolver();
        given(activityRecordRepository.findById(10L)).willReturn(Optional.of(record(10L, 1L)));
        given(notificationSettingRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByIdDesc(1L))
            .willReturn(Optional.empty());

        List<Long> receivers = resolver.resolve(NotificationRequestedEvent.immediate(
            NotificationType.COMMENT_CREATED,
            Map.of(
                "recordId", 10L,
                "commenterMemberId", 2L
            ),
            "RECORD",
            10L
        ));

        assertThat(receivers).containsExactly(1L);
    }

    @Test
    @DisplayName("버디 기록 알림은 작성자와 챌린지 알림을 끈 그룹원을 제외한다.")
    void resolveBuddyRecordReceivers() {
        NotificationRecipientResolver resolver = resolver();
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            100L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            groupMember(1L),
            groupMember(2L),
            groupMember(3L)
        ));
        NotificationSetting challengeOff = new NotificationSetting(30L, 3L);
        challengeOff.updateReminderFlags(true, true, true, false);
        given(notificationSettingRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByIdDesc(2L))
            .willReturn(Optional.empty());
        given(notificationSettingRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByIdDesc(3L))
            .willReturn(Optional.of(challengeOff));

        List<Long> receivers = resolver.resolve(NotificationRequestedEvent.immediate(
            NotificationType.BUDDY_MEAL_RECORDED,
            Map.of(
                "groupId", 100L,
                "recordId", 10L,
                "writerMemberId", 1L,
                "nickname", "민석"
            ),
            "RECORD",
            10L
        ));

        assertThat(receivers).containsExactly(2L);
    }

    private GroupMember groupMember(Long memberId) {
        return new GroupMember(memberId + 20, memberId, 100L, LocalDateTime.of(2026, 7, 1, 0, 0));
    }

    private NotificationRecipientResolver resolver() {
        return new NotificationRecipientResolver(
            activityRecordRepository,
            groupMemberRepository,
            notificationSettingRepository
        );
    }

    private ActivityRecord record(Long recordId, Long memberId) {
        return ActivityRecord.meal(
            recordId,
            memberId,
            100L,
            LocalTime.of(12, 30),
            "샐러드",
            "records/10.jpg",
            LocalDateTime.of(2026, 7, 4, 12, 30)
        );
    }
}
