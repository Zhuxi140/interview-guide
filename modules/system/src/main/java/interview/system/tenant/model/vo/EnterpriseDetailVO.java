package interview.system.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import interview.system.tenant.model.enums.EnterpriseStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 企业详情响应
 */
@Builder
@Schema(description = "企业详情响应")
public record EnterpriseDetailVO(
    @Schema(description = "企业ID")
    Long id,
    @Schema(description = "企业名称")
    String name,
    @Schema(description = "企业简称")
    String shortName,
    @Schema(description = "所属行业")
    String industry,
    @Schema(description = "企业规模")
    String scale,
    @Schema(description = "联系邮箱")
    String contactEmail,
    @Schema(description = "联系电话")
    String contactPhone,
    @Schema(description = "企业状态")
    EnterpriseStatus status,
    @Schema(description = "企业Logo URL")
    String logoUrl,
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    OffsetDateTime createdAt
) {}
