package interview.candidate.model.vo;

import interview.code.model.vo.EnterpriseCodeSubmissionListItemVO;
import interview.resume.model.vo.CandidateProfileVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "企业候选人综合看板")
public record EnterpriseCandidateOverviewVO(
        @Schema(description = "人才池候选人 ID") Long candidateId,
        @Schema(description = "最近一次已完成人才画像") CandidateProfileVO profile,
        @Schema(description = "当前企业内的投递") List<EnterpriseCandidateApplicationVO> applications,
        @Schema(description = "面试轮次") List<EnterpriseCandidateInterviewRoundVO> interviewRounds,
        @Schema(description = "面试报告摘要") List<EnterpriseCandidateReportVO> reports,
        @Schema(description = "代码提交摘要") List<EnterpriseCodeSubmissionListItemVO> codeSubmissions
) {
}
