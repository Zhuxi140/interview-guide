package interview.rag.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RAG 会话创建请求。
 */
@Data
@Schema(description = "RAG 会话创建请求")
public class RagSessionCreateReq {

    @Size(max = 256, message = "会话标题长度不能超过 256")
    @Schema(description = "会话标题；为空时由服务端按默认标题生成", example = "Java 后端岗位知识问答")
    private String title;
}
