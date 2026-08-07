package interview.interviewcfg.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.interviewcfg.mapper.InterviewPlanDraftMapper;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import interview.interviewcfg.model.enums.InterviewPlanDraftStatus;
import interview.interviewcfg.service.InterviewPlanDraftTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 定期将超过有效期的活动草案推进 EXPIRED，释放同投递的活动草案位。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewPlanDraftExpiryJob {

    private static final int BATCH_SIZE = 100;

    private final InterviewPlanDraftMapper interviewPlanDraftMapper;
    private final InterviewPlanDraftTxService draftTxService;

    @Scheduled(fixedDelayString = "${app.interview.draft-expiry-delay-ms:60000}")
    public void expireOverdueDrafts() {
        // 每轮只读取有限批次，每条候选记录使用独立短事务推进。
        OffsetDateTime now = OffsetDateTime.now();
        List<InterviewPlanDraft> candidates = interviewPlanDraftMapper.selectList(
                Wrappers.lambdaQuery(InterviewPlanDraft.class)
                        .select(InterviewPlanDraft::getId)
                        .in(InterviewPlanDraft::getStatus,
                                InterviewPlanDraftStatus.PENDING,
                                InterviewPlanDraftStatus.PROCESSING,
                                InterviewPlanDraftStatus.READY)
                        .le(InterviewPlanDraft::getExpiresAt, now)
                        .orderByAsc(InterviewPlanDraft::getExpiresAt)
                        .last("LIMIT " + BATCH_SIZE)
        );
        for (InterviewPlanDraft candidate : candidates) {
            try {
                draftTxService.expireOne(candidate.getId(), now);
            } catch (Exception e) {
                log.error("推进草案过期失败。draftId={}", candidate.getId(), e);
            }
        }
    }
}