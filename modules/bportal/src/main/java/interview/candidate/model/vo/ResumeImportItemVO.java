package interview.candidate.model.vo;

import interview.candidate.model.enums.ResumeImportItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "简历导入明细")
public record ResumeImportItemVO(
        @Schema(description = "明细 ID") Long itemId,
        @Schema(description = "原始文件名") String fileName,
        @Schema(description = "处理状态") ResumeImportItemStatus status,
        @Schema(description = "成功后生成或归并的人才池候选人 ID") Long candidateId,
        @Schema(description = "失败原因") String failureReason
) {
}
