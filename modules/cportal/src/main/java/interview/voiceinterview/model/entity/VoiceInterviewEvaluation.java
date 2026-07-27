package interview.voiceinterview.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.framework.mybatis.JsonbStringTypeHandler;
import interview.voiceinterview.model.enums.VoiceEvaluationStatus;
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
@TableName(value = "voice_interview_evaluations", autoResultMap = true)
public class VoiceInterviewEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long interviewSessionId;

    private Long enterpriseId;

    @Builder.Default
    private VoiceEvaluationStatus evaluationStatus = VoiceEvaluationStatus.PENDING;

    @Builder.Default
    private Integer attemptNo = 0;

    private String failureReason;

    private Integer overallScore;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String questionEvaluationsJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String strengthsJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String improvementsJson;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableLogic
    @Builder.Default
    private Boolean isDeleted = false;

    private String traceId;
}
