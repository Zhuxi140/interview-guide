package interview.resume.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.resume.mapper.ResumesMapper;
import interview.resume.message.ResumeCleanupMessageFactory;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.support.ResumeLockKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 简历上传状态独立事务服务。
 */
@Service
@RequiredArgsConstructor
public class ResumeTxService {

    private final ResumesMapper resumesMapper;
    private final LocalMessageApi localMessageApi;
    private static final long CLEANUP_GRACE_MINUTES = 5;
    private final ResumeCleanupMessageFactory cleanupMessageFactory;

    /**
     * 独立事务标记上传失败并确保清理消息可调度
     * @param resumeId 简历 ID
     * @param cleanupMessage 清理消息，复用旧对象时为空
     * @return 是否完成状态转换
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markUploadFailed(Long resumeId, MessageDTO cleanupMessage) {
        // 重新读取当前状态，避免覆盖已经确认成功的简历。
        Resumes current = resumesMapper.selectIncludingDeletedById(resumeId);
        if (current == null
                || current.getAnalyzeStatus() != AnalyzeStatus.UPLOADING
                || Boolean.TRUE.equals(current.getIsDeleted())) {
            return false;
        }

        // 先锁定并推进简历状态，统一所有事务的 resumes → local_message 加锁顺序。
        Resumes update = new Resumes();
        update.setAnalyzeStatus(AnalyzeStatus.UPLOAD_FAILED);
        update.setUpdatedBy(current.getUserId());
        update.setTraceId(TraceUtil.getTraceId());
        update.setUpdatedAt(OffsetDateTime.now());
        boolean updated = resumesMapper.update(update, Wrappers.lambdaUpdate(Resumes.class)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getAnalyzeStatus, AnalyzeStatus.UPLOADING)
                .eq(Resumes::getIsDeleted, false)) == 1;
        if (!updated) {
            return false;
        }

        // 状态转换成功后再确保补偿消息可执行；失败会回滚整个独立事务。
        Long cleanupMessageId = current.getCleanupMessageId();
        if (cleanupMessage != null) {
            cleanupMessageId = localMessageApi.ensurePending(cleanupMessage);
        }

        // 消息准备完成后再逻辑删除，任一步失败都回滚本事务。
        Resumes deleteUpdate = new Resumes();
        deleteUpdate.setId(resumeId);
        deleteUpdate.setIsDeleted(true);
        deleteUpdate.setCleanupMessageId(cleanupMessageId);
        deleteUpdate.setUpdatedBy(current.getUserId());
        deleteUpdate.setTraceId(TraceUtil.getTraceId());
        deleteUpdate.setUpdatedAt(OffsetDateTime.now());
        if (resumesMapper.updateById(deleteUpdate) != 1) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return true;
    }

    /**
     * 修复一条超过上传截止时间的简历
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param now 当前时间
     * @return 是否完成修复
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean repairOne(Long resumeId, Long userId, OffsetDateTime now) {
        // 与上传预占使用同一用户锁，避免配额集合并发变化。
        resumesMapper.lockResumes(ResumeLockKey.NAMESPACE, ResumeLockKey.ownerSlot(userId));
        Resumes current = resumesMapper.selectIncludingDeletedById(resumeId);
        if (current == null
                || current.getAnalyzeStatus() != AnalyzeStatus.UPLOADING
                || Boolean.TRUE.equals(current.getIsDeleted())
                || current.getUploadDeadlineAt() == null
                || current.getUploadDeadlineAt().isAfter(now)) {
            return false;
        }

        // 先推进简历状态，统一所有事务的 resumes → local_message 加锁顺序。
        Resumes update = new Resumes();
        update.setAnalyzeStatus(AnalyzeStatus.UPLOAD_FAILED);
        update.setUpdatedBy(userId);
        update.setTraceId(TraceUtil.getTraceId());
        update.setUpdatedAt(now);
        boolean updated = resumesMapper.update(update, Wrappers.lambdaUpdate(Resumes.class)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getAnalyzeStatus, AnalyzeStatus.UPLOADING)
                .eq(Resumes::getIsDeleted, false)
                .le(Resumes::getUploadDeadlineAt, now)) == 1;
        if (!updated) {
            return false;
        }

        // 只有新上传对象具有 cleanupMessageId，复用旧对象时不生成删除任务。
        Long cleanupMessageId = current.getCleanupMessageId();
        if (cleanupMessageId != null) {
            cleanupMessageId = localMessageApi.ensurePending(
                    cleanupMessageFactory.create(current, now.plusMinutes(CLEANUP_GRACE_MINUTES))
            );
        }

        // 消息准备完成后再逻辑删除，任一步失败都回滚单条修复事务。
        Resumes deleteUpdate = new Resumes();
        deleteUpdate.setId(resumeId);
        deleteUpdate.setIsDeleted(true);
        deleteUpdate.setCleanupMessageId(cleanupMessageId);
        deleteUpdate.setUpdatedBy(userId);
        deleteUpdate.setTraceId(TraceUtil.getTraceId());
        deleteUpdate.setUpdatedAt(now);
        if (resumesMapper.updateById(deleteUpdate) != 1) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return true;
    }
}
