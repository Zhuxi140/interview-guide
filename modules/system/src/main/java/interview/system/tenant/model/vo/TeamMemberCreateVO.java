package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author zhuxi
 * @apiNote 邀请成员响应
 */
@Schema(description = "邀请成员响应")
public record TeamMemberCreateVO(
    @Schema(description = "成员ID")
    Long id
) {}
