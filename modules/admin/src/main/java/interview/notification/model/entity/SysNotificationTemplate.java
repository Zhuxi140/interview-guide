package interview.notification.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 通知模板实体；同一 (notify_scene, channel_type) 有效记录唯一。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "sys_notification_templates")
public class SysNotificationTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String notifyScene;

    private String channelType;

    private String title;

    private String contentTemplate;

    @Builder.Default
    private Boolean isEnabled = true;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
