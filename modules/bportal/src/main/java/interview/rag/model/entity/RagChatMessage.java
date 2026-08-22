package interview.rag.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.rag.model.enums.RagMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * RAG 对话消息实体，对应表 rag_chat_messages。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "rag_chat_messages", autoResultMap = true)
public class RagChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 雪花主键，同时作为游标分页游标 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 逻辑关联 rag_chat_sessions.id */
    private Long sessionId;

    /** 消息类型 (USER / AI) */
    private RagMessageType type;

    /** 消息原文 */
    private String content;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    /** 发送时间 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
