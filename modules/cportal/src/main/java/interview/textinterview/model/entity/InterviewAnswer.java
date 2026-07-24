package interview.textinterview.model.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName(value = "interview_answers", autoResultMap = true)
public class InterviewAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long enterpriseId;

    private Integer questionIndex;

    private String questionText;

    private Long parentMessageId;

    @Builder.Default
    private Integer followUpDepth = 0;

    private String userAnswer;

    private Integer score;

    private String aiFeedback;

    private OffsetDateTime answeredAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
