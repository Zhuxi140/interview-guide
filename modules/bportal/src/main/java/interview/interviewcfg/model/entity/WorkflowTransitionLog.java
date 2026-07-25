package interview.interviewcfg.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.matching.model.enums.JobApplicationStatus;
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
@TableName(value = "application_transition_logs", autoResultMap = true)
public class WorkflowTransitionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long applicationId;

    private JobApplicationStatus fromStatus;

    private JobApplicationStatus toStatus;

    private Long operatorUserId;

    private String transitionReason;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
