package interview.interviewcfg.service;

import interview.common.util.TraceUtil;
import interview.interviewcfg.mapper.InterviewPlanDraftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 面试编排草案独立短事务服务。
 */
@Service
@RequiredArgsConstructor
public class InterviewPlanDraftTxService {

    private final InterviewPlanDraftMapper interviewPlanDraftMapper;

    /**
     * 独立事务将一条超过有效期的活动草案推进 EXPIRED。
     *
     * @param draftId 草案 ID
     * @param now 当前时间
     * @return 是否完成状态推进
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireOne(Long draftId, OffsetDateTime now) {
        return interviewPlanDraftMapper.expireDraft(
                draftId, TraceUtil.getTraceId(), now) == 1;
    }
}