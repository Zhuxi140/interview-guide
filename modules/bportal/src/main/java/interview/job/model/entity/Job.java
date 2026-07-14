package interview.job.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import interview.job.model.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 */
@TableName(value = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long userId;

    private String title;

    private String jdContent;

    private String department;

    private String location;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;

    private ExperienceLevel experienceReq;

    private EducationLevel educationReq;

    private String skillsJson;

    private JobStatus status;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
