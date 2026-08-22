package interview.compliance.model.req;

import interview.compliance.model.enums.PolicyAction;
import interview.compliance.model.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 创建用户 API 策略请求。
 */
@Data
@Schema(description = "创建用户 API 策略请求")
public class ApiPolicyCreateReq {

    @NotNull(message = "被管控用户 ID 不能为空")
    @Schema(description = "被管控用户 ID", example = "10001")
    private Long userId;

    @Size(max = 256, message = "API 路径模板不能超过 256 个字符")
    @Schema(description = "受限 Ant 风格路径模板（如 /api/v1/resumes/**）；为空表示匹配全部",
            example = "/api/v1/resumes/**")
    private String apiPathPattern;

    @NotNull(message = "策略类型不能为空")
    @Schema(description = "策略类型：RATE_LIMIT / BLACKLIST / WHITELIST", example = "RATE_LIMIT")
    private PolicyType policyType;

    @Min(value = 1, message = "限流次数必须为正整数")
    @Schema(description = "时间窗口内允许次数；仅 RATE_LIMIT 必填", example = "60")
    private Integer limitCount;

    @Min(value = 1, message = "时间窗口秒数必须为正整数")
    @Schema(description = "时间窗口秒数；仅 RATE_LIMIT 必填", example = "60")
    private Integer limitSeconds;

    @NotNull(message = "动作类型不能为空")
    @Schema(description = "动作：ALLOW 放行 / BLOCK 拦截", example = "BLOCK")
    private PolicyAction actionType;

    @Schema(description = "策略过期时间；为空表示永久", example = "2026-12-31T23:59:59+08:00")
    private OffsetDateTime expireTime;

    @NotBlank(message = "风控原因不能为空")
    @Size(max = 128, message = "风控原因不能超过 128 个字符")
    @Schema(description = "风控原因备注", example = "接口刷量监控触发")
    private String reason;
}
