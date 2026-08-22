package interview.knowledge.model.req;

import interview.knowledge.model.enums.KnowledgeVisibility;
import interview.knowledge.model.enums.VectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 企业知识库分页查询参数（企业私有文档 + 可用全局文档）。
 */
@Data
@Schema(description = "企业知识库分页查询参数")
public class EnterpriseKnowledgeBaseSearchReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "可见性筛选：GLOBAL 仅全局文档，PRIVATE 仅本企业私有文档；为空时两者都返回",
            example = "PRIVATE", allowableValues = {"GLOBAL", "PRIVATE"})
    private KnowledgeVisibility visibility;

    @Schema(description = "向量化状态筛选；为空时返回全部状态",
            example = "COMPLETED", allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED"})
    private VectorStatus vectorStatus;
}
