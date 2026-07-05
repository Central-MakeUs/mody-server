package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.notification.application.event.NotificationRequestedEvent;
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
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

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
