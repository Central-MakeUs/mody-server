package cmc.mody.common.upload;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.util.Arrays;

public enum UploadDomain {
    RECORD("record", "records"),
    PROFILE("profile", "profiles"),
    WEEKLY_CHALLENGE("weekly-challenge", "weekly-challenges");

    private final String value;
    private final String directory;

    UploadDomain(String value, String directory) {
        this.value = value;
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    public static UploadDomain from(String value) {
        return Arrays.stream(values())
            .filter(domain -> domain.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new GeneralException(ErrorStatus.UPLOAD_UNSUPPORTED_DOMAIN));
    }
}
