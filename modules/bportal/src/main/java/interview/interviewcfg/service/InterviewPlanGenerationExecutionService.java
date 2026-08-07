package interview.interviewcfg.service;

import cn.hutool.core.util.StrUtil;
import interview.api.aicore.InterviewPlanTradeAiApi;
import interview.api.aicore.dto.AiInterviewPlanInput;
import interview.api.aicore.dto.AiInterviewPlanResult;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.interviewcfg.model.bo.InterviewPlanGenerationExecutionBO;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 在短事务之间执行耗时的 Agent 编排调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewPlanGenerationExecutionService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final InterviewPlanGenerationStateService stateService;
    private final InterviewPlanTradeAiApi interviewPlanTradeAiApi;
    private final ObjectMapper objectMapper;

    /**
     * 执行一次 Agent 编排草案生成。
     *
     * @param messageId 生成消息 ID
     * @param draftId 草案 ID
     * @return 消息处理结果
     */
    public MessageHandleResult execute(Long messageId, Long draftId) {
        InterviewPlanGenerationExecutionBO execution =
                stateService.claim(messageId, draftId);
        if (execution == null) {
            return stateService.resolveUnclaimed(messageId, draftId);
        }

        try {
            if (StrUtil.isBlank(execution.inputSnapshotJson())) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
            }
            AiInterviewPlanInput input = new AiInterviewPlanInput(
                    execution.enterpriseId(), execution.applicationId(),
                    execution.requestJson(), execution.inputSnapshotJson());
            AiInterviewPlanResult result =
                    interviewPlanTradeAiApi.generateInterviewPlan(input);
            validateResult(execution, result);

            String planJson = objectMapper.writeValueAsString(result);
            String snapshotJson = objectMapper.writeValueAsString(result.llmConfigSnapshot());
            boolean completed = stateService.complete(execution, planJson, snapshotJson);
            return completed
                    ? MessageHandleResult.success()
                    : MessageHandleResult.ignored(
                            "interview plan generation execution fence lost");
        } catch (Exception e) {
            log.error("面试编排草案生成执行失败。draftId={}, messageId={}, attemptCount={}",
                    execution.draftId(), execution.messageId(),
                    execution.attemptCount(), e);
            return stateService.fail(execution, errorSummary(e));
        }
    }

    /**
     * 校验 AI 结果的阶段集合与顺序必须与模板快照一致，且字段符合列表契约。
     *
     * @param execution 执行上下文
     * @param result AI 编排结果
     */
    private void validateResult(
            InterviewPlanGenerationExecutionBO execution, AiInterviewPlanResult result) {
        if (result == null || result.stages() == null || result.stages().isEmpty()
                || result.llmConfigSnapshot() == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        List<String> expected = parseSnapshot(execution).stages().stream()
                .map(InterviewTemplateSnapshot.StageSnapshot::phaseCode)
                .toList();
        List<String> actual = result.stages().stream()
                .map(AiInterviewPlanResult.Stage::phaseCode)
                .toList();
        if (!expected.equals(actual)) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        for (AiInterviewPlanResult.Stage stage : result.stages()) {
            if (StrUtil.isBlank(stage.objectives())
                    || StrUtil.isBlank(stage.questionOutline())
                    || stage.durationMinutes() == null
                    || stage.durationMinutes() < 15
                    || stage.durationMinutes() > 480) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
            }
        }
        if (result.scheduleSuggestions() != null) {
            for (AiInterviewPlanResult.ScheduleSuggestion suggestion : result.scheduleSuggestions()) {
                if (suggestion.suggestionId() == null
                        || suggestion.interviewerUserId() == null
                        || suggestion.interviewTime() == null) {
                    throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
                }
            }
        }
    }

    private InterviewTemplateSnapshot parseSnapshot(
            InterviewPlanGenerationExecutionBO execution) {
        try {
            return objectMapper.readValue(
                    execution.inputSnapshotJson(), InterviewTemplateSnapshot.class);
        } catch (Exception exception) {
            log.error("面试编排草案输入快照无法解析。draftId={}",
                    execution.draftId(), exception);
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