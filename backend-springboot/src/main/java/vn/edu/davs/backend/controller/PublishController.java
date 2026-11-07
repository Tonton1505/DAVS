package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.PublishService;

@RestController
@RequestMapping("/api/v1/publishs")
public class PublishController {
    private final PublishService service;

    public PublishController(PublishService service) {
        this.service = service;
    }
}
