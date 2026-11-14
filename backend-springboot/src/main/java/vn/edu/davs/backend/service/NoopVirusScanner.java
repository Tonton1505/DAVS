package vn.edu.davs.backend.service;

import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Default no-op {@link VirusScanner} used when external AV scanning is disabled or not wired.
 */
@Slf4j
public class NoopVirusScanner implements VirusScanner {

    @Override
    public void scan(String fileName, InputStream content) {
        log.debug("NoopVirusScanner skipped AV scan for '{}'", fileName);
    }
}
