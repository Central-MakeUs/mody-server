package cmc.mody.dev.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.common.alert.ServerErrorAlertService;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.dev.application.DevToolService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevToolController.class)
@Import(WebConfig.class)
@ActiveProfiles("dev")
class DevToolControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DevToolService devToolService;

    @MockitoBean
    private ServerErrorAlertService serverErrorAlertService;

    @Test
    void throwInternalServerErrorForSlackAlertTest() throws Exception {
        mockMvc.perform(post("/api/v1/dev/errors/internal-server-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorStatus.INTERNAL_SERVER_ERROR.getCode()));

        then(serverErrorAlertService).should().notify(
            any(IllegalStateException.class),
            any(HttpServletRequest.class),
            eq(ErrorStatus.INTERNAL_SERVER_ERROR.getHttpStatus()),
            eq(ErrorStatus.INTERNAL_SERVER_ERROR.getCode())
        );
    }
}
