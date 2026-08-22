package interview.notification.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * 批量标记当前筛选范围内消息已读请求。
 */
@Data
@Schema(description = "批量标记已读请求")
public class NotificationBatchReadReq {

    @AssertTrue(message = "仅允许执行全部标记已读操作")
    @Schema(description = "是否全部标记已读，仅允许提交 true", example = "true")
    private Boolean markAllRead;

    @Schema(description = "按通知类型筛选标记范围", example = "INTERVIEW")
    private String notifyType;
}
