package com.davs.query;

import com.davs.common.error.GlobalExceptionHandler;
import com.davs.query.controller.QueryController;
import com.davs.query.dto.QueryResult;
import com.davs.query.dto.QuerySpec;
import com.davs.query.service.QueryService;
import com.davs.viz.domain.ChartType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QueryControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private QueryService queryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new QueryController(queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/query thực thi truy vấn trả về 200 OK")
    void executeQuery_shouldReturn200() throws Exception {
        QuerySpec spec = QuerySpec.builder()
                .datasourceId(UUID.randomUUID())
                .limit(10)
                .build();

        QueryResult res = QueryResult.builder()
                .columns(List.of("DoanhSo"))
                .rows(List.of(Map.of("DoanhSo", 1000)))
                .totalMarks(1)
                .dataAsOf(Instant.now())
                .recommendedChartType(ChartType.BAR)
                .build();

        when(queryService.executeQuery(any(QuerySpec.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(spec)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMarks").value(1))
                .andExpect(jsonPath("$.recommendedChartType").value("Cột"));
    }
}
