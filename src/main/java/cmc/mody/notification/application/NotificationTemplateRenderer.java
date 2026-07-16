package cmc.mody.notification.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.notification.domain.NotificationType;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateRenderer {
    public NotificationTemplate render(NotificationType type, Map<String, Object> payload) {
        return switch (type) {
            case GROUP_MEMBER_JOINED -> new NotificationTemplate(
                NotificationPayload.requireString(payload, "groupName") + "에 새 버디가 참여했어요!",
                NotificationPayload.requireString(payload, "nickname") + "님을 환영해주세요."
            );
            case EXERCISE_REMINDER -> new NotificationTemplate(
                "운동할 시간!",
                "오운완 사진 찍어서 기록해주세요."
            );
            case MEAL_REMINDER -> new NotificationTemplate(
                "식사할 시간!",
                "오늘은 어떤 식사를 하셨나요? 궁금해요!"
            );
            case COMMENT_CREATED -> new NotificationTemplate(
                NotificationPayload.requireString(payload, "nickname") + "님이 댓글을 남겼어요.",
                "어떤 이야기를 남겼는지 확인하러 가요!"
            );
            case GROUP_RECORD_STREAK_RISK -> new NotificationTemplate(
                NotificationPayload.requireString(payload, "nickname") + "님 어디가셨나요 ㅠㅠ",
                "오늘 기록하지 않으면 그룹 연속 기록이 깨져요!"
            );
            case BUDDY_NUDGE -> {
                String nickname = NotificationPayload.requireString(payload, "nickname");
                yield new NotificationTemplate(
                    nickname + "님이 콕 찔렀어요!",
                    nickname + "님의 응원을 받고 얼른 기록해주세요!"
                );
            }
            case STEP_CHALLENGE_COMPLETED -> {
                String destination = NotificationPayload.requireString(payload, "destination");
                yield new NotificationTemplate(
                    destination + "까지 걸어가기 완료!",
                    destination + "을 걸어서 방문하다니 멋진걸요?"
                );
            }
            case WEEKLY_CHALLENGE_COMPLETED -> new NotificationTemplate(
                "이번 주 챌린지 완료!",
                NotificationPayload.requireString(payload, "groupName") + " 그룹에서 챌린지 1개를 완료했어요!"
            );
            default -> throw new GeneralException(ErrorStatus.NOTIFICATION_UNSUPPORTED_TYPE);
        };
    }
}
