package interview.code.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.cportal.InterviewSessionParticipationApi;
import interview.api.cportal.dto.InterviewSessionParticipantDTO;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.code.mapper.CodeSubmissionMapper;
import interview.code.mapper.CodeSubmissionResultMapper;
import interview.code.mapper.CodeTestCaseMapper;
import interview.code.model.entity.CodeSubmission;
import interview.code.model.entity.CodeSubmissionResult;
import interview.code.model.entity.CodeTestCase;
import interview.code.model.enums.CodeExecutionStatus;
import interview.code.model.req.EnterpriseCodeSubmissionSearchReq;
import interview.code.model.req.SessionCodeSubmissionSearchReq;
import interview.code.model.vo.*;
import interview.code.service.CodeQuestionService;
import interview.code.service.CodeSubmissionQueryService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 代码提交只读查询服务实现。
 *
 * <p>提交为异步评测链路（沙箱执行 + AI 审查），本服务只负责查询；
 * 结果字段在执行器接入前可能为空，接口按现状返回。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSubmissionQueryServiceImpl
        extends ServiceImpl<CodeSubmissionMapper, CodeSubmission>
        implements CodeSubmissionQueryService {

    private final CodeSubmissionResultMapper codeSubmissionResultMapper;
    private final CodeTestCaseMapper codeTestCaseMapper;
    private final CodeQuestionService codeQuestionService;
    private final InterviewSessionParticipationApi sessionParticipationApi;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final UserApi userApi;
    private final ObjectMapper objectMapper;

    // TODO [Phase6] 沙箱执行器与AI审查接入后回填 execution_status、逐用例结果与 ai_review_json；
    //  当前查询接口照常返回已持久化的数据，未执行完成时结果字段为空。

    @Override
    public IPage<CodeSubmissionListItemVO> pageMySubmissions(
            Long sessionId, SessionCodeSubmissionSearchReq req) {
        // 会话归属校验：仅候选人本人可查询当前会话提交。
        requireSessionOwner(sessionId);

        // 单表分页查询本人提交记录，按提交时间倒序。
        Page<CodeSubmission> submissionPage = baseMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                new LambdaQueryWrapper<CodeSubmission>()
                        .select(CodeSubmission::getId, CodeSubmission::getSessionId,
                                CodeSubmission::getQuestionId, CodeSubmission::getLanguage,
                                CodeSubmission::getExecutionStatus, CodeSubmission::getCreatedAt)
                        .eq(CodeSubmission::getSessionId, sessionId)
                        .eq(req.getQuestionId() != null,
                                CodeSubmission::getQuestionId, req.getQuestionId())
                        .orderByDesc(CodeSubmission::getId));
        List<CodeSubmission> records = submissionPage.getRecords();
        Map<Long, long[]> counts = countPassedBySubmissionIds(
                records.stream().map(CodeSubmission::getId).toList());
        Map<Long, Long> attemptNumbers = attemptNumbersBySubmission(records);

        Page<CodeSubmissionListItemVO> voPage = new Page<>(
                submissionPage.getCurrent(), submissionPage.getSize(), submissionPage.getTotal());
        voPage.setRecords(records.stream()
                .map(submission -> toListItem(submission, counts, attemptNumbers))
                .toList());
        return voPage;
    }

    @Override
    public CodeSubmissionDetailVO getMySubmission(Long sessionId, Long submissionId) {
        // 会话归属校验：仅候选人本人可查询提交详情。
        requireSessionOwner(sessionId);

        // 提交必须属于当前会话，防止跨会话探测提交 ID。
        CodeSubmission submission = lambdaQuery()
                .eq(CodeSubmission::getId, submissionId)
                .eq(CodeSubmission::getSessionId, sessionId)
                .one();
        if (submission == null) {
            throw new BusinessException(ErrorCode.CODE_SUBMISSION_NOT_FOUND);
        }
        return toCandidateDetail(submission);
    }

    @Override
    public IPage<EnterpriseCodeSubmissionListItemVO> pageEnterpriseCandidateSubmissions(
            Long enterpriseId, Long candidateId, EnterpriseCodeSubmissionSearchReq req) {
        // 企业成员校验：HR/面试官必须属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // TODO [Phase6] 投递链路（提交→会话→排期→投递）校验未直查：
        //  当前以 code_submissions.enterprise_id 冗余租户键隔离，并按候选人会话集合过滤。
        List<Long> sessionIds = sessionParticipationApi.listSessionIdsByCandidate(candidateId);
        if (sessionIds.isEmpty()) {
            return new Page<>(req.getPage(), req.getSize(), 0);
        }

        // 单表分页查询候选人在本企业的提交记录。
        Page<CodeSubmission> submissionPage = baseMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                new LambdaQueryWrapper<CodeSubmission>()
                        .select(CodeSubmission::getId, CodeSubmission::getSessionId,
                                CodeSubmission::getQuestionId, CodeSubmission::getLanguage,
                                CodeSubmission::getExecutionStatus, CodeSubmission::getCreatedAt)
                        .eq(CodeSubmission::getEnterpriseId, enterpriseId)
                        .in(CodeSubmission::getSessionId, sessionIds)
                        .eq(req.getSessionId() != null,
                                CodeSubmission::getSessionId, req.getSessionId())
                        .eq(req.getQuestionId() != null,
                                CodeSubmission::getQuestionId, req.getQuestionId())
                        .eq(req.getExecutionStatus() != null,
                                CodeSubmission::getExecutionStatus, req.getExecutionStatus())
                        .orderByDesc(CodeSubmission::getId));
        List<CodeSubmission> records = submissionPage.getRecords();
        Map<Long, long[]> counts = countPassedBySubmissionIds(
                records.stream().map(CodeSubmission::getId).toList());
        Map<Long, Long> attemptNumbers = attemptNumbersBySubmission(records);
        Map<Long, Long> candidateIds = candidateIdsBySessionIds(
                records.stream().map(CodeSubmission::getSessionId).toList());
        Map<Long, String> questionTitles = codeQuestionService.getQuestionTitlesByIds(
                records.stream().map(CodeSubmission::getQuestionId).distinct().toList());

        Page<EnterpriseCodeSubmissionListItemVO> voPage = new Page<>(
                submissionPage.getCurrent(), submissionPage.getSize(), submissionPage.getTotal());
        voPage.setRecords(records.stream()
                .map(submission -> EnterpriseCodeSubmissionListItemVO.builder()
                        .submissionId(submission.getId())
                        .sessionId(submission.getSessionId())
                        .candidateId(candidateIds.get(submission.getSessionId()))
                        .questionId(submission.getQuestionId())
                        .questionTitle(questionTitles.get(submission.getQuestionId()))
                        .language(submission.getLanguage())
                        .executionStatus(submission.getExecutionStatus())
                        .passedCount(passedCount(counts, submission.getId()))
                        .totalCount(totalCount(counts, submission.getId()))
                        .attemptNumber(attemptNumbers.get(submission.getId()))
                        .submittedAt(submission.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    public EnterpriseCodeSubmissionDetailVO getEnterpriseSubmissionDetail(
            Long enterpriseId, Long submissionId) {
        // 企业成员校验：HR/面试官必须属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 租户隔离：提交冗余的 enterprise_id 必须与路径企业一致，禁止跨企业探测提交 ID。
        CodeSubmission submission = lambdaQuery()
                .eq(CodeSubmission::getId, submissionId)
                .one();
        if (submission == null || !enterpriseId.equals(submission.getEnterpriseId())) {
            throw new BusinessException(ErrorCode.CODE_SUBMISSION_NOT_FOUND);
        }

        // 跨模块补齐候选人身份与题目标题。
        InterviewSessionParticipantDTO participant =
                sessionParticipationApi.getSessionParticipant(submission.getSessionId());
        Long candidateId = participant == null ? null : participant.candidateUserId();
        String candidateName = candidateId == null
                ? null : userApi.getUserNameById(candidateId);
        Map<Long, String> questionTitles = codeQuestionService.getQuestionTitlesByIds(
                List.of(submission.getQuestionId()));

        // 组装企业侧详情：执行结果 + AI 审查全文 + 提交代码。
        ExecutionBundle bundle = loadExecutionBundle(submission.getId());
        return EnterpriseCodeSubmissionDetailVO.builder()
                .submissionId(submission.getId())
                .sessionId(submission.getSessionId())
                .candidateId(candidateId)
                .candidateName(candidateName)
                .questionId(submission.getQuestionId())
                .questionTitle(questionTitles.get(submission.getQuestionId()))
                .language(submission.getLanguage())
                .executionStatus(submission.getExecutionStatus())
                .failureReason(failureReason(submission.getExecutionStatus()))
                .executionTimeMs(submission.getExecutionTimeMs())
                .memoryUsedMb(submission.getMemoryUsedMb())
                .passedCount(bundle.passedCount())
                .totalCount(bundle.totalCount())
                .publicResults(bundle.publicResults())
                .hiddenSummary(bundle.hiddenSummary())
                .submittedCode(submission.getSubmittedCode())
                .aiReviewStatus(aiReviewStatus(submission.getAiReviewJson()))
                .aiReview(parseAiReview(submission.getAiReviewJson()))
                .submittedAt(submission.getCreatedAt())
                .build();
    }

    private void requireSessionOwner(Long sessionId) {
        // 会话归属：通过跨模块只读 API 反查参与者，比对当前登录用户。
        InterviewSessionParticipantDTO participant =
                sessionParticipationApi.getSessionParticipant(sessionId);
        if (participant == null
                || !participant.candidateUserId().equals(AuthContext.getRequiredUserId())) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND);
        }
    }

    private CodeSubmissionDetailVO toCandidateDetail(CodeSubmission submission) {
        // 组装候选人详情：公开用例逐条结果 + 隐藏用例汇总 + AI 审查。
        ExecutionBundle bundle = loadExecutionBundle(submission.getId());
        return CodeSubmissionDetailVO.builder()
                .submissionId(submission.getId())
                .executionStatus(submission.getExecutionStatus())
                .failureReason(failureReason(submission.getExecutionStatus()))
                .executionTimeMs(submission.getExecutionTimeMs())
                .memoryUsedMb(submission.getMemoryUsedMb())
                .passedCount(bundle.passedCount())
                .totalCount(bundle.totalCount())
                .publicResults(bundle.publicResults())
                .hiddenSummary(bundle.hiddenSummary())
                .aiReviewStatus(aiReviewStatus(submission.getAiReviewJson()))
                .aiReview(parseAiReview(submission.getAiReviewJson()))
                .build();
    }

    private CodeSubmissionListItemVO toListItem(
            CodeSubmission submission,
            Map<Long, long[]> counts,
            Map<Long, Long> attemptNumbers) {
        return CodeSubmissionListItemVO.builder()
                .submissionId(submission.getId())
                .questionId(submission.getQuestionId())
                .language(submission.getLanguage())
                .executionStatus(submission.getExecutionStatus())
                .passedCount(passedCount(counts, submission.getId()))
                .totalCount(totalCount(counts, submission.getId()))
                .attemptNumber(attemptNumbers.get(submission.getId()))
                .submittedAt(submission.getCreatedAt())
                .build();
    }

    private ExecutionBundle loadExecutionBundle(Long submissionId) {
        // 逐用例结果与用例元数据分两次单表查询，在 Service 层组装，避免跨表 JOIN。
        List<CodeSubmissionResult> results = codeSubmissionResultMapper.selectList(
                Wrappers.<CodeSubmissionResult>lambdaQuery()
                        .select(CodeSubmissionResult::getTestCaseId,
                                CodeSubmissionResult::getPassed,
                                CodeSubmissionResult::getActualOutput,
                                CodeSubmissionResult::getExecutionTimeMs,
                                CodeSubmissionResult::getMemoryUsedMb)
                        .eq(CodeSubmissionResult::getSubmissionId, submissionId)
                        .orderByAsc(CodeSubmissionResult::getId));
        if (results.isEmpty()) {
            // 沙箱执行器未接入时没有任何逐用例结果，直接返回空汇总。
            return new ExecutionBundle(0L, 0L, List.of(), new HiddenSummaryVO(0L, 0L));
        }
        Map<Long, CodeTestCase> testCases = codeTestCaseMapper.selectList(
                        Wrappers.<CodeTestCase>lambdaQuery()
                                .select(CodeTestCase::getId, CodeTestCase::getIsSecret)
                                .in(CodeTestCase::getId,
                                        results.stream()
                                                .map(CodeSubmissionResult::getTestCaseId)
                                                .distinct().toList()))
                .stream()
                .collect(Collectors.toMap(CodeTestCase::getId, testCase -> testCase));

        // 公开用例按用例创建顺序编号；隐藏用例只做计数汇总，不暴露输入与预期输出。
        List<Long> orderedPublicCaseIds = testCases.values().stream()
                .filter(testCase -> !Boolean.TRUE.equals(testCase.getIsSecret()))
                .map(CodeTestCase::getId)
                .sorted()
                .toList();
        Map<Long, Integer> caseNoById = new HashMap<>();
        for (int i = 0; i < orderedPublicCaseIds.size(); i++) {
            caseNoById.put(orderedPublicCaseIds.get(i), i + 1);
        }

        List<CodeCaseResultVO> publicResults = new ArrayList<>();
        long passedCount = 0L;
        long hiddenPassed = 0L;
        long hiddenTotal = 0L;
        for (CodeSubmissionResult result : results) {
            if (Boolean.TRUE.equals(result.getPassed())) {
                passedCount++;
            }
            CodeTestCase testCase = testCases.get(result.getTestCaseId());
            boolean secret = testCase != null
                    && Boolean.TRUE.equals(testCase.getIsSecret());
            if (secret) {
                hiddenTotal++;
                if (Boolean.TRUE.equals(result.getPassed())) {
                    hiddenPassed++;
                }
                continue;
            }
            publicResults.add(CodeCaseResultVO.builder()
                    .caseNo(caseNoById.get(result.getTestCaseId()))
                    .passed(result.getPassed())
                    .actualOutput(result.getActualOutput())
                    .executionTimeMs(result.getExecutionTimeMs())
                    .memoryUsedMb(result.getMemoryUsedMb())
                    .build());
        }
        return new ExecutionBundle(
                passedCount,
                (long) results.size(),
                publicResults,
                new HiddenSummaryVO(hiddenPassed, hiddenTotal));
    }

    private Map<Long, long[]> countPassedBySubmissionIds(List<Long> submissionIds) {
        // 单表 IN 查询逐用例结果，按提交分组统计通过数/总数，避免 N+1。
        if (submissionIds == null || submissionIds.isEmpty()) {
            return Map.of();
        }
        List<CodeSubmissionResult> results = codeSubmissionResultMapper.selectList(
                Wrappers.<CodeSubmissionResult>lambdaQuery()
                        .select(CodeSubmissionResult::getSubmissionId,
                                CodeSubmissionResult::getPassed)
                        .in(CodeSubmissionResult::getSubmissionId, submissionIds));
        Map<Long, long[]> counts = new HashMap<>();
        for (CodeSubmissionResult result : results) {
            long[] counter = counts.computeIfAbsent(
                    result.getSubmissionId(), key -> new long[2]);
            counter[1]++;
            if (Boolean.TRUE.equals(result.getPassed())) {
                counter[0]++;
            }
        }
        return counts;
    }

    private Map<Long, Long> attemptNumbersBySubmission(List<CodeSubmission> submissions) {
        // 同一会话同一题目的提交按 ID 升序编号，即为该次提交的 attemptNumber。
        if (submissions.isEmpty()) {
            return Map.of();
        }
        List<Long> sessionIds = submissions.stream()
                .map(CodeSubmission::getSessionId).distinct().toList();
        List<Long> questionIds = submissions.stream()
                .map(CodeSubmission::getQuestionId).distinct().toList();
        List<CodeSubmission> ordered = lambdaQuery()
                .select(CodeSubmission::getId, CodeSubmission::getSessionId,
                        CodeSubmission::getQuestionId)
                .in(CodeSubmission::getSessionId, sessionIds)
                .in(CodeSubmission::getQuestionId, questionIds)
                .orderByAsc(CodeSubmission::getId)
                .list();
        Map<String, List<Long>> idsBySessionQuestion = new HashMap<>();
        for (CodeSubmission submission : ordered) {
            idsBySessionQuestion
                    .computeIfAbsent(submission.getSessionId() + ":" + submission.getQuestionId(),
                            key -> new ArrayList<>())
                    .add(submission.getId());
        }
        Map<Long, Long> attemptNumbers = new HashMap<>();
        idsBySessionQuestion.values()
                .forEach(ids -> {
                    for (int i = 0; i < ids.size(); i++) {
                        attemptNumbers.put(ids.get(i), (long) (i + 1));
                    }
                });
        return attemptNumbers;
    }

    private Map<Long, Long> candidateIdsBySessionIds(List<Long> sessionIds) {
        // 批量补齐会话归属的候选人用户 ID。
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Map.of();
        }
        return sessionParticipationApi.listSessionParticipants(sessionIds.stream()
                                .filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(
                        InterviewSessionParticipantDTO::sessionId,
                        InterviewSessionParticipantDTO::candidateUserId,
                        (left, right) -> left));
    }

    private long passedCount(Map<Long, long[]> counts, Long submissionId) {
        long[] counter = counts.get(submissionId);
        return counter == null ? 0L : counter[0];
    }

    private long totalCount(Map<Long, long[]> counts, Long submissionId) {
        long[] counter = counts.get(submissionId);
        return counter == null ? 0L : counter[1];
    }

    private String failureReason(CodeExecutionStatus status) {
        // 提交表未单独保存失败原因，先按执行状态给出可读描述。
        if (status == CodeExecutionStatus.TIMEOUT) {
            return "代码执行超时";
        }
        if (status == CodeExecutionStatus.ERROR) {
            return "代码执行异常";
        }
        return null;
    }

    private String aiReviewStatus(String aiReviewJson) {
        // AI 审查结果尚未接入写入链路；已有快照时视为完成，否则返回 null。
        return StrUtil.isNotBlank(aiReviewJson) ? "COMPLETED" : null;
    }

    private AiReviewVO parseAiReview(String aiReviewJson) {
        if (StrUtil.isBlank(aiReviewJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(aiReviewJson, AiReviewVO.class);
        } catch (Exception e) {
            log.error("AI 代码审查 JSON 解析失败: {}", aiReviewJson, e);
            throw new BusinessException(ErrorCode.JSON_TO_OBJECT_ERROR);
        }
    }

    /**
     * 一次提交的执行结果汇总载体。
     */
    private record ExecutionBundle(
            Long passedCount,
            Long totalCount,
            List<CodeCaseResultVO> publicResults,
            HiddenSummaryVO hiddenSummary
    ) {
    }
}
