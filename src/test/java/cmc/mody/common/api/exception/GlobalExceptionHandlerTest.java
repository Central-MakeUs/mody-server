package cmc.mody.common.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import cmc.mody.common.api.ApiResponse;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.alert.ServerErrorAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    @Mock
    private ObjectProvider<ServerErrorAlertService> serverErrorAlertService;

    @Test
    void staticResourceNotFoundReturns404WithoutServerErrorAlert() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(serverErrorAlertService);

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFoundException(
            new NoResourceFoundException(HttpMethod.GET, "/favicon.ico")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody())
            .extracting(ApiResponse::code, ApiResponse::message)
            .containsExactly(ErrorStatus.NOT_FOUND.getCode(), ErrorStatus.NOT_FOUND.getMessage());
        then(serverErrorAlertService).shouldHaveNoInteractions();
    }
}
