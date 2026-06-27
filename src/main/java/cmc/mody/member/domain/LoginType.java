package cmc.mody.member.domain;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.util.Locale;

public enum LoginType {
    KAKAO,
    APPLE,
    GOOGLE;

    public static LoginType from(String value) {
        if (value == null || value.isBlank()) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_LOGIN_TYPE);
        }
        try {
            return LoginType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.UNSUPPORTED_LOGIN_TYPE);
        }
    }
}
