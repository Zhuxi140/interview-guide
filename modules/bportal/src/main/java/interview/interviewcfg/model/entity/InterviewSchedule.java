package interview.interviewcfg.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "interview_schedule", autoResultMap = true)
public class InterviewSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long companyUserId;

    private Long candidateUserId;

    private Long jobId;

    private Long templateId;

    private OffsetDateTime interviewTime;

    @Builder.Default
    private String interviewType = "TEXT";

    @Builder.Default
    private String status = "PENDING_CONFIRMATION";

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String offerDetail;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
