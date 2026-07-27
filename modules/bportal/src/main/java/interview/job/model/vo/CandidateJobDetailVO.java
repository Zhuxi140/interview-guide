package interview.job.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 * @apiNote C 端可投递岗位详情
 */
@Schema(description = "C 端可投递岗位详情")
public record CandidateJobDetailVO(
        @Schema(description = "岗位 ID", example = "193745000000000001")
        Long id,
        @Schema(description = "企业 ID", example = "193745000000000002")
        Long enterpriseId,
        @Schema(description = "企业展示名称", example = "示例科技")
        String enterpriseName,
        @Schema(description = "企业所属行业", example = "互联网")
        String industry,
        @Schema(description = "企业规模", example = "100-499人")
        String enterpriseScale,
        @Schema(description = "企业 Logo 地址")
        String enterpriseLogoUrl,
        @Schema(description = "岗位名称", example = "Java 后端工程师")
        String title,
        @Schema(description = "职位描述")
        String jdContent,
        @Schema(description = "所属部门", example = "技术研发部")
        String department,
        @Schema(description = "工作地点", example = "上海")
        String location,
        @Schema(description = "最低薪资")
        BigDecimal minSalary,
        @Schema(description = "最高薪资")
        BigDecimal maxSalary,
        @Schema(description = "经验要求")
        ExperienceLevel experienceReq,
        @Schema(description = "学历要求")
        EducationLevel educationReq,
        @Schema(description = "技能标签")
        List<String> skills,
        @Schema(description = "发布时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
