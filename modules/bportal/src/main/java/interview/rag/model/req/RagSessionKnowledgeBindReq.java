package interview.rag.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * RAG 会话绑定知识库请求（完整替换语义）。
 */
@Data
@Schema(description = "RAG 会话绑定知识库请求（完整替换语义）")
public class RagSessionKnowledgeBindReq {

    @NotEmpty(message = "知识库 ID 列表不能为空")
    @Size(max = 20, message = "单次绑定知识库数量不能超过 20")
    @Schema(description = "绑定的知识库 ID 集合（全量替换，重复 ID 自动去重）", example = "[1001, 1002]")
    private List<Long> knowledgeBaseIds;
}
