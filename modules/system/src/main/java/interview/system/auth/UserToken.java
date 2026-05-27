package interview.system.auth;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 用户长时 Refresh Token 实体
 */
@TableName("user_tokens")
@Data
public class UserToken implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String refreshTokenHash;

    private String deviceInfo;

    private String ipAddress;

    private LocalDateTime expiresAt;

    private Boolean isRevoked;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
