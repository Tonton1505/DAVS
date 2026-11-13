package com.davs.datasource;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.error.GlobalExceptionHandler;
import com.davs.datasource.controller.DataModelController;
import com.davs.datasource.domain.DataType;
import com.davs.datasource.domain.FieldRole;
import com.davs.datasource.domain.JoinType;
import com.davs.datasource.dto.DataModelDto;
import com.davs.datasource.dto.FieldDto;
import com.davs.datasource.dto.PreviewModelRequest;
import com.davs.datasource.dto.PreviewModelResponse;
import com.davs.datasource.dto.RelationshipDto;
import com.davs.datasource.dto.SuggestRelationshipsRequest;
import com.davs.datasource.dto.TableDto;
import com.davs.datasource.service.DataModelService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DataModelControllerTests {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private DataModelService dataModelService;

    private UUID datasourceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DataModelController(dataModelService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        datasourceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/datasources/{id}/model trả về 200 OK")
    void getModel_shouldReturn200() throws Exception {
        DataModelDto dto = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("Orders").build()))
                .relationships(List.of())
                .fields(List.of())
                .version(1)
                .build();

        when(dataModelService.getModel(datasourceId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/datasources/" + datasourceId + "/model"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasourceId").value(datasourceId.toString()))
                .andExpect(jsonPath("$.tables[0].name").value("Orders"));
    }

    @Test
    @DisplayName("PUT /api/v1/datasources/{id}/model thành công trả về 200 OK")
    void saveModel_shouldReturn200() throws Exception {
        DataModelDto input = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("Orders").build()))
                .relationships(List.of())
                .fields(List.of(FieldDto.builder().name("Total").dataType(DataType.NUMBER).role(FieldRole.MEASURE).table("Orders").build()))
                .version(1)
                .build();

        when(dataModelService.saveModel(eq(datasourceId), any(DataModelDto.class))).thenReturn(input);

        mockMvc.perform(put("/api/v1/datasources/" + datasourceId + "/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasourceId").value(datasourceId.toString()))
                .andExpect(jsonPath("$.fields[0].name").value("Total"));
    }

    @Test
    @DisplayName("PUT /api/v1/datasources/{id}/model khi lỗi ERR-DAT-06 trả về 422 Unprocessable Entity")
    void saveModel_whenIncompatibleKey_shouldReturn422() throws Exception {
        DataModelDto input = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("Orders").build()))
                .relationships(List.of())
                .fields(List.of())
                .build();

        when(dataModelService.saveModel(eq(datasourceId), any(DataModelDto.class)))
                .thenThrow(new DavsException(ErrorCode.ERR_DAT_06));

        mockMvc.perform(put("/api/v1/datasources/" + datasourceId + "/model")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ERR-DAT-06"))
                .andExpect(jsonPath("$.message").value(ErrorCode.ERR_DAT_06.getMessageTemplate()));
    }

    @Test
    @DisplayName("POST /api/v1/datasources/{id}/model/suggest-relationships trả về 200 OK kèm danh sách gợi ý")
    void suggestRelationships_shouldReturn200() throws Exception {
        SuggestRelationshipsRequest req = SuggestRelationshipsRequest.builder()
                .tables(List.of(TableDto.builder().name("Orders").build()))
                .fields(List.of(FieldDto.builder().name("Id").dataType(DataType.STRING).table("Orders").build()))
                .build();

        RelationshipDto rel = RelationshipDto.builder()
                .fromTable("Orders")
                .fromField("Id")
                .toTable("Customers")
                .toField("Id")
                .joinType(JoinType.RELATIONSHIP)
                .build();

        when(dataModelService.suggestRelationships(eq(datasourceId), any(SuggestRelationshipsRequest.class)))
                .thenReturn(List.of(rel));

        mockMvc.perform(post("/api/v1/datasources/" + datasourceId + "/model/suggest-relationships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromTable").value("Orders"))
                .andExpect(jsonPath("$[0].joinType").value("Relationship"));
    }

    @Test
    @DisplayName("POST /api/v1/datasources/{id}/model/preview trả về 200 OK kèm mẫu xem trước dữ liệu")
    void previewModel_shouldReturn200() throws Exception {
        DataModelDto model = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("Orders").build()))
                .relationships(List.of())
                .fields(List.of(FieldDto.builder().name("Total").dataType(DataType.NUMBER).role(FieldRole.MEASURE).table("Orders").build()))
                .build();

        PreviewModelRequest req = PreviewModelRequest.builder()
                .model(model)
                .limit(5)
                .build();

        PreviewModelResponse res = PreviewModelResponse.builder()
                .columns(List.of("Total"))
                .rows(List.of(Map.of("Total", 1500)))
                .totalRows(1)
                .affectedRows(0)
                .build();

        when(dataModelService.previewModel(eq(datasourceId), any(PreviewModelRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/v1/datasources/" + datasourceId + "/model/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0]").value("Total"))
                .andExpect(jsonPath("$.totalRows").value(1));
    }
}
