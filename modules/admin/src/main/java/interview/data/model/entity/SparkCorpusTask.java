package interview.data.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Spark 离线语料同步任务实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "spark_corpus_tasks")
public class SparkCorpusTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;

    private String sourcePath;

    private Integer rawCount;

    private Integer cleanedCount;

    private Integer chunkCount;

    /**
     * PENDING / RUNNING / SUCCESS / FAILED / CANCELLED。
     */
    private String status;

    private Integer vectorDbWriteLatencyMs;

    private Integer embeddingApiLatencyMs;

    private OffsetDateTime startedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
