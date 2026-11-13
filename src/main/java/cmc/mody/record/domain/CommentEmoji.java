package cmc.mody.record.domain;

import java.util.Arrays;

/**
 * 댓글 입력창의 응원 이모지. 이미지는 앱에 두고 서버는 id만 저장한다.
 */
public enum CommentEmoji {
    CLAP("clap"),
    MEDAL("medal"),
    FIRE("fire"),
    HEART("heart"),
    RUN("run");

    private final String id;

    CommentEmoji(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static boolean isSupported(String id) {
        return Arrays.stream(values()).anyMatch(emoji -> emoji.id.equals(id));
    }
}
