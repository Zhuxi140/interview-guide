package interview.matching.service;

import cn.hutool.core.util.StrUtil;
import interview.api.aicore.JobMatchingAiApi;
import interview.api.aicore.dto.AiDimensionMatch;
import interview.api.aicore.dto.AiHrScreeningResult;
import interview.common.enums.CandidateDimensionCode;
import interview.common.enums.ErrorCode;
import interview.common.enums.ScreeningRecommendation;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.matching.model.bo.ApplicationScreeningExecutionBO;
import interview.matching.model.bo.ScreeningThresholdSnapshot;
import interview.matching.model.message.ApplicationScreeningCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 在短事务之间执行耗时的 HR AI 初筛调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationScreeningExecutionService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final ApplicationScreeningStateService stateService;
    private final MatchingAiInputService inputService;
    private final JobMatchingAiApi jobMatchingAiApi;
    private final ObjectMapper objectMapper;

    /**
     * 执行一次 HR AI 初筛。
     *
     * @param messageId 消息 ID
     * @param command 初筛命令
     * @return 消息处理结果
     */
    public MessageHandleResult execute(
            Long messageId, ApplicationScreeningCommand command) {
        MessageHandleResult dependency = stateService.prepareProfile(messageId, command);
        if (dependency != null) {
            return dependency;
        }
        ApplicationScreeningExecutionBO execution = stateService.claim(messageId);
        if (execution == null) {
            return stateService.resolveUnclaimed(messageId, command);
        }
        try {
            AiHrScreeningResult result = jobMatchingAiApi.screenForHr(
                    inputService.loadProfile(execution.candidateProfileId()),
                    inputService.readJobSnapshot(execution.jobSnapshot()));
            validateResult(result);
            ScreeningThresholdSnapshot thresholds = objectMapper.readValue(
                    execution.thresholdSnapshot(), ScreeningThresholdSnapshot.class);
            ScreeningRecommendation recommendation = recommend(result, thresholds);
            boolean completed = stateService.complete(
                    execution, result.overallMatchScore(),
                    objectMapper.writeValueAsString(result.dimensionMatches()),
                    recommendation,
                    objectMapper.writeValueAsString(result.llmConfigSnapshot()));
            return completed
                    ? MessageHandleResult.success()
                    : MessageHandleResult.ignored(
                            "application screening execution fence lost");
        } catch (Exception e) {
            log.error("HR AI 初筛失败。screeningId={}, applicationId={}, attemptCount={}",
                    execution.screeningId(), execution.applicationId(),
                    execution.attemptCount(), e);
            return stateService.fail(execution, errorSummary(e));
        }
    }

    private ScreeningRecommendation recommend(
            AiHrScreeningResult result, ScreeningThresholdSnapshot thresholds) {
        if (thresholds == null || thresholds.overallThreshold() == null
                || result.overallMatchScore() < thresholds.overallThreshold()) {
            return ScreeningRecommendation.RECOMMEND_REJECT;
        }
        Map<CandidateDimensionCode, Integer> actual =
                new EnumMap<>(CandidateDimensionCode.class);
        result.dimensionMatches().forEach(
                item -> actual.put(item.dimensionCode(), item.score()));
        boolean passed = thresholds.dimensionThresholds() == null
                || thresholds.dimensionThresholds().entrySet().stream()
                .allMatch(entry -> actual.getOrDefault(entry.getKey(), -1)
                        >= entry.getValue());
        return passed
                ? ScreeningRecommendation.RECOMMEND_PASS
                : ScreeningRecommendation.RECOMMEND_REJECT;
    }

    private void validateResult(AiHrScreeningResult result) {
        if (result == null || result.overallMatchScore() == null
                || result.overallMatchScore() < 0
                || result.overallMatchScore() > 100
                || result.dimensionMatches() == null
                || result.llmConfigSnapshot() == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        Set<CandidateDimensionCode> dimensions = new HashSet<>();
        for (AiDimensionMatch match : result.dimensionMatches()) {
            if (match == null || match.dimensionCode() == null
                    || match.score() == null || match.score() < 0
                    || match.score() > 100 || StrUtil.isBlank(match.explanation())
                    || !dimensions.add(match.dimensionCode())) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
            }
        }
    }

    private String errorSummary(Exception exception) {
        String message = exception.getClass().getSimpleName()
                + ": " + StrUtil.nullToEmpty(exception.getMessage());
        return message.length() <= MAX_ERROR_LENGTH
                ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
