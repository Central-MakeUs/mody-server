package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.client.KakaoApiFeignClient;
import cmc.mody.auth.infrastructure.oauth.client.KakaoAuthFeignClient;
import cmc.mody.auth.infrastructure.oauth.dto.KakaoOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.KakaoUserResponse;
import cmc.mody.common.api.status.ErrorStatus;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthClient extends OAuthProviderClient {
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final KakaoAuthFeignClient kakaoAuthFeignClient;
    private final KakaoApiFeignClient kakaoApiFeignClient;
    private final OAuthProperties.Provider properties;

    public KakaoOAuthClient(
        KakaoAuthFeignClient kakaoAuthFeignClient,
        KakaoApiFeignClient kakaoApiFeignClient,
        OAuthProperties properties
    ) {
        this.kakaoAuthFeignClient = kakaoAuthFeignClient;
        this.kakaoApiFeignClient = kakaoApiFeignClient;
        this.properties = properties.getKakao();
    }

    public String getRedirectUrl() {
        return buildRedirectUrl(properties);
    }

    public KakaoOAuthTokenResponse requestAccessToken(String code) {
        return executeFeign(
            () -> kakaoAuthFeignClient.requestAccessToken(
                GRANT_TYPE_AUTHORIZATION_CODE,
                properties.redirectUri(),
                properties.clientId(),
                code,
                blankToNull(properties.clientSecret())
            ),
            ErrorStatus.INVALID_OAUTH_TOKEN
        );
    }

    public KakaoUserResponse requestUserInfo(String accessToken) {
        validateToken(accessToken);
        return executeFeign(
            () -> kakaoApiFeignClient.requestUserInfo(bearer(accessToken)),
            ErrorStatus.OAUTH_PROFILE_REQUEST_FAILED
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
