package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.common.enums.AiTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 岗位无关人才画像响应。
 */
@Schema(description = "岗位无关人才画像响应")
public record CandidateProfileVO(
        @Schema(description = "画像 ID")
        Long profileId,
        @Schema(description = "简历 ID")
        Long resumeId,
        @Schema(description = "画像任务状态")
        AiTaskStatus status,
        @Schema(description = "画像规范版本")
        String profileSchemaVersion,
        @Schema(description = "画像摘要")
        String summary,
        @Schema(description = "固定维度评分")
        List<CandidateSkillScoreVO> dimensions,
        @Schema(description = "分析完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime analyzedAt
) {
}
