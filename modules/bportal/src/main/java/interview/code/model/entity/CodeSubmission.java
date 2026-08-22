package interview.code.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.code.model.enums.CodeExecutionStatus;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 代码提交与运行记录实体，对应表 code_submissions。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "code_submissions", autoResultMap = true)
public class CodeSubmission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 逻辑外键 interview_sessions.id */
    private Long sessionId;

    /** 租户隔离键，逻辑外键 enterprises.id */
    private Long enterpriseId;

    /** 逻辑外键 code_questions.id */
    private Long questionId;

    /** 编程语言 */
    private String language;

    /** 用户提交的代码原文 */
    private String submittedCode;

    private CodeExecutionStatus executionStatus;

    /** 实际执行耗时（毫秒） */
    private Integer executionTimeMs;

    /** 实际内存占用（MB） */
    private Integer memoryUsedMb;

    /** AI 静态审查与重构建议，JSONB 字符串 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String aiReviewJson;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
