package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.AdhocExploreService;

@RestController
@RequestMapping("/api/v1/adhocexplores")
public class AdhocExploreController {
    private final AdhocExploreService service;

    public AdhocExploreController(AdhocExploreService service) {
        this.service = service;
    }
}
