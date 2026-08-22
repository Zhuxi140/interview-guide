package interview.rag.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * RAG 会话-知识库关联实体，对应表 rag_session_knowledge_bases。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "rag_session_knowledge_bases", autoResultMap = true)
public class RagSessionKnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 逻辑关联 rag_chat_sessions.id */
    private Long sessionId;

    /** 逻辑关联 knowledge_bases.id */
    private Long knowledgeBaseId;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;
}
