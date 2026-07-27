package interview.infra.localMessage.model.vo;

import interview.common.enums.MsgStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 本地消息人工重试结果。
 */
@Schema(description = "本地消息人工重试结果")
public record LocalMessageRetryVO(
        @Schema(description = "消息 ID")
        Long id,
        @Schema(description = "重新调度后的状态")
        MsgStatus status,
        @Schema(description = "下次执行时间")
        OffsetDateTime nextRetryAt
) {
}
