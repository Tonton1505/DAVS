package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.ShareService;

@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {
    private final ShareService service;

    public ShareController(ShareService service) {
        this.service = service;
    }
}
