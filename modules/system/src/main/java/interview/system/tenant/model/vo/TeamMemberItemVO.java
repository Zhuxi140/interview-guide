package interview.system.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 团队成员列表项响应
 */
@Builder
@Schema(description = "团队成员列表项响应")
public record TeamMemberItemVO(
    @Schema(description = "成员ID")
    Long id,
    @Schema(description = "用户ID")
    Long userId,
    @Schema(description = "登录账号")
    String username,
    @Schema(description = "用户昵称")
    String nickname,
    @Schema(description = "邮箱")
    String email,
    @Schema(description = "角色编码")
    String roleCode,
    @Schema(description = "加入时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    OffsetDateTime createdAt
) {}
