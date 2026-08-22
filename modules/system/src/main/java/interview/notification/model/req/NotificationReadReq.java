package interview.notification.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

/**
 * 标记单条消息已读请求。
 */
@Data
@Schema(description = "标记单条消息已读请求")
public class NotificationReadReq {

    @AssertTrue(message = "仅允许将消息标记为已读")
    @Schema(description = "是否已读，仅允许提交 true", example = "true")
    private Boolean isRead;
}
