package cmc.mody.notification.application;

public final class BuddyNudgeDedupeKey {
    private BuddyNudgeDedupeKey() {
    }

    public static String create(Long groupId, Long senderMemberId, Long receiverMemberId, String date) {
        return String.join(":",
            "BUDDY_NUDGE",
            "DATE",
            "GROUP",
            groupId.toString(),
            senderMemberId.toString(),
            receiverMemberId.toString(),
            date
        );
    }

    public static String legacy(Long senderMemberId, Long receiverMemberId, String date) {
        return String.join(":",
            "BUDDY_NUDGE",
            "DATE",
            senderMemberId.toString(),
            receiverMemberId.toString(),
            date
        );
    }
}
