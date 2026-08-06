package interview.textinterview.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.textinterview.model.enums.InterviewTakeoverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 面试官人工接管记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_takeovers")
public class InterviewTakeover implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long enterpriseId;

    private Long interviewerUserId;

    /** 接管命令幂等 ID，对应 Idempotency-Key。 */
    private String requestId;

    @Builder.Default
    private InterviewTakeoverStatus status = InterviewTakeoverStatus.ACTIVE;

    private String reason;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;
}