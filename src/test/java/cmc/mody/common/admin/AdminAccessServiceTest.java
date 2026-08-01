package cmc.mody.common.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import org.junit.jupiter.api.Test;

class AdminAccessServiceTest {
    @Test
    void acceptMatchingApiKey() {
        AdminAccessService service = new AdminAccessService("admin-key");

        assertThatCode(() -> service.validate("admin-key")).doesNotThrowAnyException();
    }

    @Test
    void rejectMissingOrNonMatchingApiKey() {
        AdminAccessService service = new AdminAccessService("admin-key");

        assertThatThrownBy(() -> service.validate(null))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getStatus()).isEqualTo(ErrorStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.validate("other-key"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getStatus()).isEqualTo(ErrorStatus.FORBIDDEN));
    }
}
