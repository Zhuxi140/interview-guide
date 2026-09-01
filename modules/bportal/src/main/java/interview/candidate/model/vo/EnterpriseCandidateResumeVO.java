package interview.candidate.model.vo;

import interview.candidate.model.enums.ResumeImportItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "人才池候选人导入简历摘要")
public record EnterpriseCandidateResumeVO(
        @Schema(description = "导入明细 ID") Long itemId,
        @Schema(description = "文件名") String fileName,
        @Schema(description = "处理状态") ResumeImportItemStatus status,
        @Schema(description = "失败原因") String failureReason
) {
}
