package vn.edu.davs.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs {@link SystemSeedService#seedIfNeeded()} once on application startup.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class SystemSeedRunner implements ApplicationRunner {

    private final SystemSeedService systemSeedService;

    @Override
    public void run(ApplicationArguments args) {
        systemSeedService.seedIfNeeded();
    }
}
