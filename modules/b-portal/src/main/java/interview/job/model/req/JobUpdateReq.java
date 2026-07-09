package interview.job.model.req;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zhuxi
 * @apiNote 编辑岗位请求
 */
@Data
public class JobUpdateReq {

    private String title;

    private String jdContent;

    private String department;

    private String location;

    @DecimalMin(value = "0", message = "最低薪资不能为负数")
    private BigDecimal minSalary;

    @DecimalMin(value = "0", message = "最高薪资不能为负数")
    private BigDecimal maxSalary;

    private String experienceReq;

    private String educationReq;

    private String skillsJson;
}
