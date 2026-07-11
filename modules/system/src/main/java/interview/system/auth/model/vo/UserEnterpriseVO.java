package interview.system.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "用户所属企业简要信息")
public record UserEnterpriseVO(
    @Schema(description = "企业 ID") Long id,
    @Schema(description = "企业名称") String name,
    @Schema(description = "企业简称") String shortName,
    @Schema(description = "企业 Logo URL") String logoUrl
) {}
