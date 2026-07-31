package interview.resume.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.common.enums.AiTaskStatus;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 一份简历的一版岗位无关人才画像。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "candidate_ai_profiles", autoResultMap = true)
public class CandidateProfile implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long candidateId;

    private Long resumeId;

    private Long sourceApplicationId;

    private Long sourceEnterpriseId;

    private AiTaskStatus status;

    private String profileSchemaVersion;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String summaryJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String llmConfigSnapshot;

    private Integer attemptCount;

    private OffsetDateTime deadlineAt;

    private String failureReason;

    private OffsetDateTime analyzedAt;

    @TableLogic
    private Boolean isDeleted;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
