package vn.edu.davs.backend.controller;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Query Engine execution (UC15).
 */
@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public ResponseEntity<QueryResult> query(@RequestBody QuerySpec spec) {
        return ResponseEntity.ok(queryService.executeQuery(spec));
    }
}
