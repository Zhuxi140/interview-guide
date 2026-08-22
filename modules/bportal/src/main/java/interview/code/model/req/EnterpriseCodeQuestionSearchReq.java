package interview.code.model.req;

import interview.code.model.enums.CodeVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 企业编程题分页查询参数（企业私有题 + 可用全局题）。
 */
@Data
@Schema(description = "企业编程题分页查询参数")
public class EnterpriseCodeQuestionSearchReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Schema(description = "可见性筛选：GLOBAL 仅全局题，PRIVATE 仅本企业私有题；为空时两者都返回",
            example = "PRIVATE", allowableValues = {"GLOBAL", "PRIVATE"})
    private CodeVisibility visibility;

    @Size(max = 64, message = "关键词长度不能超过 64")
    @Schema(description = "题目标题关键词", example = "两数之和")
    private String keyword;
}
