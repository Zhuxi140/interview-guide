package interview.ai.config.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新 AI 全局默认路由请求")
public class AiGlobalRouteUpdateReq {

    @NotBlank(message = "Provider ID不能为空")
    @Schema(description = "新的默认 Provider ID", example = "dashscope-chat")
    private String providerId;

    @NotNull(message = "乐观锁版本不能为空")
    @Min(value = 0, message = "乐观锁版本不能小于0")
    @Schema(description = "期望版本号（CAS 乐观锁）", example = "0")
    private Integer expectedVersion;
}
