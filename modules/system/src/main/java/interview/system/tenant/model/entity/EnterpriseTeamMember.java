package interview.system.tenant.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 企业内部团队成员
 */

@TableName("enterprise_team_members")
@Data
public class EnterpriseTeamMember implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    private Long roleId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;
}
