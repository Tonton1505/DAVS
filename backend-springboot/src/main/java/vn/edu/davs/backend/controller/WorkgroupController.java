package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.WorkgroupService;

@RestController
@RequestMapping("/api/v1/workgroups")
public class WorkgroupController {
    private final WorkgroupService service;

    public WorkgroupController(WorkgroupService service) {
        this.service = service;
    }
}
