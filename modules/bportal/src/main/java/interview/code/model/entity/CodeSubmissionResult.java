package interview.code.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 逐测试用例执行结果实体，对应表 code_submission_results。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "code_submission_results", autoResultMap = true)
public class CodeSubmissionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 逻辑外键 code_submissions.id */
    private Long submissionId;

    /** 逻辑外键 code_test_cases.id */
    private Long testCaseId;

    /** 是否通过 */
    private Boolean passed;

    /** 实际输出原文 */
    private String actualOutput;

    /** 该用例执行耗时（毫秒） */
    private Integer executionTimeMs;

    /** 该用例内存占用（MB） */
    private Integer memoryUsedMb;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
