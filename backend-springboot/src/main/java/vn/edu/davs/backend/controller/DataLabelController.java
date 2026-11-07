package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.DataLabelService;

@RestController
@RequestMapping("/api/v1/datalabels")
public class DataLabelController {
    private final DataLabelService service;

    public DataLabelController(DataLabelService service) {
        this.service = service;
    }
}
