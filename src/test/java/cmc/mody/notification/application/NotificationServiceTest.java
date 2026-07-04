package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationStatus;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("내 알림 목록을 최신순으로 조회한다.")
    void getNotifications() {
        NotificationService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationRepository.findByReceiverMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(1L))
            .willReturn(List.of(new Notification(
                10L,
                1L,
                NotificationType.COMMENT,
                "새 댓글",
                "친구가 기록에 댓글을 남겼어요."
            )));

        NotificationService.NotificationListResult result = service.getNotifications(1L);

        assertThat(result.notifications()).hasSize(1);
        assertThat(result.notifications().get(0).notificationId()).isEqualTo(10L);
        assertThat(result.notifications().get(0).read()).isFalse();
    }

    @Test
    @DisplayName("내 알림을 읽음 처리한다.")
    void readNotification() {
        NotificationService service = service();
        Notification notification = new Notification(
            10L,
            1L,
            NotificationType.COMMENT,
            "새 댓글",
            "친구가 기록에 댓글을 남겼어요."
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));

        service.readNotification(1L, 10L);

        assertThat(notification.getNotificationStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 회원 알림은 읽음 처리할 수 없다.")
    void readOtherMemberNotification() {
        NotificationService service = service();
        Notification notification = new Notification(
            10L,
            2L,
            NotificationType.COMMENT,
            "새 댓글",
            "친구가 기록에 댓글을 남겼어요."
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.readNotification(1L, 10L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.NOTIFICATION_NOT_FOUND));
    }

    @Test
    @DisplayName("회원이 없으면 알림을 조회할 수 없다.")
    void getNotificationsMemberNotFound() {
        NotificationService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNotifications(1L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private NotificationService service() {
        return new NotificationService(memberRepository, notificationRepository);
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }
}
