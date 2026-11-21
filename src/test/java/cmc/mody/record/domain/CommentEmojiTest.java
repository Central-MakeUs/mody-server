package cmc.mody.record.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentEmojiTest {

    @Test
    @DisplayName("지원하는 이모지 토큰 하나뿐인 내용이면 id를 찾는다.")
    void findSingleEmojiId() {
        assertThat(CommentEmoji.findSingleEmojiId(":mody-clap:")).contains("clap");
        assertThat(CommentEmoji.findSingleEmojiId("  :mody-run:  ")).contains("run");
    }

    @Test
    @DisplayName("텍스트가 섞였거나 지원하지 않는 토큰이면 찾지 않는다.")
    void findSingleEmojiIdIgnoresOtherContent() {
        assertThat(CommentEmoji.findSingleEmojiId("좋다 :mody-clap:")).isEmpty();
        assertThat(CommentEmoji.findSingleEmojiId(":mody-clap::mody-fire:")).isEmpty();
        assertThat(CommentEmoji.findSingleEmojiId(":mody-smile:")).isEmpty();
        assertThat(CommentEmoji.findSingleEmojiId(":clap:")).isEmpty();
        assertThat(CommentEmoji.findSingleEmojiId("10:30:")).isEmpty();
        assertThat(CommentEmoji.findSingleEmojiId(null)).isEmpty();
    }
}
