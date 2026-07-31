package interview.resume.service;

import cn.hutool.core.util.StrUtil;
import interview.api.aicore.ResumeAiApi;
import interview.api.aicore.dto.AiResumeAnalysisResult;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.resume.model.bo.ResumeAnalysisExecutionBO;
import interview.resume.model.message.ResumeAnalysisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 在短事务之间执行耗时的简历 AI 调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAnalysisExecutionService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final ResumeAnalysisStateService stateService;
    private final ResumeAiApi resumeAiApi;
    private final ObjectMapper objectMapper;

    /**
     * 执行一次简历 AI 分析。
     *
     * @param messageId 消息 ID
     * @param command 分析命令
     * @return 消息处理结果
     */
    public MessageHandleResult execute(
            Long messageId, ResumeAnalysisCommand command) {
        ResumeAnalysisExecutionBO execution =
                stateService.claim(messageId, command);
        if (execution == null) {
            return stateService.resolveUnclaimed(messageId, command);
        }

        try {
            if (StrUtil.isBlank(execution.resumeText())) {
                throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED);
            }
            AiResumeAnalysisResult result =
                    resumeAiApi.analysisResume(execution.resumeText());
            validateResult(result);

            String strengthsJson =
                    objectMapper.writeValueAsString(result.strengthsJson());
            String suggestionsJson =
                    objectMapper.writeValueAsString(result.suggestionsJson());
            String snapshotJson =
                    objectMapper.writeValueAsString(result.llmConfigSnapshot());
            boolean completed = stateService.complete(
                    execution, result.overallScore(), strengthsJson,
                    suggestionsJson, snapshotJson);
            return completed
                    ? MessageHandleResult.success()
                    : MessageHandleResult.ignored(
                            "resume analysis execution fence lost");
        } catch (Exception e) {
            log.error("简历 AI 分析执行失败。messageId={}, resumeId={}, attemptCount={}",
                    execution.messageId(), execution.resumeId(),
                    execution.attemptCount(), e);
            return stateService.fail(execution, errorSummary(e));
        }
    }

    private void validateResult(AiResumeAnalysisResult result) {
        if (result == null || result.overallScore() == null
                || result.overallScore() < 0 || result.overallScore() > 100
                || result.strengthsJson() == null
                || result.suggestionsJson() == null
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
