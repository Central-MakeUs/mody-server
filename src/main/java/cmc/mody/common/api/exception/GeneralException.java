package cmc.mody.common.api.exception;

import cmc.mody.common.api.status.ErrorStatus;

public class GeneralException extends RuntimeException {
    private final ErrorStatus status;

    public GeneralException(ErrorStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public ErrorStatus getStatus() {
        return status;
    }
}
