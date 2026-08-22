package interview.compliance.model.req;

import interview.compliance.model.enums.PolicyAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 更新用户 API 策略请求（半量更新 + 乐观锁）。
 */
@Data
@Schema(description = "更新用户 API 策略请求")
public class ApiPolicyUpdateReq {

    @Size(max = 256, message = "API 路径模板不能超过 256 个字符")
    @Schema(description = "受限 Ant 风格路径模板；为空表示匹配全部，不传保持不变",
            example = "/api/v1/resumes/**")
    private String apiPathPattern;

    @Min(value = 1, message = "限流次数必须为正整数")
    @Schema(description = "时间窗口内允许次数；仅 RATE_LIMIT 策略可更新", example = "30")
    private Integer limitCount;

    @Min(value = 1, message = "时间窗口秒数必须为正整数")
    @Schema(description = "时间窗口秒数；仅 RATE_LIMIT 策略可更新", example = "60")
    private Integer limitSeconds;

    @Schema(description = "动作：ALLOW 放行 / BLOCK 拦截；不传保持不变", example = "BLOCK")
    private PolicyAction actionType;

    @Schema(description = "策略过期时间；不传保持不变，传空值表示改为永久", example = "2026-12-31T23:59:59+08:00")
    private OffsetDateTime expireTime;

    @Size(max = 128, message = "风控原因不能超过 128 个字符")
    @Schema(description = "风控原因备注；不传保持不变", example = "接口刷量监控触发")
    private String reason;

    @NotNull(message = "期望版本号不能为空")
    @Min(value = 0, message = "版本号不能小于 0")
    @Max(value = Integer.MAX_VALUE, message = "版本号超出范围")
    @Schema(description = "期望版本号（乐观锁）", example = "0")
    private Integer expectedVersion;
}
