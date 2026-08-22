package interview.rag.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 面试模板绑定知识库响应（完整替换后的生效集合）。
 */
@Schema(description = "面试模板绑定知识库响应")
public record TemplateKnowledgeBindVO(
        @Schema(description = "模板 ID")
        Long templateId,
        @Schema(description = "绑定生效的知识库 ID 集合（已去重）", example = "[1001, 1002]")
        List<Long> knowledgeBaseIds,
        @Schema(description = "模板当前版本号（绑定集合为独立聚合，不递增模板版本）", example = "0")
        Integer templateVersion
) {
}
