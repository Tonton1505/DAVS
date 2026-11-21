package vn.edu.davs.backend.rest;

import vn.edu.davs.backend.dto.QueryResult;
import vn.edu.davs.backend.dto.QuerySpec;
import vn.edu.davs.backend.model.DavsUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for Query Engine execution (UC15 & T-101 RLS).
 */
@RequestMapping("/api/v1/query")
public interface QueryRest {


    @PostMapping
    ResponseEntity<QueryResult> query(
            @RequestBody QuerySpec spec,
            @AuthenticationPrincipal DavsUser actor
    );
}
