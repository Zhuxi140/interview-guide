package interview.notification.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建通知模板请求。
 */
@Data
@Schema(description = "创建通知模板请求")
public class NotificationTemplateCreateReq {

    @NotBlank(message = "业务场景不能为空")
    @Pattern(regexp = "^(INTERVIEW_INVITE|INTERVIEW_CANCEL|OFFER_SENT|OFFER_DECIDED|REPORT_READY|SYSTEM)$",
            message = "业务场景取值不合法")
    @Schema(description = "业务场景", example = "INTERVIEW_INVITE")
    private String notifyScene;

    @NotBlank(message = "渠道类型不能为空")
    @Pattern(regexp = "^(IN_APP|EMAIL|SMS)$", message = "渠道类型仅支持 IN_APP/EMAIL/SMS")
    @Schema(description = "发送渠道", example = "EMAIL")
    private String channelType;

    @NotBlank(message = "标题模板不能为空")
    @Size(max = 128, message = "标题模板最长 128 位")
    @Schema(description = "标题模板，支持白名单占位符", example = "${candidateName} 的面试邀请")
    private String title;

    @NotBlank(message = "正文模板不能为空")
    @Schema(description = "正文模板，支持白名单占位符，禁止提交渲染后全文",
            example = "${candidateName} 您好，您有一场 ${interviewTime} 的面试")
    private String contentTemplate;
}
