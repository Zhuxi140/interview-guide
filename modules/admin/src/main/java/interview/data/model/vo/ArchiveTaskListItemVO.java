package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 数据归档任务列表项。
 */
@Schema(description = "数据归档任务列表项")
public record ArchiveTaskListItemVO(
        @Schema(description = "归档任务 ID")
        Long taskId,
        @Schema(description = "资源类型", example = "API_LOG")
        String resourceType,
        @Schema(description = "任务状态", example = "COMPLETED")
        String status,
        @Schema(description = "已归档条数")
        Long archivedCount,
        @Schema(description = "创建时间")
        OffsetDateTime createdAt,
        @Schema(description = "完成时间，未完成为 null")
        OffsetDateTime completedAt
) {
}
