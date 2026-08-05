package interview.interviewcfg.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import interview.framework.mybatis.JsonbStringTypeHandler;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Agent 生成的面试编排业务草案。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "interview_plan_drafts", autoResultMap = true)
public class InterviewPlanDraft implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long applicationId;

    private Long templateId;

    private Long requestedBy;

    private String idempotencyKey;

    /** HR 应用草案时使用的幂等键；仅 APPLIED 后有值。 */
    private String applyIdempotencyKey;

    /** HR 应用草案时提交的修订后计划快照（幂等对比与页面刷新恢复用）。 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String appliedPlanJson;

    /** 应用时创建的排期 ID 数组快照（幂等重放时返回原结果）。 */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String appliedScheduleIds;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String requestJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String inputSnapshotJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String planJson;

    private InterviewPlanDraftStatus status;

    private String failureReason;

    private Long generationMessageId;

    @Version
    private Integer version;

    private OffsetDateTime expiresAt;

    private OffsetDateTime appliedAt;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
