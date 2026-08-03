package interview.interviewcfg.service.support;

import interview.common.exception.BusinessException;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.req.InterviewTemplateCreateReq;
import interview.interviewcfg.model.vo.StageVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterviewTemplateRulesTest {

    @Test
    void validatesContinuousStagesAndResolvesRoundFromSnapshot() {
        List<StageVO> stages = InterviewTemplateStageRules.normalize(List.of(
                new InterviewTemplateCreateReq.StageItem("HR", "HR面", 2),
                new InterviewTemplateCreateReq.StageItem("TECHNICAL", "技术面", 1)
        ));
        assertEquals("TECHNICAL", stages.getFirst().phaseCode());

        InterviewTemplateSnapshot snapshot = new InterviewTemplateSnapshot(
                1L, 0, "Java面试", List.of(
                new InterviewTemplateSnapshot.StageSnapshot(
                        "TECHNICAL", "技术面", 1, 3, 0.5, null, 0),
                new InterviewTemplateSnapshot.StageSnapshot(
                        "HR", "HR面", 2, 3, 0.5, null, 0)
        ));
        assertEquals("HR", snapshot.stageForRound((short) 2).phaseCode());
        assertThrows(BusinessException.class, () -> snapshot.stageForRound((short) 3));
    }

    @Test
    void rejectsGapDuplicateAndIllegalCode() {
        assertThrows(BusinessException.class, () -> InterviewTemplateStageRules.normalize(List.of(
                new InterviewTemplateCreateReq.StageItem("TECHNICAL", "技术面", 1),
                new InterviewTemplateCreateReq.StageItem("HR", "HR面", 3)
        )));
        assertThrows(BusinessException.class, () -> InterviewTemplateStageRules.normalize(List.of(
                new InterviewTemplateCreateReq.StageItem("TECHNICAL", "技术面", 1),
                new InterviewTemplateCreateReq.StageItem("TECHNICAL", "复试", 2)
        )));
        assertThrows(BusinessException.class, () -> InterviewTemplateStageRules.normalize(List.of(
                new InterviewTemplateCreateReq.StageItem("technical", "技术面", 1)
        )));
    }
}
