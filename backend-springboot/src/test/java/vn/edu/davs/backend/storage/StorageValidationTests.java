package vn.edu.davs.backend.storage;

import vn.edu.davs.backend.service.FileNameSanitizer;
import vn.edu.davs.backend.service.MagicBytesValidator;

import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageValidationTests {

    private final MagicBytesValidator validator = new MagicBytesValidator();

    @Test
    void T009_filenameSanitizerNeutralizesPathTraversalAndUnsafeChars() {
        assertThat(FileNameSanitizer.sanitize("../../x.csv")).isEqualTo("x.csv");
        assertThat(FileNameSanitizer.sanitize("..\\..\\etc\\passwd")).isEqualTo("passwd");
        assertThat(FileNameSanitizer.sanitize("C:\\temp\\my report (1)!.xlsx")).isEqualTo("my_report__1__.xlsx");
        assertThat(FileNameSanitizer.sanitize("../../")).isEqualTo("file");
        assertThat(FileNameSanitizer.sanitize("   ")).isEqualTo("file");
    }

    @Test
    void T009_magicBytesValidatorAcceptsMatchingHeadersAndRejectsSpoofedFiles() {
        byte[] csvBytes = "id,name,amount\n1,A,100\n".getBytes(StandardCharsets.UTF_8);
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        byte[] zipHeader = {'P', 'K', 0x03, 0x04, 0x14, 0x00};
        byte[] exeHeader = {'M', 'Z', (byte) 0x90, 0x00};

        assertThatCode(() -> validator.validate("sales.csv", csvBytes)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("chart.png", pngHeader)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("book.twbx", zipHeader)).doesNotThrowAnyException();

        // Spoofed EXE renamed to .csv
        assertThatThrownBy(() -> validator.validate("fake.csv", exeHeader))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_08);

        // Plain text renamed to .png
        assertThatThrownBy(() -> validator.validate("fake.png", csvBytes))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_08);

        // Plain text renamed to .xlsx
        assertThatThrownBy(() -> validator.validate("fake.xlsx", csvBytes))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_08);

        // Disallowed executable extension
        assertThatThrownBy(() -> validator.validate("malware.exe", exeHeader))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_08);
    }
}
