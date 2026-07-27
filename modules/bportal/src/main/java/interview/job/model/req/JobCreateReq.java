package interview.job.model.req;

import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote 发布岗位请求
 */
@Data
@Schema(description = "发布岗位请求")
public class JobCreateReq {

    @NotBlank(message = "岗位名称不能为空")
    @Schema(description = "岗位名称")
    private String title;

    @NotBlank(message = "职位描述不能为空")
    @Schema(description = "职位描述")
    private String jdContent;

    @NotBlank(message = "所属部门不能为空")
    @Schema(description = "所属部门")
    private String department;

    @NotBlank(message = "工作地点不能为空")
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

    @Size(max = 30, message = "技能标签不能超过 30 个")
    @Schema(description = "技能标签")
    private List<@NotBlank(message = "技能标签不能为空")
            @Size(max = 64, message = "单个技能标签不能超过 64 个字符") String> skills;

    @Schema(description = "岗位状态，仅允许 DRAFT / OPEN；不传默认 DRAFT")
    private JobStatus status;
}
