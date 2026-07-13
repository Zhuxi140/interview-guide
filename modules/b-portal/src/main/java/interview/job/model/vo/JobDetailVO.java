package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import interview.job.model.enums.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 岗位详情响应
 */
@Builder
@Schema(description = "岗位详情响应")
public record JobDetailVO(
        @Schema(description = "岗位 ID")
        Long id,
        @Schema(description = "岗位名称")
        String title,
        @Schema(description = "职位描述")
        String jdContent,
        @Schema(description = "所属部门")
        String department,
        @Schema(description = "工作地点")
        String location,
        @Schema(description = "最低薪资")
        BigDecimal minSalary,
        @Schema(description = "最高薪资")
        BigDecimal maxSalary,
        @Schema(description = "经验要求")
        ExperienceLevel experienceReq,
        @Schema(description = "学历要求")
        EducationLevel educationReq,
        @Schema(description = "技能标签 JSON")
        String skillsJson,
        @Schema(description = "岗位状态")
        JobStatus status,
        @Schema(description = "发布人 ID")
        Long userId,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
