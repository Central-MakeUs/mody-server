package cmc.mody.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void 요청마다_requestId를_만들어_응답_헤더와_MDC에_넣고_끝나면_지운다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/records");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> requestIdInChain.set(MDC.get(LogKeys.REQUEST_ID)));

        assertThat(requestIdInChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo(requestIdInChain.get());
        assertThat(MDC.get(LogKeys.REQUEST_ID)).isNull();
    }

    @Test
    void 클라이언트가_보낸_안전한_requestId는_그대로_쓴다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/records");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "ios-1234abcd");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("ios-1234abcd");
    }

    @Test
    void 형식이_이상한_requestId는_새로_만든다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/records");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "bad id\nforged-log-line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).doesNotContain("forged");
    }
}
