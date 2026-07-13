package interview.job.model.req;

import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zhuxi
 * @apiNote 编辑岗位请求
 */
@Data
@Schema(description = "编辑岗位请求")
public class JobUpdateReq {

    @Schema(description = "岗位名称")
    private String title;

    @Schema(description = "职位描述")
    private String jdContent;

    @Schema(description = "所属部门")
    private String department;

    @Schema(description = "工作地点")
    private String location;

    @DecimalMin(value = "0", message = "最低薪资不能为负数")
    @Schema(description = "最低薪资")
    private BigDecimal minSalary;

    @DecimalMin(value = "0", message = "最高薪资不能为负数")
    @Schema(description = "最高薪资")
    private BigDecimal maxSalary;

    @Schema(description = "经验要求")
    private ExperienceLevel experienceReq;

    @Schema(description = "学历要求")
    private EducationLevel educationReq;

    @Schema(description = "技能标签 JSON")
    private String skillsJson;
}
