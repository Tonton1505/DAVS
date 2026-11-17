package vn.edu.davs.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.edu.davs.backend.security.TraceIdFilter;

class TraceIdFilterTests {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdWhenHeaderMissingAndClearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] capturedMdc = new String[1];

        FilterChain chain = (req, res) -> capturedMdc[0] = MDC.get(TraceIdFilter.MDC_TRACE_KEY);
        filter.doFilter(request, response, chain);

        assertThat(capturedMdc[0]).isNotBlank();
        assertThat(response.getHeader(TraceIdFilter.HEADER_REQUEST_ID)).isEqualTo(capturedMdc[0]);
        assertThat(MDC.get(TraceIdFilter.MDC_TRACE_KEY)).isNull();
    }

    @Test
    void preservesValidIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.addHeader(TraceIdFilter.HEADER_REQUEST_ID, "req-t107-abc_123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] capturedMdc = new String[1];

        FilterChain chain = (req, res) -> capturedMdc[0] = MDC.get(TraceIdFilter.MDC_TRACE_KEY);
        filter.doFilter(request, response, chain);

        assertThat(capturedMdc[0]).isEqualTo("req-t107-abc_123");
        assertThat(response.getHeader(TraceIdFilter.HEADER_REQUEST_ID)).isEqualTo("req-t107-abc_123");
        assertThat(MDC.get(TraceIdFilter.MDC_TRACE_KEY)).isNull();
    }

    @Test
    void replacesUnsafeHeaderToPreventLogInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.addHeader(TraceIdFilter.HEADER_REQUEST_ID, "bad\n\"injected\":true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] capturedMdc = new String[1];

        FilterChain chain = (req, res) -> capturedMdc[0] = MDC.get(TraceIdFilter.MDC_TRACE_KEY);
        filter.doFilter(request, response, chain);

        assertThat(capturedMdc[0]).isNotBlank();
        assertThat(capturedMdc[0]).doesNotContain("\n").doesNotContain("\"");
        assertThat(response.getHeader(TraceIdFilter.HEADER_REQUEST_ID)).isEqualTo(capturedMdc[0]);
    }
}
