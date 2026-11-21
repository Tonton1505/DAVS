package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.ExportLimitsDto;
import vn.edu.davs.backend.dto.ExportRequest;
import vn.edu.davs.backend.model.DavsUser;
import org.springframework.http.MediaType;

public interface ExportService {


    public record ExportResult(
            byte[] data,
            MediaType mediaType,
            String filename
    ) {
    }


    ExportLimitsDto getLimits();


    ExportResult export(ExportRequest request, DavsUser actor);
}
