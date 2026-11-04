package cmc.mody.notification.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.PushPlatform;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberPushTokenRepositoryTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 15, 0);

    @Autowired
    private MemberPushTokenRepository memberPushTokenRepository;

    @Test
    @DisplayName("같은 회원·기기로 두 번 등록하면 한 행만 남기고 최신 토큰으로 갱신한다.")
    void upsertKeepsSingleRowPerDevice() {
        memberPushTokenRepository.upsert(10L, 1L, "ios-device", "IOS", "first-token", NOW);
        memberPushTokenRepository.upsert(11L, 1L, "ios-device", "IOS", "second-token", NOW.plusMinutes(1));

        List<MemberPushToken> tokens = memberPushTokenRepository.findByMemberIdAndEnabledTrueAndDeletedAtIsNull(1L);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getId()).isEqualTo(10L);
        assertThat(tokens.get(0).getFcmToken()).isEqualTo("second-token");
        assertThat(tokens.get(0).getPlatform()).isEqualTo(PushPlatform.IOS);
        assertThat(tokens.get(0).getLastSeenAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    @DisplayName("꺼진 토큰을 다시 등록하면 다시 켠다.")
    void upsertReEnablesDisabledToken() {
        memberPushTokenRepository.upsert(10L, 1L, "ios-device", "IOS", "fcm-token", NOW);
        MemberPushToken token = memberPushTokenRepository.findById(10L).orElseThrow();
        token.disable();
        memberPushTokenRepository.flush();

        memberPushTokenRepository.upsert(11L, 1L, "ios-device", "IOS", "fcm-token", NOW.plusMinutes(1));

        assertThat(memberPushTokenRepository.findById(10L).orElseThrow().isEnabled()).isTrue();
        assertThat(memberPushTokenRepository.findById(11L)).isEmpty();
    }

    @Test
    @DisplayName("같은 토큰을 쓰던 다른 회원·기기 행만 지운다.")
    void deleteOtherDevicesUsingToken() {
        memberPushTokenRepository.upsert(10L, 1L, "ios-device", "IOS", "fcm-token", NOW);
        memberPushTokenRepository.upsert(11L, 2L, "ios-device", "IOS", "fcm-token", NOW);
        memberPushTokenRepository.upsert(12L, 1L, "old-device", "IOS", "fcm-token", NOW);
        memberPushTokenRepository.upsert(13L, 3L, "android-device", "ANDROID", "other-token", NOW);

        int deleted = memberPushTokenRepository.deleteOtherDevicesUsingToken("fcm-token", 1L, "ios-device");

        assertThat(deleted).isEqualTo(2);
        assertThat(memberPushTokenRepository.findAll())
            .extracting(MemberPushToken::getId)
            .containsExactlyInAnyOrder(10L, 13L);
    }
}
