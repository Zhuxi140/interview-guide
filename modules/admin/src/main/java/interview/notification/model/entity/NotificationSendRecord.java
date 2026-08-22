package interview.notification.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 全站通知发送记录只读实体，复用 sys_notifications 表。
 */
@Data
@TableName(value = "sys_notifications")
public class NotificationSendRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    private String notifyScene;

    private String channelType;

    private String sendStatus;

    private String failureReason;

    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;
}
