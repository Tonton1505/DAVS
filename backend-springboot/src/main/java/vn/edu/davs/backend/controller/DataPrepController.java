package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.DataPrepService;

@RestController
@RequestMapping("/api/v1/datapreps")
public class DataPrepController {
    private final DataPrepService service;

    public DataPrepController(DataPrepService service) {
        this.service = service;
    }
}
