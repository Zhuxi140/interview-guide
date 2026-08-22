package interview.cert.model.req;

import interview.cert.model.enums.CertAuditAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台端审核企业资质请求。
 */
@Data
@Schema(description = "平台端审核企业资质请求")
public class EnterpriseCertAuditReq {

    @NotNull(message = "审核动作不能为空")
    @Schema(description = "审核动作：APPROVE 通过 / REJECT 拒绝", example = "APPROVE")
    private CertAuditAction action;

    @Size(max = 256, message = "拒绝原因不能超过 256 个字符")
    @Schema(description = "拒绝原因；action=REJECT 时必填，APPROVE 时必须为空",
            example = "营业执照与统一社会信用代码不一致")
    private String rejectReason;
}
