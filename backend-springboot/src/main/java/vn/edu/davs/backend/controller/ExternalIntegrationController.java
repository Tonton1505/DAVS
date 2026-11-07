package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.ExternalIntegrationService;

@RestController
@RequestMapping("/api/v1/externalintegrations")
public class ExternalIntegrationController {
    private final ExternalIntegrationService service;

    public ExternalIntegrationController(ExternalIntegrationService service) {
        this.service = service;
    }
}
