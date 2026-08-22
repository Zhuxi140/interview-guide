package interview.kyc.model.req;

import interview.kyc.model.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台端实名认证审核列表分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "实名认证审核列表分页查询请求")
public class KycAuditSearchReq extends KycPageReq {

    @Schema(description = "认证状态筛选项；不传返回全部", example = "PENDING")
    private KycStatus authStatus;
}
