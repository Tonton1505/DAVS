package vn.edu.davs.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.davs.backend.service.CalculatedFieldService;

@RestController
@RequestMapping("/api/v1/calculatedfields")
public class CalculatedFieldController {
    private final CalculatedFieldService service;

    public CalculatedFieldController(CalculatedFieldService service) {
        this.service = service;
    }
}
