package vn.edu.davs.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;
import vn.edu.davs.backend.config.NeonDatabaseEnvironmentPostProcessor.JdbcParts;

class NeonDatabaseEnvironmentPostProcessorTests {

    @Test
    void parsesNeonPooledAndDirectConnectionStringsToJdbc() {
        JdbcParts parts = NeonDatabaseEnvironmentPostProcessor.parsePostgresUri(
                "postgresql://neondb_owner:my%2Bpass@ep-cool-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require"
        );
        assertThat(parts.jdbcUrl())
                .isEqualTo("jdbc:postgresql://ep-cool-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require");
        assertThat(parts.username()).isEqualTo("neondb_owner");
        assertThat(parts.password()).isEqualTo("my+pass");
    }

    @Test
    void bindsDatabaseUrlAndUnpooledUrlToDatasourceAndFlyway() {
        MockEnvironment env = new MockEnvironment()
                .withProperty(
                        "DATABASE_URL",
                        "postgresql://user1:pwd1@ep-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require"
                )
                .withProperty(
                        "DATABASE_URL_UNPOOLED",
                        "postgresql://user1:pwd1@ep-direct.us-east-2.aws.neon.tech/neondb?sslmode=require"
                );

        new NeonDatabaseEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("user1");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("pwd1");
        assertThat(env.getProperty("spring.flyway.url"))
                .isEqualTo("jdbc:postgresql://ep-direct.us-east-2.aws.neon.tech/neondb?sslmode=require");
        assertThat(env.getProperty("spring.flyway.user")).isEqualTo("user1");
        assertThat(env.getProperty("spring.flyway.password")).isEqualTo("pwd1");
    }
}
