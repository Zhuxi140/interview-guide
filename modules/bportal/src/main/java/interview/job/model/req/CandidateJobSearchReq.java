package interview.job.model.req;

import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author zhuxi
 * @apiNote C 端岗位发现查询参数
 */
@Data
@Schema(description = "C 端岗位发现查询参数")
public class CandidateJobSearchReq {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    @Size(max = 64, message = "搜索关键词长度不能超过 64")
    @Schema(description = "岗位、部门、职位描述或企业名称关键词", example = "Java")
    private String keyword;

    @Size(max = 64, message = "行业长度不能超过 64")
    @Schema(description = "企业行业，精确匹配", example = "互联网")
    private String industry;

    @Size(max = 64, message = "城市长度不能超过 64")
    @Schema(description = "工作城市或地点，模糊匹配", example = "上海")
    private String city;

    @Schema(description = "经验要求")
    private ExperienceLevel experienceReq;

    @Schema(description = "学历要求")
    private EducationLevel educationReq;

    @Pattern(regexp = "^createdAt$", message = "仅支持按 createdAt 排序")
    @Schema(description = "排序字段", example = "createdAt")
    private String sort = "createdAt";

    @Pattern(regexp = "^(?i)(asc|desc)$", message = "排序方向仅支持 asc / desc")
    @Schema(description = "排序方向", example = "desc")
    private String order = "desc";
}
