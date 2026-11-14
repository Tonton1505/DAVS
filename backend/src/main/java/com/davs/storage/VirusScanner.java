package com.davs.storage;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import java.io.InputStream;

/**
 * Pluggable antivirus scanning interface for uploaded objects (T-009).
 */
public interface VirusScanner {

    /**
     * Scans the given file stream; throws {@link DavsException} with {@link ErrorCode#ERR_PRJ_08}
     * if a threat is detected.
     */
    void scan(String fileName, InputStream content);
}
