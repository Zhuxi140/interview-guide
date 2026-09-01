package interview.candidate.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.candidate.model.enums.ResumeImportItemStatus;
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
@TableName("resume_import_items")
public class ResumeImportItem implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long batchId;
    private Long enterpriseId;
    private Long enterpriseCandidateId;
    private String originalFilename;
    private String objectKey;
    private String fileHash;
    private ResumeImportItemStatus status;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
