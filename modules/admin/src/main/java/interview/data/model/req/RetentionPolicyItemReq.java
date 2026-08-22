package interview.data.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 单类资源的数据保留策略项（更新请求子项）。
 */
@Data
@Schema(description = "数据保留策略项")
public class RetentionPolicyItemReq {

    @NotBlank(message = "资源类型不能为空")
    @Pattern(regexp = "^(API_LOG|OPERATE_LOG|INTERVIEW_TIMELINE|VOICE_MEDIA|INTERVIEW_REPORT)$",
            message = "资源类型取值不合法")
    @Schema(description = "资源类型", example = "API_LOG")
    private String resourceType;

    @NotNull(message = "热数据保留天数不能为空")
    @Min(value = 1, message = "热数据保留天数至少 1 天")
    @Max(value = 3650, message = "热数据保留天数最长 3650 天")
    @Schema(description = "热数据保留天数", example = "30")
    private Integer hotRetentionDays;

    @NotNull(message = "归档开关不能为空")
    @Schema(description = "是否启用归档", example = "true")
    private Boolean archiveEnabled;

    @Min(value = 1, message = "冷数据保留天数至少 1 天")
    @Schema(description = "冷数据保留天数，可为空表示永久保留", example = "365")
    private Integer coldRetentionDays;
}
