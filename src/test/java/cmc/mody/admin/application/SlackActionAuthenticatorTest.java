package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SlackActionAuthenticatorTest {
    /** mody-slack-bot test/report-actions.test.js와 같은 고정 값이다. 두 쪽 서명이 어긋나면 둘 중 하나가 실패한다. */
    @Test
    void signatureMatchesBotVector() {
        assertThat(SlackActionAuthenticator.sign("test-secret".getBytes(StandardCharsets.UTF_8), "1700000000", "POST",
            "/api/v1/admin/slack/reports/records/100/hide", "U0TEST"))
            .isEqualTo("v1=c465434c42378debf7b2d699de0fc365de9ddf9a73d9fb93fa529daa8647d38e");
    }
}
