package vn.edu.davs.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Recursively redacts sensitive fields (passwords, tokens, secrets, cookies, keys)
 * from audit state payloads before persistence.
 */
@Component
@RequiredArgsConstructor
public class SensitiveDataMasker {

    public static final String REDACTED = "***REDACTED***";

    private static final List<String> SENSITIVE_KEY_FRAGMENTS = List.of(
            "password",
            "passwd",
            "pwd",
            "token",
            "secret",
            "apikey",
            "authorization",
            "cookie",
            "privatekey",
            "encryptionkey",
            "credential",
            "otp",
            "pin"
    );

    private final ObjectMapper objectMapper;

    /**
     * Converts an arbitrary state object into a masked {@link JsonNode}, or returns {@code null}
     * if {@code state} is {@code null}.
     */
    public JsonNode mask(Object state) {
        if (state == null) {
            return null;
        }
        JsonNode root = state instanceof JsonNode node
                ? node.deepCopy()
                : objectMapper.valueToTree(state);
        return maskNode(root);
    }

    private JsonNode maskNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitiveKey(entry.getKey())) {
                    entry.setValue(TextNode.valueOf(REDACTED));
                } else {
                    entry.setValue(maskNode(entry.getValue()));
                }
            }
            return objectNode;
        }
        if (node instanceof ArrayNode arrayNode) {
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, maskNode(arrayNode.get(i)));
            }
            return arrayNode;
        }
        return node;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]", "");
        for (String fragment : SENSITIVE_KEY_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
