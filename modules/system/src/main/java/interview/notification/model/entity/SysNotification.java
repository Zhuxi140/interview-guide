package interview.notification.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 系统消息与投递记录实体（含 EMAIL_LOG / SMS_LOG 流水）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "sys_notifications")
public class SysNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    /**
     * SYSTEM / INTERVIEW / EMAIL_LOG / SMS_LOG。
     */
    private String notifyType;

    /**
     * INTERVIEW_INVITE / INTERVIEW_CANCEL / OFFER_SENT / OFFER_DECIDED / REPORT_READY / SYSTEM。
     */
    private String notifyScene;

    /**
     * IN_APP / EMAIL / SMS。
     */
    @Builder.Default
    private String channelType = "IN_APP";

    /**
     * PENDING / SENT / FAILED。
     */
    @Builder.Default
    private String sendStatus = "SENT";

    private String failureReason;

    /**
     * 发送幂等键（{scene}:{bizId}）；唯一索引仅约束非空且未删除的行，
     * 投递流水（EMAIL_LOG / SMS_LOG）不带该键，可多行并存。
     */
    private String idempotencyKey;

    private String title;

    private String content;

    @Builder.Default
    private Boolean isRead = false;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
