package interview.infra.localMessage.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 批量重试请求
 * @since 2026/07/18
 */
@Data
@Schema(description = "批量重试请求")
public class BatchRetryReq {
    @Schema(description = "消息ID列表")
    @NotEmpty(message = "ID列表不能为空")
    @Size(max = 100, message = "单次最多重试 100 条消息")
    private List<@NotNull(message = "消息 ID 不能为空") Long> ids;
}
