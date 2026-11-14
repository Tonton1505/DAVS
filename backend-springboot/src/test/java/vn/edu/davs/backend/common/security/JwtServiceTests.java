package vn.edu.davs.backend.common.security;

import vn.edu.davs.backend.config.SecurityProperties;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.security.JwtService;

import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTests {

    private static final String SECRET_A = Base64.getEncoder().encodeToString(
            "davs-test-secret-key-a-must-be-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)
    );
    private static final String SECRET_B = Base64.getEncoder().encodeToString(
            "davs-test-secret-key-b-must-be-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)
    );

    private Instant fixedNow;
    private JwtService jwtService;
    private DavsUser sampleUser;

    @BeforeEach
    void setUp() {
        fixedNow = Instant.parse("2026-11-14T00:00:00Z");
        SecurityProperties props = new SecurityProperties();
        props.setJwtSecret(SECRET_A);
        props.setAccessTokenTtlMinutes(15L);

        jwtService = new JwtService(props, Clock.fixed(fixedNow, ZoneOffset.UTC));
        sampleUser = DavsUser.of(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "ha.le@congty.vn",
                "Lê Thu Hà",
                SiteRole.SITE_ADMIN
        );
    }

    @Test
    void T005_generateAndParseAccessTokenSucceedsWithin15Minutes() {
        String token = jwtService.generateAccessToken(sampleUser);
        DavsUser parsed = jwtService.parseAndValidateAccessToken(token);

        assertThat(parsed.getId()).isEqualTo(sampleUser.getId());
        assertThat(parsed.getEmail()).isEqualTo("ha.le@congty.vn");
        assertThat(parsed.getFullName()).isEqualTo("Lê Thu Hà");
        assertThat(parsed.getSiteRole()).isEqualTo(SiteRole.SITE_ADMIN);
        assertThat(parsed.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_SITE_ADMIN");
    }

    @Test
    void T005_expiredAccessTokenAfter15MinutesThrowsErrAuth05() {
        String token = jwtService.generateAccessToken(sampleUser);

        SecurityProperties props = new SecurityProperties();
        props.setJwtSecret(SECRET_A);
        props.setAccessTokenTtlMinutes(15L);
        JwtService laterJwtService = new JwtService(
                props,
                Clock.fixed(fixedNow.plus(Duration.ofMinutes(16)), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> laterJwtService.parseAndValidateAccessToken(token))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_AUTH_05);
    }

    @Test
    void T005_invalidSignatureTokenThrowsErrAuth05() {
        SecurityProperties otherProps = new SecurityProperties();
        otherProps.setJwtSecret(SECRET_B);
        otherProps.setAccessTokenTtlMinutes(15L);
        JwtService otherJwtService = new JwtService(otherProps, Clock.fixed(fixedNow, ZoneOffset.UTC));

        String tokenSignedByWrongKey = otherJwtService.generateAccessToken(sampleUser);

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(tokenSignedByWrongKey))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_AUTH_05);
    }

    @Test
    void T005_tamperedOrMalformedTokenThrowsErrAuth05() {
        String token = jwtService.generateAccessToken(sampleUser);
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(tampered))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_AUTH_05);

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken("not.a.jwt"))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_AUTH_05);

        assertThatThrownBy(() -> jwtService.parseAndValidateAccessToken(" "))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_AUTH_05);
    }

    @Test
    void T005_bcryptPasswordEncoderHashesAndMatches() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Matkhau@123456");

        assertThat(hash).startsWith("$2");
        assertThat(encoder.matches("Matkhau@123456", hash)).isTrue();
        assertThat(encoder.matches("SaiMatKhau", hash)).isFalse();
    }
}
