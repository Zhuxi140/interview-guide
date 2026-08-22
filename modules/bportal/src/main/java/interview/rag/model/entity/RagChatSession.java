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
 * RAG 对话会话实体，对应表 rag_chat_sessions。
 *
 * <p>为保证写入性能，会话表仅配置 trace_id 与 is_deleted，抛弃厚重审计；
 * 会话仅对创建者本人可见。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "rag_chat_sessions", autoResultMap = true)
public class RagChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 归属企业租户 ID（会话必须属于路径中的企业） */
    private Long enterpriseId;

    /** 创建者用户 ID（企业成员），会话仅本人可见 */
    private Long userId;

    /** 会话标题 */
    private String title;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
