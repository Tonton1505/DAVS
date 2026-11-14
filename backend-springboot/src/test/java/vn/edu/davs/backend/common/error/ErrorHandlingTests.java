package vn.edu.davs.backend.common.error;

import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.exception.FieldErrorItem;
import vn.edu.davs.backend.exception.GlobalExceptionHandler;
import vn.edu.davs.backend.exception.ValidationErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlingTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyErrorController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest(name = "T004_{index}_{0}_returnsCanonicalJsonAndNoStackTrace")
    @EnumSource(ErrorCode.class)
    void T004_everyErrorCodeReturnsCanonicalJsonFormatAndNoStackTrace(ErrorCode errorCode) throws Exception {
        String sampleArg = "test-arg-01";
        MvcResult result = mockMvc.perform(get("/test/errors/{name}", errorCode.name())
                        .param("arg", sampleArg)
                        .header("X-Trace-Id", "trace-" + errorCode.getCode()))
                .andExpect(status().is(errorCode.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(errorCode.getCode()))
                .andExpect(jsonPath("$.message").value(errorCode.formatMessage(sampleArg)))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.traceId").value("trace-" + errorCode.getCode()))
                .andReturn();

        String rawBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(rawBody)
                .doesNotContain("stackTrace")
                .doesNotContain("Suppressed")
                .doesNotContain("GlobalExceptionHandler.java")
                .doesNotContain("DummyErrorController");
    }

    @Test
    void T004_all46AppendixAErrorCodesArePresentAndMatchMessagesProperties() throws Exception {
        assertThat(ErrorCode.values()).hasSize(46);

        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("messages_vi.properties")) {
            assertThat(is).isNotNull();
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        }

        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ErrorCode.fromCode(ec.getCode())).contains(ec);
            String propValue = props.getProperty(ec.getCode());
            assertThat(propValue)
                    .as("messages_vi.properties entry for %s", ec.getCode())
                    .isEqualTo(ec.getMessageTemplate());
        }
    }

    @Test
    void T004_errUsr02MatchesExactSection7ContractExample() throws Exception {
        mockMvc.perform(get("/test/errors/usr02-with-field")
                        .header("X-Trace-Id", "trace-sec7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-USR-02"))
                .andExpect(jsonPath("$.message").value("Tài khoản \"an@x.vn\" đã tồn tại."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Tài khoản đã tồn tại"))
                .andExpect(jsonPath("$.traceId").value("trace-sec7"));
    }

    @Test
    void T004_jakartaValidationMapsToFieldErrorsAndAnnotatedErrorCode() throws Exception {
        mockMvc.perform(post("/test/errors/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR-PRJ-02"))
                .andExpect(jsonPath("$.message").value(ErrorCode.ERR_PRJ_02.getMessageTemplate()))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void T004_unexpectedExceptionMapsToErrSys01WithTraceIdAndHidesInternalDetails() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/errors/unexpected")
                        .header("X-Trace-Id", "req-999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERR-SYS-01"))
                .andExpect(jsonPath("$.message").value(
                        "Hệ thống gặp sự cố. Vui lòng thử lại sau hoặc liên hệ hỗ trợ (mã: req-999)."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.traceId").value("req-999"))
                .andReturn();

        String rawBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(rawBody).doesNotContain("secret-db-connection-string");
    }

    @ValidationErrorCode(ErrorCode.ERR_PRJ_02)
    record DummyProjectRequest(
            @NotBlank(message = "Tên dự án không được để trống")
            @Size(min = 1, max = 128)
            String name
    ) {
    }

    @RestController
    static class DummyErrorController {

        @GetMapping("/test/errors/{name}")
        void throwByEnumName(
                @PathVariable("name") String name,
                @RequestParam(value = "arg", required = false) String arg
        ) {
            ErrorCode code = ErrorCode.valueOf(name);
            throw new DavsException(code, arg);
        }

        @GetMapping("/test/errors/usr02-with-field")
        void throwUsr02WithField() {
            throw new DavsException(
                    ErrorCode.ERR_USR_02,
                    List.of(new FieldErrorItem("username", "Tài khoản đã tồn tại")),
                    "an@x.vn"
            );
        }

        @PostMapping("/test/errors/projects")
        void validateProject(@Valid @RequestBody DummyProjectRequest request) {
            // no-op: validation triggers before method body when invalid
        }

        @GetMapping("/test/errors/unexpected")
        void throwUnexpected() {
            throw new IllegalStateException("Internal failure with secret-db-connection-string");
        }
    }
}
