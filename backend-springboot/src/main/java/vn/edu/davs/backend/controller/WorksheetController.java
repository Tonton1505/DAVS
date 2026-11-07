package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.WorksheetService;

@RestController
@RequestMapping("/api/v1/worksheets")
public class WorksheetController {
    private final WorksheetService service;

    public WorksheetController(WorksheetService service) {
        this.service = service;
    }
}
