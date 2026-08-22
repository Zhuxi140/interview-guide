package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.code.model.req.SessionCodeSubmissionSearchReq;
import interview.code.model.vo.CodeSubmissionDetailVO;
import interview.code.model.vo.CodeSubmissionListItemVO;
import interview.code.model.vo.SessionCodeQuestionVO;
import interview.code.service.CodeQuestionService;
import interview.code.service.CodeSubmissionQueryService;
import interview.common.annonate.MaxRiskLevel;
import interview.common.constant.ApiVersion;
import interview.common.constant.Result;
import interview.common.enums.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选人代码面试接口（会话内读取题目与本人提交记录）。
 *
 * <p>会话归属在 Service 层通过跨模块只读 API 校验，仅候选人本人可访问；
 * 提交端点（沙箱异步评测）为复杂流程型接口，暂不提供。</p>
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/interview-sessions")
@Tag(name = "代码面试（候选人）")
@RequiredArgsConstructor
@Validated
public class SessionCodeController {

    private final CodeQuestionService codeQuestionService;
    private final CodeSubmissionQueryService codeSubmissionQueryService;

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "读取当前会话分配的题目（公开用例可见）")
    @GetMapping("/{sessionId}/code-questions/{questionId}")
    public Result<SessionCodeQuestionVO> getSessionQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId) {
        return Result.success(codeQuestionService.getSessionQuestion(sessionId, questionId));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询本人当前会话提交记录（分页）")
    @GetMapping("/{sessionId}/code-submissions")
    public Result<IPage<CodeSubmissionListItemVO>> pageMySubmissions(
            @PathVariable Long sessionId,
            @Valid SessionCodeSubmissionSearchReq req) {
        return Result.success(
                codeSubmissionQueryService.pageMySubmissions(sessionId, req));
    }

    @MaxRiskLevel(RiskLevel.NO_RISK)
    @Operation(summary = "查询指定提交的执行与 AI 审查结果")
    @GetMapping("/{sessionId}/code-submissions/{submissionId}")
    public Result<CodeSubmissionDetailVO> getMySubmission(
            @PathVariable Long sessionId,
            @PathVariable Long submissionId) {
        return Result.success(
                codeSubmissionQueryService.getMySubmission(sessionId, submissionId));
    }
}
