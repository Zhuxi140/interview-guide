package interview.ai.providerconfig.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 启停大模型提供商请求
 * @since 2026/7/26 14:05
 */
@Data
@Schema(description = "启停大模型提供商请求")
public class LlmProviderStatusReq {

    @NotNull(message = "乐观锁版本不能为空")
    @Schema(description = "期望版本号（CAS 乐观锁）", example = "0")
    private Integer expectedVersion;

    @NotNull(message = "路由开关不能为空")
    @Schema(description = "路由开关")
    private Boolean enabled;
}
