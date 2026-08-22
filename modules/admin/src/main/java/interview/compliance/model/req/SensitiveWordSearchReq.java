package interview.compliance.model.req;

import interview.compliance.model.enums.SensitiveCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 敏感词库分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "敏感词库分页查询请求")
public class SensitiveWordSearchReq extends CompliancePageReq {

    @Schema(description = "类别筛选项；不传返回全部", example = "CHEAT")
    private SensitiveCategory category;
}
