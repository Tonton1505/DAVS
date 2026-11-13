package com.davs.viz;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.error.GlobalExceptionHandler;
import com.davs.viz.controller.SheetController;
import com.davs.viz.dto.SheetDto;
import com.davs.viz.service.SheetService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SheetControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SheetService sheetService;

    private UUID workbookId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SheetController(sheetService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        workbookId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/workbooks/{id}/sheets trả về 200 OK")
    void getSheets_shouldReturn200() throws Exception {
        SheetDto s = SheetDto.builder().id(UUID.randomUUID()).workbookId(workbookId).name("Sheet 1").build();

        when(sheetService.getSheets(workbookId)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/v1/workbooks/" + workbookId + "/sheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sheet 1"));
    }

    @Test
    @DisplayName("POST /api/v1/workbooks/{id}/sheets tạo sheet thành công trả về 201 Created")
    void createSheet_shouldReturn201() throws Exception {
        SheetDto input = SheetDto.builder().name("Sheet Mới").build();
        SheetDto created = SheetDto.builder().id(UUID.randomUUID()).workbookId(workbookId).name("Sheet Mới").build();

        when(sheetService.createSheet(eq(workbookId), any(SheetDto.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/workbooks/" + workbookId + "/sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sheet Mới"));
    }

    @Test
    @DisplayName("POST /api/v1/workbooks/{id}/sheets trùng tên trả về 409 Conflict (ERR-VIZ-05)")
    void createSheet_whenDuplicateName_shouldReturn409() throws Exception {
        SheetDto input = SheetDto.builder().name("Trùng Tên").build();

        when(sheetService.createSheet(eq(workbookId), any(SheetDto.class)))
                .thenThrow(new DavsException(ErrorCode.ERR_VIZ_05));

        mockMvc.perform(post("/api/v1/workbooks/" + workbookId + "/sheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERR-VIZ-05"))
                .andExpect(jsonPath("$.message").value(ErrorCode.ERR_VIZ_05.getMessageTemplate()));
    }

    @Test
    @DisplayName("DELETE /api/v1/workbooks/{id}/sheets/{sheetId} trả về 204 No Content")
    void deleteSheet_shouldReturn204() throws Exception {
        UUID sheetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/workbooks/" + workbookId + "/sheets/" + sheetId))
                .andExpect(status().isNoContent());
    }
}
