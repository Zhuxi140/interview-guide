package interview.matching.model.req;

import interview.common.enums.CandidateDimensionCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 岗位 AI 初筛配置更新请求。
 */
@Data
@Schema(description = "岗位 AI 初筛配置更新请求")
public class JobScreeningConfigUpdateReq {

    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    @Schema(description = "客户端读取配置时得到的版本号", example = "0")
    private Integer expectedVersion;

    @NotNull(message = "初筛开关不能为空")
    @Schema(description = "是否在投递后自动发起 AI 初筛", example = "true")
    private Boolean enabled;

    @NotNull(message = "总体阈值不能为空")
    @Min(value = 0, message = "总体阈值不能小于 0")
    @Max(value = 100, message = "总体阈值不能大于 100")
    @Schema(description = "总体匹配分阈值", example = "70")
    private Integer overallThreshold;

    @Valid
    @NotNull(message = "维度阈值不能为空")
    @Size(max = 8, message = "维度阈值数量不能超过 8 个")
    @Schema(description = "指定画像维度的最低分")
    private Map<@NotNull CandidateDimensionCode,
            @NotNull @Min(0) @Max(100) Integer> dimensionThresholds;
}
