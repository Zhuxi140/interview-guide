package interview.job.model.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zhuxi
 * @apiNote 发布岗位请求
 */
@Data
public class JobCreateReq {

    @NotBlank(message = "岗位名称不能为空")
    private String title;

    @NotBlank(message = "职位描述不能为空")
    private String jdContent;

    @NotBlank(message = "所属部门不能为空")
    private String department;

    @NotBlank(message = "工作地点不能为空")
    private String location;

    @DecimalMin(value = "0", message = "最低薪资不能为负数")
    private BigDecimal minSalary;

    @DecimalMin(value = "0", message = "最高薪资不能为负数")
    private BigDecimal maxSalary;

    private String experienceReq;

    private String educationReq;

    private String skillsJson;
}
