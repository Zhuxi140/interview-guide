package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.resume.model.enums.AnalyzeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "简历上传响应")
public record ResumeUploadVO(
        @Schema(description = "简历ID")
        Long id,
        @Schema(description = "文件名")
        String fileName,
        @Schema(description = "文件大小（字节）")
        Long fileSize,
        @Schema(description = "服务端检测的媒体类型")
        String detectedMediaType,
        @Schema(description = "分析状态")
        AnalyzeStatus analyzeStatus,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
