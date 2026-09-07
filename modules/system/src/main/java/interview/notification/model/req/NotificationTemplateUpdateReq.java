package interview.notification.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑通知模板请求（携带 expectedVersion CAS）。
 */
@Data
@Schema(description = "编辑通知模板请求")
public class NotificationTemplateUpdateReq {

    @Size(max = 128, message = "标题模板最长 128 位")
    @Schema(description = "标题模板，不提交则不修改", example = "${candidateName} 的面试邀请")
    private String title;

    @Schema(description = "正文模板，不提交则不修改")
    private String contentTemplate;

    @Schema(description = "模板启停，不提交则不修改", example = "true")
    private Boolean enabled;

    @NotNull(message = "期望版本不能为空")
    @Min(value = 0, message = "期望版本不能小于 0")
    @Schema(description = "客户端读取到的模板版本号", example = "0")
    private Integer expectedVersion;
}
