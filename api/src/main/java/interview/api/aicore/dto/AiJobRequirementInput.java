package interview.api.aicore.dto;

import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;

import java.util.List;

/**
 * 岗位匹配使用的岗位要求输入。
 */
public record AiJobRequirementInput(
        String title,
        String description,
        ExperienceLevel experienceRequirement,
        EducationLevel educationRequirement,
        List<String> skills
) {
}
