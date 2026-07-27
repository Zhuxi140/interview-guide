package interview.infra.localMessage.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 本地消息详情
 * @since 2026/07/18
 */
@Builder
@Schema(description = "本地消息详情")
public record LocalMessageDetailVO(
        @Schema(description = "消息ID")
        Long id,
        @Schema(description = "消息主题")
        String topic,
        @Schema(description = "业务幂等键")
        String bizKey,
        @Schema(description = "消息协议版本")
        Integer schemaVersion,
        @Schema(description = "消息载荷")
        String payload,
        @Schema(description = "优先级 HIGH/MEDIUM/LOW")
        String priority,
        @Schema(description = "消息状态")
        String status,
        @Schema(description = "已重试次数")
        Integer retryCount,
        @Schema(description = "最大重试次数")
        Integer maxRetries,
        @Schema(description = "下次重试时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime nextRetryAt,
        @Schema(description = "重试历史")
        String retryHistory,
        @Schema(description = "最后一次错误信息")
        String lastError,
        @Schema(description = "当前租约持有者")
        String leaseOwner,
        @Schema(description = "当前租约截止时间")
        OffsetDateTime leaseUntil,
        @Schema(description = "租约栅栏版本")
        Long leaseVersion,
        @Schema(description = "链路追踪ID")
        String traceId,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
