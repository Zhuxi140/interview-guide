package interview.rag.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 面试模板绑定知识库请求（完整替换语义）。
 */
@Data
@Schema(description = "面试模板绑定知识库请求（完整替换语义）")
public class TemplateKnowledgeBindReq {

    @NotEmpty(message = "知识库 ID 列表不能为空")
    @Size(max = 20, message = "单次绑定知识库数量不能超过 20")
    @Schema(description = "绑定的知识库 ID 集合（全量替换，重复 ID 自动去重）", example = "[1001, 1002]")
    private List<Long> knowledgeBaseIds;

    @NotNull(message = "期望模板版本号不能为空")
    @Schema(description = "期望模板版本号，用于并发编辑防覆盖校验", example = "0")
    private Integer expectedTemplateVersion;
}
