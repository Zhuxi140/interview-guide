package interview.notification.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 通知发送渠道配置实体；每渠道固定一行，凭证字段在 configJson 内加密存储。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "sys_notification_channels", autoResultMap = true)
public class SysNotificationChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * EMAIL / SMS / IN_APP。
     */
    private String channelType;

    @Builder.Default
    private Boolean isEnabled = false;

    private String provider;

    /**
     * 渠道参数 JSON；凭证字段以密文信封存储。
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String configJson;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
