package interview.notification.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 渠道配置全量更新请求（按版本 CAS）。
 */
@Data
@Schema(description = "渠道配置全量更新请求")
public class NotificationChannelsUpdateReq {

    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    @Schema(description = "客户端读取到的聚合版本号（各渠道行版本最大值）", example = "0")
    private Integer expectedVersion;

    @NotEmpty(message = "渠道配置不能为空")
    @Valid
    @Schema(description = "渠道配置列表")
    private List<NotificationChannelItemReq> channels;
}
