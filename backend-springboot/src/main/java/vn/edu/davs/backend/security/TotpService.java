package vn.edu.davs.backend.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * RFC 6238 Time-based One-Time Password (TOTP) generator and verifier for T-102 MFA.
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final long TIME_STEP_SECONDS = 30L;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a 160-bit random Base32 secret suitable for authenticator apps.
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Builds an {@code otpauth://totp/...} provisioning URI for QR code generation.
     */
    public String buildOtpAuthUri(String issuer, String accountEmail, String secret) {
        String safeIssuer = issuer != null && !issuer.isBlank() ? issuer.trim() : "DAVS";
        String label = urlEncode(safeIssuer + ":" + accountEmail.trim());
        return "otpauth://totp/" + label
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(safeIssuer)
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    /**
     * Verifies a 6-digit TOTP code against the secret within {@code +/- allowedDrift} time steps.
     */
    public boolean verifyCode(String base32Secret, String code, Instant now, int allowedDrift) {
        if (base32Secret == null || base32Secret.isBlank() || code == null) {
            return false;
        }
        String normalizedCode = code.trim();
        if (!normalizedCode.matches("^\\d{6}$")) {
            return false;
        }
        byte[] secretBytes = base32Decode(base32Secret);
        if (secretBytes.length == 0) {
            return false;
        }
        long currentStep = now.getEpochSecond() / TIME_STEP_SECONDS;
        int drift = Math.max(0, allowedDrift);
        for (int offset = -drift; offset <= drift; offset++) {
            String expected = generateCodeForStep(secretBytes, currentStep + offset);
            if (MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    normalizedCode.getBytes(StandardCharsets.UTF_8)
            )) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates the 6-digit TOTP code for a given instant (used in tests and verification).
     */
    public String generateCode(String base32Secret, Instant at) {
        byte[] secretBytes = base32Decode(base32Secret);
        long step = at.getEpochSecond() / TIME_STEP_SECONDS;
        return generateCodeForStep(secretBytes, step);
    }

    private static String generateCodeForStep(byte[] secretBytes, long timeStep) {
        try {
            byte[] counter = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to compute TOTP code", ex);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                sb.append(BASE32_ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String base32) {
        String clean = base32.trim().replace("=", "").replace(" ", "").toUpperCase();
        byte[] out = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32_ALPHABET.indexOf(clean.charAt(i));
            if (val < 0) {
                return new byte[0];
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return Arrays.copyOf(out, count);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
