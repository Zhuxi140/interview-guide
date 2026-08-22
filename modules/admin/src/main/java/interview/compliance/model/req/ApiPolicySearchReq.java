package interview.compliance.model.req;

import interview.compliance.model.enums.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户 API 策略列表分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户 API 策略列表分页查询请求")
public class ApiPolicySearchReq extends CompliancePageReq {

    @Schema(description = "策略类型筛选项；不传返回全部", example = "BLACKLIST")
    private PolicyType policyType;
}
