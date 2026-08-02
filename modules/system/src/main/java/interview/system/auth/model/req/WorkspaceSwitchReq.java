package interview.system.auth.model.req;

import interview.system.auth.model.enums.WorkspaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote 工作区切换请求
 */
@Data
@Schema(description = "工作区切换请求")
public class WorkspaceSwitchReq {

    @NotNull(message = "工作区类型不能为空")
    @Schema(description = "目标工作区类型", example = "ENTERPRISE")
    private WorkspaceType workspaceType;

    @Positive(message = "企业 ID 必须为正数")
    @Schema(description = "目标企业 ID；企业工作区必填，平台工作区必须为空")
    private Long enterpriseId;
}
