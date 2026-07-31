package interview.matching.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.matching.model.enums.JobApplicationStatus;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * <p>
 * 简历投递与初筛记录表
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("job_applications")
public class JobApplications implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 强隔离：关联企业租户 ID
     */
    private Long enterpriseId;

    /**
     * [逻辑外键]→jobs
     */
    private Long jobId;

    /**
     * [逻辑外键]→sys_users, 仅 user_type='CANDIDATE'
     */
    private Long candidateId;

    /**
     * [逻辑外键]→resumes
     */
    private Long resumeId;

    /**
     * 客户端请求幂等键
     */
    private String idempotencyKey;

    /**
     * APPLIED / REVIEWING / PASSED / REJECTED / WITHDRAWN
     */
    private JobApplicationStatus status;

    /**
     * 投递时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 轻量审计：操作的 HR ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 调用链 ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    /**
     * 状态更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
