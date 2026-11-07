package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.DataSourceService;

@RestController
@RequestMapping("/api/v1/datasources")
public class DataSourceController {
    private final DataSourceService service;

    public DataSourceController(DataSourceService service) {
        this.service = service;
    }
}
