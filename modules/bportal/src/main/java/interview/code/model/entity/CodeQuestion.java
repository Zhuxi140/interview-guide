package interview.code.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.code.model.enums.CodeVisibility;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 编程题库实体，对应表 code_questions。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "code_questions", autoResultMap = true)
public class CodeQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** GLOBAL 为平台公共题；PRIVATE 为企业私有题 */
    private CodeVisibility visibility;

    /** 归属企业租户 ID，GLOBAL 题为 null */
    private Long enterpriseId;

    private String title;

    /** 题目详细描述（Markdown） */
    private String description;

    /** 沙箱运行时间限制（毫秒） */
    private Integer timeLimitMs;

    /** 沙箱运行内存限制（MB） */
    private Integer memoryLimitMb;

    /** 允许的编程语言列表，JSONB 数组字符串 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String supportedLanguagesJson;

    @Version
    private Integer version;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
