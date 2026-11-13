package com.davs.datasource;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.datasource.domain.DataModelEntity;
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
import com.davs.datasource.repository.DataModelRepository;
import com.davs.datasource.repository.DataSourceRepository;
import com.davs.datasource.service.DataModelService;
import com.davs.datasource.service.impl.DataModelServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataModelServiceTests {

    @Mock
    private DataModelRepository dataModelRepository;

    @Mock
    private DataSourceRepository dataSourceRepository;

    private DataModelService dataModelService;

    private UUID datasourceId;

    @BeforeEach
    void setUp() {
        dataModelService = new DataModelServiceImpl(dataModelRepository, dataSourceRepository);
        datasourceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("UC13 Luồng chính: Gợi ý quan hệ tự động khi tên và kiểu trường trùng khớp")
    void suggestRelationships_shouldSuggestWhenMatchingNameAndCompatibleType() {
        TableDto t1 = TableDto.builder().id("1").name("DonHang").build();
        TableDto t2 = TableDto.builder().id("2").name("KhachHang").build();

        FieldDto f1 = FieldDto.builder().name("Ma_KH").dataType(DataType.STRING).table("DonHang").build();
        FieldDto f2 = FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).table("DonHang").build();
        FieldDto f3 = FieldDto.builder().name("Ma_KH").dataType(DataType.STRING).table("KhachHang").build();

        SuggestRelationshipsRequest req = SuggestRelationshipsRequest.builder()
                .tables(List.of(t1, t2))
                .fields(List.of(f1, f2, f3))
                .build();

        List<RelationshipDto> suggestions = dataModelService.suggestRelationships(datasourceId, req);

        assertThat(suggestions).hasSize(1);
        RelationshipDto rel = suggestions.get(0);
        assertThat(rel.getFromTable()).isEqualTo("DonHang");
        assertThat(rel.getFromField()).isEqualTo("Ma_KH");
        assertThat(rel.getToTable()).isEqualTo("KhachHang");
        assertThat(rel.getToField()).isEqualTo("Ma_KH");
        assertThat(rel.getJoinType()).isEqualTo(JoinType.RELATIONSHIP); // BUS-DAT-07
    }

    @Test
    @DisplayName("UC13 Luồng 4a: Không có trường trùng khớp thì trả về danh sách rỗng để người dùng chọn thủ công")
    void suggestRelationships_shouldReturnEmptyWhenNoMatches() {
        TableDto t1 = TableDto.builder().id("1").name("DonHang").build();
        TableDto t2 = TableDto.builder().id("2").name("KhachHang").build();

        FieldDto f1 = FieldDto.builder().name("OrderID").dataType(DataType.NUMBER).table("DonHang").build();
        FieldDto f2 = FieldDto.builder().name("CustomerName").dataType(DataType.STRING).table("KhachHang").build();

        SuggestRelationshipsRequest req = SuggestRelationshipsRequest.builder()
                .tables(List.of(t1, t2))
                .fields(List.of(f1, f2))
                .build();

        List<RelationshipDto> suggestions = dataModelService.suggestRelationships(datasourceId, req);
        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("UC13 Luồng 6a: Kiểu dữ liệu khóa quan hệ không tương thích ném ERR-DAT-06")
    void saveModel_shouldThrowERRDAT06WhenRelationshipKeyTypesIncompatible() {
        FieldDto f1 = FieldDto.builder().name("CustomerID").dataType(DataType.STRING).table("Orders").build();
        FieldDto f2 = FieldDto.builder().name("CustomerID").dataType(DataType.NUMBER).table("Customers").build();

        RelationshipDto rel = RelationshipDto.builder()
                .fromTable("Orders")
                .fromField("CustomerID")
                .toTable("Customers")
                .toField("CustomerID")
                .joinType(JoinType.RELATIONSHIP)
                .build();

        DataModelDto model = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(
                        TableDto.builder().name("Orders").build(),
                        TableDto.builder().name("Customers").build()
                ))
                .relationships(List.of(rel))
                .fields(List.of(f1, f2))
                .build();

        assertThatThrownBy(() -> dataModelService.saveModel(datasourceId, model))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> {
                    DavsException de = (DavsException) ex;
                    assertThat(de.getErrorCode()).isEqualTo(ErrorCode.ERR_DAT_06);
                });
    }

    @Test
    @DisplayName("BUS-DAT-09: Blend chỉ áp dụng cho hai bảng thuộc nguồn khác nhau; cùng nguồn thì từ chối")
    void saveModel_shouldRejectBlendWhenSameDataSource() {
        UUID sourceA = UUID.randomUUID();

        FieldDto f1 = FieldDto.builder().name("ID").dataType(DataType.STRING).table("A").build();
        FieldDto f2 = FieldDto.builder().name("ID").dataType(DataType.STRING).table("B").build();

        RelationshipDto rel = RelationshipDto.builder()
                .fromTable("A")
                .fromField("ID")
                .toTable("B")
                .toField("ID")
                .joinType(JoinType.BLEND)
                .fromSourceId(sourceA)
                .toSourceId(sourceA) // Cùng một nguồn
                .build();

        DataModelDto model = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("A").build(), TableDto.builder().name("B").build()))
                .relationships(List.of(rel))
                .fields(List.of(f1, f2))
                .build();

        assertThatThrownBy(() -> dataModelService.saveModel(datasourceId, model))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> {
                    DavsException de = (DavsException) ex;
                    assertThat(de.getErrorCode()).isEqualTo(ErrorCode.ERR_DAT_06);
                });
    }

    @Test
    @DisplayName("BUS-DAT-08: Trường kiểu Number mặc định là Measure, trừ trường mã/ID là Dimension")
    void applyDefaultFieldRoles_shouldAssignMeasureToNumbersAndDimensionToIds() {
        FieldDto amount = FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).table("Orders").build();
        FieldDto quantity = FieldDto.builder().name("SoLuong").dataType(DataType.NUMBER).table("Orders").build();
        FieldDto orderId = FieldDto.builder().name("Ma_DonHang").dataType(DataType.NUMBER).table("Orders").build();
        FieldDto customerId = FieldDto.builder().name("customer_id").dataType(DataType.NUMBER).table("Orders").build();
        FieldDto region = FieldDto.builder().name("Vung").dataType(DataType.STRING).table("Orders").build();

        List<FieldDto> fields = new ArrayList<>(List.of(amount, quantity, orderId, customerId, region));
        dataModelService.applyDefaultFieldRoles(fields);

        assertThat(amount.getRole()).isEqualTo(FieldRole.MEASURE);
        assertThat(quantity.getRole()).isEqualTo(FieldRole.MEASURE);
        assertThat(orderId.getRole()).isEqualTo(FieldRole.DIMENSION); // ID/Mã
        assertThat(customerId.getRole()).isEqualTo(FieldRole.DIMENSION); // ID/Mã
        assertThat(region.getRole()).isEqualTo(FieldRole.DIMENSION);
    }

    @Test
    @DisplayName("UC13 Lưu mô hình thành công và cập nhật version")
    void saveModel_shouldPersistModelSuccessfully() {
        FieldDto f1 = FieldDto.builder().name("Ma_KH").dataType(DataType.STRING).table("Orders").build();
        FieldDto f2 = FieldDto.builder().name("Ma_KH").dataType(DataType.STRING).table("Customers").build();

        RelationshipDto rel = RelationshipDto.builder()
                .fromTable("Orders")
                .fromField("Ma_KH")
                .toTable("Customers")
                .toField("Ma_KH")
                .joinType(JoinType.RELATIONSHIP)
                .build();

        DataModelDto input = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("Orders").build(), TableDto.builder().name("Customers").build()))
                .relationships(List.of(rel))
                .fields(List.of(f1, f2))
                .build();

        when(dataModelRepository.findByDatasourceId(datasourceId)).thenReturn(Optional.empty());
        when(dataModelRepository.save(any(DataModelEntity.class))).thenAnswer(invocation -> {
            DataModelEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        DataModelDto result = dataModelService.saveModel(datasourceId, input);

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(1);
        verify(dataModelRepository).save(any(DataModelEntity.class));
    }

    @Test
    @DisplayName("UC13 Luồng 7a: Preview mô hình dữ liệu và kiểm tra dòng ảnh hưởng")
    void previewModel_shouldReturnColumnsRowsAndNullCounts() {
        FieldDto f1 = FieldDto.builder().name("NgayDatHang").dataType(DataType.DATE).table("Orders").build();
        FieldDto f2 = FieldDto.builder().name("DoanhSo").dataType(DataType.NUMBER).role(FieldRole.MEASURE).table("Orders").build();

        DataModelDto model = DataModelDto.builder()
                .datasourceId(datasourceId)
                .tables(List.of(TableDto.builder().name("Orders").build()))
                .relationships(List.of())
                .fields(List.of(f1, f2))
                .build();

        PreviewModelRequest req = PreviewModelRequest.builder()
                .model(model)
                .limit(10)
                .build();

        PreviewModelResponse preview = dataModelService.previewModel(datasourceId, req);

        assertThat(preview.getColumns()).containsExactly("NgayDatHang", "DoanhSo");
        assertThat(preview.getRows()).hasSize(10);
        assertThat(preview.getTotalRows()).isEqualTo(10);
    }
}
