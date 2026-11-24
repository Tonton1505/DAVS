package vn.edu.davs.backend.rest.impl;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.davs.backend.rest.QueryRest;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/query")
public class QueryRestImpl implements QueryRest {


    private final QueryService queryService;

    @Override
    public ResponseEntity<QueryResult> query(
            @RequestBody QuerySpec spec,
            @AuthenticationPrincipal DavsUser actor
    ) {
        return ResponseEntity.ok(queryService.executeQuery(spec, actor));
    }
}
