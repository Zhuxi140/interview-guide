package interview.matching.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ScreeningRecommendation;
import interview.framework.mybatis.JsonbStringTypeHandler;
import interview.matching.model.enums.JobApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * HR AI 初筛结果、输入快照和人工审核记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "application_ai_screenings", autoResultMap = true)
public class ApplicationAiScreening implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long applicationId;

    private Long candidateProfileId;

    private AiTaskStatus status;

    private Integer overallMatchScore;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String dimensionMatchesJson;

    private ScreeningRecommendation recommendation;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String thresholdSnapshot;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String jobSnapshot;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String llmConfigSnapshot;

    private JobApplicationStatus reviewDecision;

    private Long reviewedBy;

    private OffsetDateTime reviewedAt;

    private Integer attemptCount;

    private OffsetDateTime deadlineAt;

    private String failureReason;

    private String idempotencyKeyHash;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
