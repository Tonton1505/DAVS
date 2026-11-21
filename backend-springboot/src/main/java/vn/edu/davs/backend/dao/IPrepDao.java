package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.PrepFlowDto;
import vn.edu.davs.backend.dto.PrepRunDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for UC12 data preparation flows ({@code prep_flows}) and execution runs ({@code prep_runs}).
 */
public interface IPrepDao {


    List<PrepFlowDto> listFlows(UUID datasourceId);


    Optional<PrepFlowDto> findFlowById(UUID flowId);


    UUID insertFlow(
            UUID datasourceId,
            String name,
            String description,
            List<PrepFlowDto.PrepStepDto> steps,
            List<PrepFlowDto.LineageStepDto> lineage,
            UUID ownerId,
            boolean published
    );


    void updateFlow(
            UUID flowId,
            UUID datasourceId,
            String name,
            String description,
            List<PrepFlowDto.PrepStepDto> steps,
            List<PrepFlowDto.LineageStepDto> lineage,
            boolean published
    );


    UUID insertRun(UUID flowId, UUID jobId, UUID createdBy);


    void markRunRunning(UUID runId);


    void completeRunSuccess(
            UUID runId,
            UUID flowId,
            long inputRows,
            long outputRows,
            long removedDuplicates,
            long replacedNulls,
            List<PrepFlowDto.LineageStepDto> lineage
    );


    void completeRunFailure(UUID runId, String errorCode, String errorMessage);


    Optional<PrepRunDto> findRunById(UUID runId);


    List<PrepRunDto> listRuns(UUID flowId);
}
