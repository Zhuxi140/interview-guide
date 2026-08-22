package interview.tutor.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * AI 答疑交互消息实体；用户消息与 AI 回答分开保存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "ai_tutor_messages")
public class AiTutorMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    /**
     * USER / AI。
     */
    private String messageType;

    private String content;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
