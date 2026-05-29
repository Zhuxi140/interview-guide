package interview.system.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.UserType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 系统用户主表
 */

@Data
@TableName("sys_users")
@Builder
public class SysUser implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String email;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private String phone;

    private UserType userType;

    private Integer riskLevel;

    private Integer status;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
