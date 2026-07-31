package interview.matching.service;

import cn.hutool.core.util.StrUtil;
import interview.api.aicore.JobMatchingAiApi;
import interview.api.aicore.dto.AiCandidateJobMatchResult;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.matching.model.bo.CandidateJobMatchExecutionBO;
import interview.matching.model.message.CandidateJobMatchCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 在短事务之间执行耗时的候选人岗位匹配调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateJobMatchExecutionService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final CandidateJobMatchStateService stateService;
    private final MatchingAiInputService inputService;
    private final JobMatchingAiApi jobMatchingAiApi;
    private final ObjectMapper objectMapper;

    /**
     * 执行一次候选人岗位适配预测。
     *
     * @param messageId 消息 ID
     * @param command 预测命令
     * @return 消息处理结果
     */
    public MessageHandleResult execute(
            Long messageId, CandidateJobMatchCommand command) {
        MessageHandleResult dependency = stateService.prepareProfile(messageId, command);
        if (dependency != null) {
            return dependency;
        }
        CandidateJobMatchExecutionBO execution = stateService.claim(messageId);
        if (execution == null) {
            return stateService.resolveUnclaimed(messageId, command);
        }
        try {
            AiCandidateJobMatchResult result = jobMatchingAiApi.matchForCandidate(
                    inputService.loadProfile(execution.candidateProfileId()),
                    inputService.readJobSnapshot(execution.jobSnapshot()));
            validateResult(result);
            boolean completed = stateService.complete(
                    execution, result.matchScore(), result.passProbability(),
                    objectMapper.writeValueAsString(result.strengths()),
                    objectMapper.writeValueAsString(result.gaps()),
                    objectMapper.writeValueAsString(result.llmConfigSnapshot()));
            return completed
                    ? MessageHandleResult.success()
                    : MessageHandleResult.ignored(
                            "candidate job match execution fence lost");
        } catch (Exception e) {
            log.error("候选人岗位预测失败。analysisId={}, applicationId={}, attemptCount={}",
                    execution.analysisId(), execution.applicationId(),
                    execution.attemptCount(), e);
            return stateService.fail(execution, errorSummary(e));
        }
    }

    private void validateResult(AiCandidateJobMatchResult result) {
        if (result == null || result.matchScore() == null
                || result.matchScore() < 0 || result.matchScore() > 100
                || result.passProbability() == null
                || result.passProbability() < 0 || result.passProbability() > 100
                || result.strengths() == null || result.gaps() == null
                || result.llmConfigSnapshot() == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }

    private String errorSummary(Exception exception) {
        String message = exception.getClass().getSimpleName()
                + ": " + StrUtil.nullToEmpty(exception.getMessage());
        return message.length() <= MAX_ERROR_LENGTH
                ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
