package interview.system.tenant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author zhuxi
 * @apiNote 更新企业信息响应
 */
@Schema(description = "更新企业信息响应")
public record EnterpriseUpdateVO(
    @Schema(description = "企业ID")
    Long id,
    @Schema(description = "企业名称")
    String name,
    @Schema(description = "企业简称")
    String shortName,
    @Schema(description = "所属行业")
    String industry
) {}
