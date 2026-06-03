package interview.system.rbac.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.UserType;
import interview.framework.security.mybatis.AesTypeHandler;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

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

    // 必填
    private String username;

    private String email;

    // 必填
    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    @TableField(typeHandler = AesTypeHandler.class)
    private String phone;

    // 必填
    private UserType userType;

    // 默认值 0
    private Integer riskLevel;

    // 默认值 1
    private Integer status;

    // 默认值 false
    @TableLogic
    private Boolean isDeleted;

    // 必填
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
