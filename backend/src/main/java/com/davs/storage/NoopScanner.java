package com.davs.storage;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default configurable virus scanner implementation for v1 (ADR D8).
 * When enabled, detects the standard EICAR test signature and allows clean files through.
 */
@Component
@ConditionalOnMissingBean(name = "customVirusScanner")
public class NoopScanner implements VirusScanner {

    private static final String EICAR_SIGNATURE =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    private final boolean enabled;

    public NoopScanner(@Value("${davs.storage.virus-scan.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ScanResult scan(String fileName, byte[] headerContent, long totalSize) {
        if (!enabled || headerContent == null || headerContent.length == 0) {
            return ScanResult.ok();
        }
        String preview = new String(headerContent, StandardCharsets.ISO_8859_1);
        if (preview.contains(EICAR_SIGNATURE)) {
            return ScanResult.rejected("EICAR test signature detected");
        }
        return ScanResult.ok();
    }
}
