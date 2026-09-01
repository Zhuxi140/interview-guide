package interview.candidate.model.vo;

import interview.candidate.model.enums.ResumeImportBatchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "简历导入批次详情")
public record ResumeImportBatchVO(
        @Schema(description = "批次 ID") Long batchId,
        @Schema(description = "批次状态") ResumeImportBatchStatus status,
        @Schema(description = "文件总数") Integer totalCount,
        @Schema(description = "成功数量") Integer successCount,
        @Schema(description = "失败数量") Integer failedCount,
        @Schema(description = "创建时间") OffsetDateTime createdAt,
        @Schema(description = "完成时间") OffsetDateTime completedAt
) {
}
