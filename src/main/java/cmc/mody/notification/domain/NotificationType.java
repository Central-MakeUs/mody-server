package cmc.mody.notification.domain;

public enum NotificationType {
    GROUP_JOINED,
    RECORD_REMINDER,
    COMMENT,
    STREAK,
    NUDGE,
    CHALLENGE,
    GROUP_MEMBER_JOINED,
    EXERCISE_REMINDER,
    MEAL_REMINDER,
    COMMENT_CREATED,
    GROUP_RECORD_STREAK_RISK,
    BUDDY_NUDGE,
    STEP_CHALLENGE_COMPLETED,
    WEEKLY_CHALLENGE_COMPLETED,
    BUDDY_MEAL_RECORDED,
    BUDDY_EXERCISE_RECORDED,
    /** 관리자 공지. 푸시로만 보내며 알림 목록(notification 테이블)에는 저장하지 않는다. */
    ADMIN_ANNOUNCEMENT,
    DEV_TEST
}
