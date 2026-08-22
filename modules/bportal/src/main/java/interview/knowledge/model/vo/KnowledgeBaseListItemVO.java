package interview.knowledge.model.vo;

import interview.knowledge.model.enums.KnowledgeVisibility;
import interview.knowledge.model.enums.VectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 知识库列表项响应。
 */
@Builder
@Schema(description = "知识库列表项响应")
public record KnowledgeBaseListItemVO(
        @Schema(description = "知识库 ID")
        Long id,
        @Schema(description = "知识库名称")
        String name,
        @Schema(description = "原始文件名")
        String fileName,
        @Schema(description = "文件大小（字节）", example = "1048576")
        Long fileSize,
        @Schema(description = "可见性：GLOBAL 公共底座 / PRIVATE 企业私有")
        KnowledgeVisibility visibility,
        @Schema(description = "向量化状态：PENDING / PROCESSING / COMPLETED / FAILED")
        VectorStatus vectorStatus,
        @Schema(description = "最近一次向量化失败原因，成功后为空")
        String failureReason,
        @Schema(description = "向量分块数量", example = "128")
        Integer chunkCount,
        @Schema(description = "上传时间")
        OffsetDateTime uploadedAt
) {
}
