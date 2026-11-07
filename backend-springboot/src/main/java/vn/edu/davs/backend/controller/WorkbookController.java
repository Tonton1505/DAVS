package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.WorkbookService;

@RestController
@RequestMapping("/api/v1/workbooks")
public class WorkbookController {
    private final WorkbookService service;

    public WorkbookController(WorkbookService service) {
        this.service = service;
    }
}
