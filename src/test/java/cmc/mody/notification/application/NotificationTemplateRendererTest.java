package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.notification.domain.NotificationType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTemplateRendererTest {
    private final NotificationTemplateRenderer renderer = new NotificationTemplateRenderer();

    @Test
    @DisplayName("확정된 그룹 참여 알림 템플릿을 렌더링한다.")
    void renderGroupMemberJoinedTemplate() {
        NotificationTemplate template = renderer.render(NotificationType.GROUP_MEMBER_JOINED, Map.of(
            "groupName", "모디그룹",
            "nickname", "민석"
        ));

        assertThat(template.title()).isEqualTo("모디그룹에 새 버디가 참여했어요!");
        assertThat(template.content()).isEqualTo("민석님을 환영해주세요.");
    }

    @Test
    @DisplayName("변경된 알림 템플릿을 렌더링한다.")
    void renderUpdatedTemplates() {
        assertThat(renderer.render(NotificationType.EXERCISE_REMINDER, Map.of()).content())
            .isEqualTo("오운완 사진 찍어서 기록해주세요.");
        assertThat(renderer.render(NotificationType.MEAL_REMINDER, Map.of()).content())
            .isEqualTo("오늘은 어떤 식사를 하셨나요? 궁금해요!");
        assertThat(renderer.render(NotificationType.COMMENT_CREATED, Map.of("nickname", "예은")).title())
            .isEqualTo("예은님이 댓글을 남겼어요.");
        assertThat(renderer.render(NotificationType.BUDDY_NUDGE, Map.of("nickname", "예은")).content())
            .isEqualTo("예은님의 응원을 받고 얼른 기록해주세요!");
        assertThat(renderer.render(NotificationType.STEP_CHALLENGE_COMPLETED, Map.of("destination", "수원")).content())
            .isEqualTo("수원을 걸어서 방문하다니 멋진걸요?");
        assertThat(renderer.render(NotificationType.WEEKLY_CHALLENGE_COMPLETED, Map.of("groupName", "2키로만")).title())
            .isEqualTo("이번 주 챌린지 완료!");
        assertThat(renderer.render(NotificationType.WEEKLY_CHALLENGE_COMPLETED, Map.of("groupName", "2키로만")).content())
            .isEqualTo("2키로만 그룹에서 챌린지 1개를 완료했어요!");
    }

    @Test
    @DisplayName("필수 payload가 없으면 알림 요청값 예외가 발생한다.")
    void renderMissingPayload() {
        assertThatThrownBy(() -> renderer.render(NotificationType.BUDDY_NUDGE, Map.of()))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.NOTIFICATION_PAYLOAD_INVALID));
    }

    @Test
    @DisplayName("이전 enum 타입은 신규 발송 템플릿에서 지원하지 않는다.")
    void renderUnsupportedType() {
        assertThatThrownBy(() -> renderer.render(NotificationType.COMMENT, Map.of()))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.NOTIFICATION_UNSUPPORTED_TYPE));
    }
}
