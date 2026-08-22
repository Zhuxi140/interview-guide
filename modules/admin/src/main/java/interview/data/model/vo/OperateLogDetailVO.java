package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 业务操作审计日志详情（含字段级脱敏变更）。
 */
@Schema(description = "业务操作审计日志详情")
public record OperateLogDetailVO(
        @Schema(description = "日志 ID")
        Long id,
        @Schema(description = "关联同一次 HTTP 请求的链路 ID")
        String traceId,
        @Schema(description = "操作人用户 ID")
        Long userId,
        @Schema(description = "操作人用户名（批量补齐）")
        String username,
        @Schema(description = "业务模块", example = "job")
        String module,
        @Schema(description = "操作类型", example = "UPDATE")
        String operateType,
        @Schema(description = "业务资源类型", example = "JOB")
        String resourceType,
        @Schema(description = "业务资源 ID")
        Long resourceId,
        @Schema(description = "字段级脱敏变更列表")
        List<OperateLogChangeVO> changes,
        @Schema(description = "操作发生时间")
        OffsetDateTime createdAt
) {
}
