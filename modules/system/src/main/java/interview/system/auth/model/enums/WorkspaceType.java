package interview.system.auth.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author zhuxi
 * @apiNote 管理前端工作区类型
 */
@Schema(description = "管理前端工作区类型")
public enum WorkspaceType {
    ENTERPRISE,
    PLATFORM
}
