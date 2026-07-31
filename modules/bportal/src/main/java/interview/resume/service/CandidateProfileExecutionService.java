package interview.resume.service;

import cn.hutool.core.util.StrUtil;
import interview.api.aicore.CandidateProfileAiApi;
import interview.api.aicore.dto.AiCandidateDimensionScore;
import interview.api.aicore.dto.AiCandidateProfileResult;
import interview.common.enums.CandidateDimensionCode;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.spi.MessageHandleResult;
import interview.resume.model.bo.CandidateProfileExecutionBO;
import interview.resume.model.message.CandidateProfileCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * 在短事务之间执行耗时的人才画像 AI 调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateProfileExecutionService {

    private static final int MAX_ERROR_LENGTH = 500;

    private final CandidateProfileStateService stateService;
    private final CandidateProfileAiApi candidateProfileAiApi;
    private final ObjectMapper objectMapper;

    /**
     * 执行一次人才画像生成。
     *
     * @param messageId 消息 ID
     * @param command 画像命令
     * @return 消息处理结果
     */
    public MessageHandleResult execute(
            Long messageId, CandidateProfileCommand command) {
        CandidateProfileExecutionBO execution = stateService.claim(messageId, command);
        if (execution == null) {
            return stateService.resolveUnclaimed(messageId, command);
        }
        try {
            if (StrUtil.isBlank(execution.resumeText())) {
                throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED);
            }
            AiCandidateProfileResult result =
                    candidateProfileAiApi.generateProfile(execution.resumeText());
            validateResult(result);
            String summaryJson = objectMapper.writeValueAsString(
                    Map.of("summary", result.summary()));
            String snapshotJson = objectMapper.writeValueAsString(
                    result.llmConfigSnapshot());
            boolean completed = stateService.complete(
                    execution, summaryJson, snapshotJson, result.dimensions());
            return completed
                    ? MessageHandleResult.success()
                    : MessageHandleResult.ignored(
                            "candidate profile execution fence lost");
        } catch (Exception e) {
            log.error("人才画像生成失败。profileId={}, resumeId={}, attemptCount={}",
                    execution.profileId(), execution.resumeId(),
                    execution.attemptCount(), e);
            return stateService.fail(execution, errorSummary(e));
        }
    }

    private void validateResult(AiCandidateProfileResult result) {
        if (result == null || StrUtil.isBlank(result.summary())
                || result.llmConfigSnapshot() == null
                || result.dimensions() == null
                || result.dimensions().size() != CandidateDimensionCode.values().length) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
        EnumSet<CandidateDimensionCode> actual =
                EnumSet.noneOf(CandidateDimensionCode.class);
        for (AiCandidateDimensionScore dimension : result.dimensions()) {
            if (dimension == null || dimension.dimensionCode() == null
                    || dimension.score() == null || dimension.score() < 0
                    || dimension.score() > 100
                    || StrUtil.isBlank(dimension.justification())
                    || dimension.evidence() == null
                    || !actual.add(dimension.dimensionCode())) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
            }
        }
        if (!actual.equals(EnumSet.allOf(CandidateDimensionCode.class))) {
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
