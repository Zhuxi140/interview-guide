package interview.system.auth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote Refresh Token 实体
 */
@TableName("user_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh Token 实体")
public class UserToken implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 必填
    private Long userId;

    // 必填
    private String refreshTokenHash;

    private String deviceInfo;

    private String ipAddress;

    // 必填
    private OffsetDateTime expiresAt;

    // 默认值 false
    private Boolean isRevoked;

    // 默认值 false
    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    // 必填
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
