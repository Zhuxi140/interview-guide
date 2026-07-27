package interview.infra.localMessage.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.MsgPriority;
import interview.common.enums.MsgStatus;
import interview.common.enums.MsgTopic;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "local_message", autoResultMap = true)
public class LocalMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private MsgTopic topic;

    private String bizKey;

    private Integer schemaVersion;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String payload;

    private MsgPriority priority;

    private MsgStatus status;

    private Integer retryCount;

    private Integer maxRetries;

    private OffsetDateTime nextRetryAt;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String retryHistory;

    private String lastError;

    private String leaseOwner;

    private OffsetDateTime leaseUntil;

    private Long leaseVersion;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
