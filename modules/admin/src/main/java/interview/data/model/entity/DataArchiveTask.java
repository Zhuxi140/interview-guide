package interview.data.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 数据归档任务实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "data_archive_tasks")
public class DataArchiveTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * API_LOG / OPERATE_LOG / INTERVIEW_TIMELINE / VOICE_MEDIA / INTERVIEW_REPORT。
     */
    private String resourceType;

    private OffsetDateTime beforeTime;

    /**
     * PENDING / PROCESSING / COMPLETED / FAILED / CANCELLED。
     */
    private String status;

    private Long archivedCount;

    private String failureReason;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    private OffsetDateTime completedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
