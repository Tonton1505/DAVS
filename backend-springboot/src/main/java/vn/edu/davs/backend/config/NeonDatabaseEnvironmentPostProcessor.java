package vn.edu.davs.backend.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Wires Neon {@code DATABASE_URL} (pooled) for {@code spring.datasource} and
 * {@code DATABASE_URL_UNPOOLED} (direct) for {@code spring.flyway}, reading from
 * environment variables or {@code .env.local} when {@code DB_URL} is not explicitly set.
 */
public class NeonDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "neonDatabaseEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasExplicitDbUrl(environment)) {
            return;
        }

        boolean isTestRun = environment.getPropertySources().contains("Inlined Test Properties")
                || System.getProperty("surefire.test.class.path") != null;
        Map<String, String> dotEnv = isTestRun ? Map.of() : loadDotEnvLocal();
        String pooledUrl = firstNonBlank(environment.getProperty("DATABASE_URL"), dotEnv.get("DATABASE_URL"));
        String directUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL_UNPOOLED"),
                dotEnv.get("DATABASE_URL_UNPOOLED")
        );

        Map<String, Object> overrides = new HashMap<>();
        if (pooledUrl != null) {
            JdbcParts pooled = parsePostgresUri(pooledUrl);
            overrides.put("spring.datasource.url", pooled.jdbcUrl());
            if (pooled.username() != null) {
                overrides.put("spring.datasource.username", pooled.username());
            }
            if (pooled.password() != null) {
                overrides.put("spring.datasource.password", pooled.password());
            }
        }
        if (directUrl != null) {
            JdbcParts direct = parsePostgresUri(directUrl);
            overrides.put("spring.flyway.url", direct.jdbcUrl());
            if (direct.username() != null) {
                overrides.put("spring.flyway.user", direct.username());
            }
            if (direct.password() != null) {
                overrides.put("spring.flyway.password", direct.password());
            }
        }

        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
        }
    }

    static JdbcParts parsePostgresUri(String rawUri) {
        String trimmed = rawUri.trim();
        if (trimmed.startsWith("jdbc:")) {
            return new JdbcParts(trimmed, null, null);
        }
        URI uri = URI.create(trimmed);
        String scheme = uri.getScheme();
        if (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Unsupported DATABASE_URL scheme: " + scheme);
        }
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getRawPath() != null ? uri.getRawPath() : "/neondb";
        String query = uri.getRawQuery();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
        if (port > 0) {
            jdbc.append(':').append(port);
        }
        jdbc.append(path);
        if (query != null && !query.isBlank()) {
            jdbc.append('?').append(query);
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = urlDecode(userInfo.substring(0, colon));
                password = urlDecode(userInfo.substring(colon + 1));
            } else {
                username = urlDecode(userInfo);
            }
        }
        return new JdbcParts(jdbc.toString(), username, password);
    }

    private static boolean hasExplicitDbUrl(ConfigurableEnvironment environment) {
        String dbUrl = environment.getProperty("DB_URL");
        return dbUrl != null && !dbUrl.isBlank();
    }

    private static Map<String, String> loadDotEnvLocal() {
        Map<String, String> values = new HashMap<>();
        for (Path candidate : List.of(Path.of(".env.local"), Path.of("..", ".env.local"))) {
            if (Files.isRegularFile(candidate)) {
                try {
                    for (String line : Files.readAllLines(candidate, StandardCharsets.UTF_8)) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eq = trimmed.indexOf('=');
                        if (eq > 0) {
                            String key = trimmed.substring(0, eq).trim();
                            String val = unquote(trimmed.substring(eq + 1).trim());
                            values.putIfAbsent(key, val);
                        }
                    }
                    break;
                } catch (IOException ignored) {
                    // Ignore unreadable .env.local
                }
            }
        }
        return values;
    }

    private static String unquote(String val) {
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second != null && !second.isBlank()) ? second : null;
    }

    record JdbcParts(String jdbcUrl, String username, String password) {
    }
}
