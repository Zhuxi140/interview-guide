package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.resume.model.enums.AnalyzeStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ResumeUploadVO(
        Long id,
        String fileName,
        Long fileSize,
        String fileType,
        AnalyzeStatus analyzeStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
