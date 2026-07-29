package interview.ai.config.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "启停 AI 场景请求")
public class LlmSceneStatusReq {

    @NotNull(message = "乐观锁版本不能为空")
    @Schema(description = "期望版本号（CAS 乐观锁）", example = "0")
    private Integer expectedVersion;

    @NotNull(message = "场景开关不能为空")
    @Schema(description = "场景开关")
    private Boolean enabled;
}
