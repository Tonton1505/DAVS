package com.davs.datasource;

import com.davs.common.error.GlobalExceptionHandler;
import com.davs.datasource.controller.DataSourceController;
import com.davs.datasource.domain.Frequency;
import com.davs.datasource.domain.RefreshType;
import com.davs.datasource.dto.DataSourceSummaryDto;
import com.davs.datasource.dto.DependentWorkbookDto;
import com.davs.datasource.dto.RefreshRunDto;
import com.davs.datasource.dto.RefreshScheduleDto;
import com.davs.datasource.dto.UpdateModeRequest;
import com.davs.datasource.service.DataSourceManageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DataSourceControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DataSourceManageService dataSourceManageService;

    private UUID datasourceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DataSourceController(dataSourceManageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        datasourceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/datasources?managed=true trả về 200 OK")
    void listDataSources_shouldReturn200() throws Exception {
        DataSourceSummaryDto dto = DataSourceSummaryDto.builder()
                .id(datasourceId)
                .name("Sales_CRM")
                .type("POSTGRESQL")
                .mode("EXTRACT")
                .status("ACTIVE")
                .dependentWorkbooksCount(2)
                .build();

        when(dataSourceManageService.listManagedDataSources()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/datasources?managed=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sales_CRM"))
                .andExpect(jsonPath("$[0].dependentWorkbooksCount").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/datasources/{id}/mode đổi chế độ trả về 200 OK")
    void updateMode_shouldReturn200() throws Exception {
        UpdateModeRequest req = UpdateModeRequest.builder().mode("EXTRACT").build();
        DataSourceSummaryDto res = DataSourceSummaryDto.builder().id(datasourceId).mode("EXTRACT").build();

        when(dataSourceManageService.updateMode(eq(datasourceId), any(UpdateModeRequest.class))).thenReturn(res);

        mockMvc.perform(put("/api/v1/datasources/" + datasourceId + "/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("EXTRACT"));
    }

    @Test
    @DisplayName("PUT /api/v1/datasources/{id}/refresh-schedule lưu lịch trả về 200 OK")
    void setRefreshSchedule_shouldReturn200() throws Exception {
        RefreshScheduleDto req = RefreshScheduleDto.builder()
                .frequency(Frequency.DAILY)
                .runTime("04:00")
                .timeZone("Asia/Ho_Chi_Minh")
                .refreshType(RefreshType.FULL)
                .build();

        when(dataSourceManageService.setRefreshSchedule(eq(datasourceId), any(RefreshScheduleDto.class))).thenReturn(req);

        mockMvc.perform(put("/api/v1/datasources/" + datasourceId + "/refresh-schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runTime").value("04:00"));
    }

    @Test
    @DisplayName("POST /api/v1/datasources/{id}/refresh trả về 202 ACCEPTED")
    void triggerRefresh_shouldReturn202Accepted() throws Exception {
        RefreshRunDto run = RefreshRunDto.builder()
                .id(UUID.randomUUID())
                .datasourceId(datasourceId)
                .status("SUCCESS")
                .rowsProcessed(25000L)
                .startedAt(Instant.now())
                .build();

        when(dataSourceManageService.triggerRefresh(datasourceId)).thenReturn(run);

        mockMvc.perform(post("/api/v1/datasources/" + datasourceId + "/refresh"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/datasources/{id}/dependents trả về 200 OK")
    void getDependents_shouldReturn200() throws Exception {
        DependentWorkbookDto wb = DependentWorkbookDto.builder()
                .id(UUID.randomUUID())
                .name("Dashboard doanh thu")
                .build();

        when(dataSourceManageService.getDependentWorkbooks(datasourceId)).thenReturn(List.of(wb));

        mockMvc.perform(get("/api/v1/datasources/" + datasourceId + "/dependents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dashboard doanh thu"));
    }
}
