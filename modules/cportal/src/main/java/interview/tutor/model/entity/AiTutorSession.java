package interview.tutor.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * C 端智能考点答疑会话实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "ai_tutor_sessions")
public class AiTutorSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    private Long associatedReportId;

    private String sessionTitle;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
