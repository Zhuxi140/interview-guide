package interview.candidate.model.vo;

import interview.candidate.model.enums.CandidateBasicProfileStatus;
import interview.resume.model.vo.CandidateSkillScoreVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "候选人可编辑资料和能力画像摘要")
public record CandidateBasicProfileVO(
        @Schema(description = "用户 ID")
        Long userId,
        @Schema(description = "展示名称")
        String displayName,
        @Schema(description = "联系邮箱")
        String email,
        @Schema(description = "期望工作城市")
        String city,
        @Schema(description = "教育背景摘要")
        String education,
        @Schema(description = "个人简介")
        String summary,
        @Schema(description = "资料完整度状态")
        CandidateBasicProfileStatus profileStatus,
        @Schema(description = "最近一次已完成人才画像维度")
        List<CandidateSkillScoreVO> skillDimensions,
        @Schema(description = "资料版本")
        Integer version,
        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt
) {
}
