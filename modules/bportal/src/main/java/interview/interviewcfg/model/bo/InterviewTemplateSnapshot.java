package interview.interviewcfg.model.bo;

import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;

import java.util.List;

/**
 * 面试模板及各阶段组卷配置的业务快照。
 */
public record InterviewTemplateSnapshot(
        Long templateId,
        Integer templateVersion,
        String templateName,
        List<StageSnapshot> stages
) {

    /**
     * 按轮次读取快照阶段。
     * @param roundNo 面试轮次
     * @return 对应阶段
     */
    public StageSnapshot stageForRound(Short roundNo) {
        return stages.stream()
                .filter(stage -> stage.sortOrder().shortValue() == roundNo)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERVIEW_ROUND_NOT_DEFINED));
    }

    /**
     * 模板阶段及组卷参数快照。
     */
    public record StageSnapshot(
            String phaseCode,
            String phaseName,
            Integer sortOrder,
            Integer questionCount,
            Double difficultyWeight,
            String promptOverride,
            Integer phaseConfigVersion
    ) {
    }
}
