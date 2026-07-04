package cmc.mody.notification.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResult getNotifications(Long memberId) {
        getMember(memberId);
        List<NotificationResult> notifications = notificationRepository
            .findByReceiverMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(memberId)
            .stream()
            .map(NotificationResult::from)
            .toList();
        return new NotificationListResult(notifications);
    }

    @Transactional
    public void readNotification(Long memberId, Long notificationId) {
        getMember(memberId);
        Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(notificationId)
            .filter(found -> found.getReceiverMemberId().equals(memberId))
            .orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));
        notification.markAsRead(LocalDateTime.now());
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    public record NotificationListResult(List<NotificationResult> notifications) {
    }

    public record NotificationResult(
        Long notificationId,
        NotificationType type,
        String title,
        String description,
        LocalDateTime createdAt,
        boolean read
    ) {
        public static NotificationResult from(Notification notification) {
            return new NotificationResult(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getCreatedAt(),
                notification.isRead()
            );
        }
    }
}
