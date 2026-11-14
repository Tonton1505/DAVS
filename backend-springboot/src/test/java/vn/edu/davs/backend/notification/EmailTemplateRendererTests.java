package vn.edu.davs.backend.notification;

import vn.edu.davs.backend.enu.EmailTemplate;

import vn.edu.davs.backend.service.EmailTemplateRenderer;
import vn.edu.davs.backend.service.EmailTemplateRenderer.RenderedEmail;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRendererTests {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer("DAVS", "http://localhost:3000");

    @Test
    void rendersAllFiveTemplatesAndEscapesHtml() {
        for (EmailTemplate template : EmailTemplate.values()) {
            RenderedEmail email = renderer.render(template, Map.of(
                    "inviterName", "<script>alert(1)</script>",
                    "datasourceName", "Sales DB",
                    "errorMessage", "Connection refused",
                    "senderName", "Minh",
                    "resourceName", "Q4 Revenue",
                    "authorName", "An",
                    "commentExcerpt", "<b>Check</b> @minh"));

            assertThat(email.subject()).contains("[DAVS]");
            assertThat(email.htmlBody()).contains("<!DOCTYPE html>");
            assertThat(email.htmlBody()).doesNotContain("<script>");
        }
    }
}
