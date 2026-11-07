package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.ConnectedAppService;

@RestController
@RequestMapping("/api/v1/connectedapps")
public class ConnectedAppController {
    private final ConnectedAppService service;

    public ConnectedAppController(ConnectedAppService service) {
        this.service = service;
    }
}
