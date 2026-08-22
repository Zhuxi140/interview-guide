package interview.system.rbac.model.req;

import interview.common.enums.UserType;
import interview.system.auth.model.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 平台用户分页查询参数
 */
@Data
@Schema(description = "平台用户分页查询参数")
public class AdminUserSearchReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Size(max = 64, message = "关键词长度不能超过 64")
    @Schema(description = "用户名/昵称关键词；手机号加密存储，不支持模糊匹配", example = "zhuxi")
    private String keyword;

    @Schema(description = "用户类型", example = "CANDIDATE",
            allowableValues = {"ENTERPRISE_USER", "CANDIDATE", "PLATFORM_ADMIN", "PLATFORM_OPS"})
    private UserType userType;

    @Schema(description = "用户状态", example = "NORMAL", allowableValues = {"NORMAL", "DISABLED"})
    private UserStatus status;
}
