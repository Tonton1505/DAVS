package com.davs;

import com.davs.common.seed.DataSeeder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedDataTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(
                jdbcTemplate,
                passwordEncoder,
                "admin@davs.local",
                "Secret@Admin123"
        );
    }

    @Test
    void seedInitialAdminUser_createsAdminWhenNotExists() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("admin@davs.local")))
                .thenReturn(List.of());
        when(passwordEncoder.encode("Secret@Admin123")).thenReturn("$2a$10$hashedAdminPassword");

        boolean created = dataSeeder.seedInitialAdminUser();

        assertThat(created).isTrue();
        verify(passwordEncoder).encode("Secret@Admin123");
        verify(jdbcTemplate).update(contains("INSERT INTO users"), any(), eq("admin@davs.local"), eq("$2a$10$hashedAdminPassword"));
        verify(jdbcTemplate).update(contains("INSERT INTO group_members"), any(Object.class));
        verify(jdbcTemplate).update(contains("UPDATE license_pools SET used = used + 1"));
    }

    @Test
    void seedInitialAdminUser_isIdempotentAndDoesNotDuplicateAdmin() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("admin@davs.local")))
                .thenReturn(List.of("00000000-0000-0000-0000-000000000099"));

        boolean created = dataSeeder.seedInitialAdminUser();

        assertThat(created).isFalse();
        verify(passwordEncoder, never()).encode(anyString());
        verify(jdbcTemplate, never()).update(contains("INSERT INTO users"), any(), any(), any());
    }
}
