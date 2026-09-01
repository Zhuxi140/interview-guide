package interview.candidate.model.vo;

import interview.resume.model.vo.CandidateProfileVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "企业人才池候选人详情")
public record EnterpriseCandidateDetailVO(
        @Schema(description = "人才池候选人 ID") Long candidateId,
        @Schema(description = "候选人姓名") String candidateName,
        @Schema(description = "脱敏联系方式") CandidateContactMaskedVO contactMasked,
        @Schema(description = "导入简历") List<EnterpriseCandidateResumeVO> resumes,
        @Schema(description = "最近一次已完成岗位无关人才画像") CandidateProfileVO profile,
        @Schema(description = "当前企业内的投递") List<EnterpriseCandidateApplicationVO> applications
) {
}
