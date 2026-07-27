package interview.infra.localMessage.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 本地消息分页列表项
 * @since 2026/07/18
 */
@Builder
@Schema(description = "本地消息分页列表项")
public record LocalMessagePageVO(
        @Schema(description = "消息ID")
        Long id,
        @Schema(description = "消息主题")
        String topic,
        @Schema(description = "业务幂等键")
        String bizKey,
        @Schema(description = "优先级 HIGH/MEDIUM/LOW")
        String priority,
        @Schema(description = "消息状态")
        String status,
        @Schema(description = "已重试次数")
        Integer retryCount,
        @Schema(description = "最大重试次数")
        Integer maxRetries,
        @Schema(description = "下次执行时间")
        OffsetDateTime nextRetryAt,
        @Schema(description = "当前租约截止时间")
        OffsetDateTime leaseUntil,
        @Schema(description = "最后一次错误信息")
        String lastError,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
