package interview.system.auth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote Refresh Token 实体
 */
@TableName("user_tokens")
@Data
@Schema(description = "Refresh Token 实体")
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
