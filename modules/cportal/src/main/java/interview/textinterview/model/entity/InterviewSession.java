package interview.textinterview.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.InterviewSessionStatus;
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
@TableName(value = "interview_sessions", autoResultMap = true)
public class InterviewSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    private Long scheduleId;

    @Builder.Default
    private Short attemptNo = 1;

    private String sessionType;

    private String idempotencyKey;

    private Integer totalQuestions;

    @Builder.Default
    private Integer currentQuestionIndex = 0;

    @Builder.Default
    private Long lastEventSequence = 0L;

    @Builder.Default
    private InterviewSessionStatus status = InterviewSessionStatus.CREATED;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    private String terminationReason;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
