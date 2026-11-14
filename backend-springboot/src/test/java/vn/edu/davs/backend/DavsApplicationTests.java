package vn.edu.davs.backend;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DavsApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void T003_contextLoadsAndFlywayBaselineRuns() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' "
                        + "AND table_name IN ('users', 'system_settings', 'audit_logs', 'jobs')",
                String.class
        );
        assertThat(tables).containsExactlyInAnyOrder("users", "system_settings", "audit_logs", "jobs");

        String uuid = jdbcTemplate.queryForObject("SELECT gen_random_uuid()::text", String.class);
        assertThat(uuid).isNotBlank();
    }

    @Test
    void T003_actuatorHealthReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void T005_unauthenticatedProtectedEndpointReturns401ErrAuth05() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-05"));
    }

    @Test
    void T003_openApiDocsEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/v1/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DAVS REST API"))
                .andExpect(jsonPath("$.info.version").value("v1.0"));
    }
}
