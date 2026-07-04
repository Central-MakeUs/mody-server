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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResult getNotifications(Long memberId, Long cursor, int size) {
        getMember(memberId);
        int pageSize = normalizeSize(size);
        List<Notification> notifications = notificationRepository
            .findByReceiverMemberIdByCursor(memberId, cursor, PageRequest.of(0, pageSize + 1));
        boolean hasNext = notifications.size() > pageSize;
        List<Notification> pageNotifications = hasNext ? notifications.subList(0, pageSize) : notifications;
        List<NotificationResult> results = pageNotifications
            .stream()
            .map(NotificationResult::from)
            .toList();
        Long nextCursor = hasNext ? pageNotifications.get(pageNotifications.size() - 1).getId() : null;
        return new NotificationListResult(results, nextCursor, hasNext);
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

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public record NotificationListResult(List<NotificationResult> notifications, Long nextCursor, boolean hasNext) {
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
