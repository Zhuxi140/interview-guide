package interview.interviewcfg.service.support;

import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.interviewcfg.model.req.InterviewTemplateCreateReq;
import interview.interviewcfg.model.vo.StageVO;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 面试模板阶段编码和顺序规则。
 */
public final class InterviewTemplateStageRules {

    private static final int MAX_STAGE_COUNT = 10;
    private static final Pattern PHASE_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");

    private InterviewTemplateStageRules() {
    }

    /**
     * 规范化并校验模板阶段。
     * @param items 阶段请求
     * @return 按连续顺序排列的阶段
     */
    public static List<StageVO> normalize(
            List<InterviewTemplateCreateReq.StageItem> items) {
        if (items == null || items.isEmpty() || items.size() > MAX_STAGE_COUNT) {
            throw invalidStage();
        }

        Set<String> phaseCodes = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        List<StageVO> stages = items.stream()
                .map(item -> normalize(item, phaseCodes, sortOrders))
                .sorted(Comparator.comparing(StageVO::sortOrder))
                .toList();
        for (int index = 0; index < stages.size(); index++) {
            if (stages.get(index).sortOrder() != index + 1) {
                throw invalidStage();
            }
        }
        return stages;
    }

    private static StageVO normalize(InterviewTemplateCreateReq.StageItem item,
                                     Set<String> phaseCodes,
                                     Set<Integer> sortOrders) {
        if (item == null || item.phaseCode() == null
                || item.phaseName() == null || item.sortOrder() == null) {
            throw invalidStage();
        }
        String phaseCode = item.phaseCode().trim();
        String phaseName = item.phaseName().trim();
        Integer sortOrder = item.sortOrder();
        if (!PHASE_CODE_PATTERN.matcher(phaseCode).matches()
                || phaseName.isEmpty() || phaseName.length() > 64
                || sortOrder < 1 || sortOrder > MAX_STAGE_COUNT
                || !phaseCodes.add(phaseCode)
                || !sortOrders.add(sortOrder)) {
            throw invalidStage();
        }
        return new StageVO(phaseCode, phaseName, sortOrder);
    }

    private static BusinessException invalidStage() {
        return new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_STAGE_INVALID);
    }
}
