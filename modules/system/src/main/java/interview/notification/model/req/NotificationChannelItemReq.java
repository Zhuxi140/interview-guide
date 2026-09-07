package interview.notification.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 渠道配置项（PUT 全量更新请求的子项）。
 */
@Data
@Schema(description = "渠道配置项")
public class NotificationChannelItemReq {

    @NotBlank(message = "渠道类型不能为空")
    @Pattern(regexp = "^(IN_APP|EMAIL|SMS)$", message = "渠道类型仅支持 IN_APP/EMAIL/SMS")
    @Schema(description = "渠道类型", example = "EMAIL")
    private String channelType;

    @NotNull(message = "启用开关不能为空")
    @Schema(description = "渠道启用开关；IN_APP 恒为 true")
    private Boolean enabled;

    @Size(max = 64, message = "供应商标识最长 64 位")
    @Schema(description = "供应商标识", example = "ALIYUN_SMS")
    private String provider;

    @Schema(description = "渠道参数；凭证字段（password/secret/apiKey/token）服务端加密落盘",
            example = "{\"host\":\"smtp.example.com\",\"port\":465,\"username\":\"noreply\",\"password\":\"******\"}")
    private Map<String, Object> config;
}
