package interview.knowledge.model.req;

import interview.knowledge.model.enums.VectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 平台全局知识库分页查询参数。
 */
@Data
@Schema(description = "平台全局知识库分页查询参数")
public class AdminKnowledgeBaseSearchReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "向量化状态筛选；为空时返回全部状态",
            example = "COMPLETED", allowableValues = {"PENDING", "PROCESSING", "COMPLETED", "FAILED"})
    private VectorStatus vectorStatus;
}
