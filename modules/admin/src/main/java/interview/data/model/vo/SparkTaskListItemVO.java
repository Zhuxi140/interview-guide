package interview.data.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Spark 离线语料任务列表项。
 */
@Schema(description = "Spark 离线语料任务列表项")
public record SparkTaskListItemVO(
        @Schema(description = "任务 ID")
        Long id,
        @Schema(description = "批次任务唯一编号")
        String taskNo,
        @Schema(description = "任务状态", example = "SUCCESS")
        String status,
        @Schema(description = "原始采集文本总条数")
        Integer rawCount,
        @Schema(description = "清洗去噪后的有效条数")
        Integer cleanedCount,
        @Schema(description = "成功向量化入库的 Chunk 总数")
        Integer chunkCount,
        @Schema(description = "任务启动时间")
        OffsetDateTime startedAt
) {
}
