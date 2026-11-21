package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.PrepFlowDto;
import vn.edu.davs.backend.dto.PrepPreviewRequest;
import vn.edu.davs.backend.dto.PrepPreviewResponse;
import vn.edu.davs.backend.dto.PrepRunDto;
import vn.edu.davs.backend.dto.SavePrepFlowRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC12 business service for Data Preparation &amp; Cleaning flows:
 * supports FILTER, RENAME, DROP, REPLACE_NULL, DEDUPE, CAST, JOIN, UNION, PIVOT, AGGREGATE,
 * 1,000-row preview with column profiling (BUS-DAT-06), step lineage (BUS-DAT-05),
 * and read-only source isolation (BUS-DAT-04).
 */
public interface DataPrepService {


    public static final String PREP_RUN_JOB_TYPE = "PREP_RUN";


    @Transactional(readOnly = true)
    List<PrepFlowDto> listFlows(UUID datasourceId);


    @Transactional(readOnly = true)
    PrepFlowDto getFlow(UUID flowId);


    @Transactional
    PrepFlowDto createFlow(SavePrepFlowRequest request, DavsUser actor);


    @Transactional
    PrepFlowDto updateFlow(UUID flowId, SavePrepFlowRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    PrepPreviewResponse previewFlow(UUID flowId, PrepPreviewRequest request);


    @Transactional
    PrepRunDto enqueueRun(UUID flowId, boolean simulateFailure, DavsUser actor);


    @Transactional
    PrepRunDto executeRun(UUID runId, boolean simulateFailure);


    @Transactional(readOnly = true)
    List<PrepRunDto> listRuns(UUID flowId);
}
