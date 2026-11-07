package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.DataModelService;

@RestController
@RequestMapping("/api/v1/datamodels")
public class DataModelController {
    private final DataModelService service;

    public DataModelController(DataModelService service) {
        this.service = service;
    }
}
