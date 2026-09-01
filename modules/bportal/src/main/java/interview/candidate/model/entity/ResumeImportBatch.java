package interview.candidate.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.candidate.model.enums.ResumeImportBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("resume_import_batches")
public class ResumeImportBatch implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long enterpriseId;
    private Long operatorUserId;
    private ResumeImportBatchStatus status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private OffsetDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
