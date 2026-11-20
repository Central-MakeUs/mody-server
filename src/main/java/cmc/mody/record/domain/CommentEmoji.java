package cmc.mody.record.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 댓글 입력창의 응원 이모지. 이미지는 앱에 두고 서버는 id만 저장한다.
 * 텍스트 사이에 넣을 때는 content에 {@code :id:} 형식으로 쓴다.
 */
public enum CommentEmoji {
    CLAP("clap"),
    MEDAL("medal"),
    FIRE("fire"),
    HEART("heart"),
    RUN("run");

    private static final Pattern SINGLE_TOKEN = Pattern.compile("^:([a-z]+):$");

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

    /**
     * content가 지원하는 이모지 토큰 하나뿐이면(예: {@code :clap:}) 그 id를 돌려준다.
     */
    public static Optional<String> findSingleEmojiId(String content) {
        if (content == null) {
            return Optional.empty();
        }
        Matcher matcher = SINGLE_TOKEN.matcher(content.trim());
        if (!matcher.matches() || !isSupported(matcher.group(1))) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
}
