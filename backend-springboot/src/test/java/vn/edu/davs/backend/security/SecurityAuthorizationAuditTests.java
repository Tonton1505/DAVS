package vn.edu.davs.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.controller.AdminSettingsController;
import vn.edu.davs.backend.controller.ConnectedAppController;
import vn.edu.davs.backend.rest.DashboardController;
import vn.edu.davs.backend.controller.FormulaController;
import vn.edu.davs.backend.controller.GroupController;
import vn.edu.davs.backend.controller.ParameterController;
import vn.edu.davs.backend.controller.RowFilterController;
import vn.edu.davs.backend.controller.SheetController;
import vn.edu.davs.backend.controller.UserCreationController;
import vn.edu.davs.backend.controller.UserManagementController;
import vn.edu.davs.backend.dto.SaveRowFilterRequest;
import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.UpdateSettingsRequest;
import vn.edu.davs.backend.exception.ApiErrorResponse;
import vn.edu.davs.backend.exception.GlobalExceptionHandler;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.SensitiveDataMasker;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-104 Automated Security Audit Tests:
 * - Vertical & horizontal authorization coverage across all write endpoints
 * - ERR-SYS-01 non-disclosure of internal stack traces / SQL details
 * - Sensitive data masking in audit logs
 */
class SecurityAuthorizationAuditTests {

    private static final Set<String> PUBLIC_AUTH_WRITE_METHODS = Set.of(
            "login", "refresh", "logout", "generateToken"
    );

    @Test
    void allWriteEndpointsEnforcePreAuthorizeOrActorPermissionChecks() {
        List<Class<?>> controllers = List.of(
                AdminSettingsController.class,
                ConnectedAppController.class,
                DashboardController.class,
                FormulaController.class,
                GroupController.class,
                ParameterController.class,
                RowFilterController.class,
                SheetController.class,
                UserCreationController.class,
                UserManagementController.class
        );

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                boolean isWrite = method.isAnnotationPresent(PostMapping.class)
                        || method.isAnnotationPresent(PutMapping.class)
                        || method.isAnnotationPresent(PatchMapping.class)
                        || method.isAnnotationPresent(DeleteMapping.class);
                if (!isWrite || PUBLIC_AUTH_WRITE_METHODS.contains(method.getName())) {
                    continue;
                }
                boolean hasPreAuthorize = method.isAnnotationPresent(PreAuthorize.class);
                boolean hasActorParam = hasDavsUserParameter(method);
                assertThat(hasPreAuthorize || hasActorParam)
                        .as("Write endpoint %s#%s must enforce @PreAuthorize or DavsUser permission check",
                                controller.getSimpleName(), method.getName())
                        .isTrue();
            }
        }
    }

    @Test
    void adminAndAuthoringWriteEndpointsBlockViewerViaPreAuthorize() throws NoSuchMethodException {
        PreAuthorize settingsPut = AdminSettingsController.class
                .getDeclaredMethod("updateSettings", DavsUser.class, UpdateSettingsRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertThat(settingsPut).isNotNull();
        assertThat(settingsPut.value()).contains("SITE_ADMIN");

        PreAuthorize rlsPost = RowFilterController.class
                .getDeclaredMethod("saveRowFilter", SaveRowFilterRequest.class, DavsUser.class)
                .getAnnotation(PreAuthorize.class);
        assertThat(rlsPost).isNotNull();
        assertThat(rlsPost.value()).contains("SITE_ADMIN");

        PreAuthorize sheetPost = SheetController.class
                .getDeclaredMethod("createSheet", UUID.class, SaveSheetRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertThat(sheetPost).isNotNull();
        assertThat(sheetPost.value()).doesNotContain("VIEWER");
    }

    @Test
    void errSys01DoesNotLeakInternalStackTraceOrSqlDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RuntimeException internalFailure = new RuntimeException(
                "PSQLException: FATAL: password authentication failed at com.zaxxer.hikari.HikariPool"
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/query");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(internalFailure, request);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ERR-SYS-01");
        assertThat(response.getBody().traceId()).isNotBlank();
        assertThat(response.getBody().message())
                .doesNotContain("PSQLException")
                .doesNotContain("hikari")
                .doesNotContain("password");
    }

    @Test
    void sensitiveDataMaskerRedactsSecretsAndTokens() {
        SensitiveDataMasker masker = new SensitiveDataMasker(new ObjectMapper());
        JsonNode masked = masker.mask(Map.of(
                "username", "admin@davs.local",
                "password", "SuperSecret!123",
                "clientSecret", "sec_live_987654",
                "accessToken", "eyJhbGciOiJIUzI1NiJ9.payload.sig"
        ));

        assertThat(masked.get("username").asText()).isEqualTo("admin@davs.local");
        assertThat(masked.get("password").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("clientSecret").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
        assertThat(masked.get("accessToken").asText()).isEqualTo(SensitiveDataMasker.REDACTED);
    }

    private boolean hasDavsUserParameter(Method method) {
        for (Parameter param : method.getParameters()) {
            if (DavsUser.class.isAssignableFrom(param.getType())) {
                return true;
            }
        }
        return false;
    }
}
