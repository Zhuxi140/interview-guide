package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.resume.model.enums.AnalyzeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 简历详情响应
 */
@Builder
@Schema(description = "简历详情响应")
public record ResumeVO(
        @Schema(description = "简历ID")
        Long id,
        @Schema(description = "用户ID")
        Long userId,
        @Schema(description = "文件名")
        String fileName,
        @Schema(description = "文件大小（字节）")
        Long fileSize,
        @Schema(description = "文件类型")
        String fileType,
        @Schema(description = "文件哈希")
        String fileHash,
        @Schema(description = "存储URL")
        String storageUrl,
        @Schema(description = "简历文本内容")
        String resumeText,
        @Schema(description = "分析状态")
        AnalyzeStatus analyzeStatus,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
