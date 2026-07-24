package interview.resume.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.service.impl.ResumeTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 定期修复超过截止时间的简历上传死记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadRepairJob {

    private static final int BATCH_SIZE = 100;

    private final ResumesMapper resumesMapper;
    private final ResumeTxService repairTxService;

    @Scheduled(fixedDelayString = "${app.resume.upload-repair-delay-ms:60000}")
    public void repairExpiredUploading() {
        // 每轮只读取有限批次，每条候选记录使用独立短事务修复。
        OffsetDateTime now = OffsetDateTime.now();
        List<Resumes> candidates = resumesMapper.selectList(
                Wrappers.lambdaQuery(Resumes.class)
                        .select(Resumes::getId, Resumes::getUserId)
                        .eq(Resumes::getAnalyzeStatus, AnalyzeStatus.UPLOADING)
                        .le(Resumes::getUploadDeadlineAt, now)
                        .orderByAsc(Resumes::getUploadDeadlineAt)
                        .last("LIMIT " + BATCH_SIZE)
        );
        for (Resumes candidate : candidates) {
            try {
                repairTxService.repairOne(candidate.getId(), candidate.getUserId(), now);
            } catch (Exception e) {
                log.error("修复简历上传死记录失败。resumeId={}", candidate.getId(), e);
            }
        }
    }
}
