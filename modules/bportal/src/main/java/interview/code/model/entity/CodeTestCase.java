package interview.code.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 编程题测试用例实体，对应表 code_test_cases。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "code_test_cases", autoResultMap = true)
public class CodeTestCase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 逻辑外键 code_questions.id */
    private Long questionId;

    /** 输入测试用例参数原文 */
    private String inputCase;

    /** 期望的标准判定输出结果 */
    private String expectedOutput;

    /** 是否为隐藏黑盒边界用例 */
    private Boolean isSecret;

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
