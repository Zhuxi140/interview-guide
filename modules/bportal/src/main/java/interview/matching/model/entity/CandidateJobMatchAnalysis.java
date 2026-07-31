package interview.matching.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * 候选人私有岗位适配预测结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "candidate_job_match_analyses", autoResultMap = true)
public class CandidateJobMatchAnalysis implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long applicationId;

    private Long candidateProfileId;

    private AiTaskStatus status;

    private Integer matchScore;

    private Integer passProbability;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String strengthsJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String gapsJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String jobSnapshot;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String llmConfigSnapshot;

    private Integer attemptCount;

    private OffsetDateTime deadlineAt;

    private String failureReason;

    private String idempotencyKeyHash;

    private OffsetDateTime analyzedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
