package interview.resume.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.infra.FileHashApi;
import interview.api.infra.FileParseApi;
import interview.api.infra.FileStorageApi;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.*;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import interview.resume.model.entity.ResumeAnalyses;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalyzeTriggerVO;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeUploadVO;
import interview.resume.model.vo.ResumeVO;
import interview.resume.model.vo.ResumeDownloadVO;
import interview.resume.service.*;
import interview.resume.message.ResumeCleanupMessageFactory;
import interview.resume.support.ResumeLockKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;

/**
 * @author zhuxi
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumesServiceImpl extends ServiceImpl<ResumesMapper, Resumes> implements ResumesService {

    private final ResumeAnalysesService resumeAnalysesService;
    private final CandidateSkillScoresService candidateSkillScoresService;
    private final CandidateProfileService candidateProfileService;
    private final ResumeAnalysisTaskHandler resumeAnalysisTaskHandler;
    private final FileStorageApi fileStorageService;
    private final FileHashApi fileHashService;
    private final FileParseApi fileParseService;
    private final LocalMessageApi localMessageApi;
    private final TransactionTemplate transactionTemplate;
    private final ResumesMapper resumeMapper;
    private final ResumeTxService resumeTxService;
    private final ResumeCleanupMessageFactory cleanupMessageFactory;

    private static final int MAX_RESUME_COUNT = 5;
    private static final long UPLOAD_DEADLINE_MINUTES = 15;
    private static final long CLEANUP_GRACE_MINUTES = 5;


    @Override
    public ResumeUploadVO uploadResume(MultipartFile file, ResumeUploadReq metadata) {
        // 获取当前用户并完成文件安全校验。
        Long userId = AuthContext.getRequiredUserId();
        long size = file.getSize();
        String originalFilename = file.getOriginalFilename();
        String type;
        try {
            type = fileStorageService.verifyFileType(size, originalFilename, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        // 计算文件哈希，事务外查询仅用于快速失败。
        String hash = fileHashService.calculateHash(file);
        boolean exists = lambdaQuery()
                .eq(Resumes::getFileHash, hash)
                .eq(Resumes::getUserId, userId)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.FILE_IS_EXISTS);
        }
        // 仅复用仍存在的逻辑删除对象，复用对象不创建上传补偿消息。
        String oldUrl = resumeMapper.selectReusableDeletedByHash(
                hash, userId, AnalyzeStatus.UPLOAD_FAILED
        );
        String reusedUrl = StrUtil.isNotBlank(oldUrl) && fileStorageService.fileExists(oldUrl) ? oldUrl : null;

        String newUrl = reusedUrl == null
                ? fileStorageService.generateFileKey(originalFilename, FileSort.RESUME) : null;
        String storageUrl = reusedUrl != null ? reusedUrl : newUrl;
        String name = StrUtil.isNotBlank(metadata.getFileName()) ?
                metadata.getFileName() : originalFilename;

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime uploadDeadlineAt = now.plusMinutes(UPLOAD_DEADLINE_MINUTES);
        Resumes resumes = Resumes.builder()
                .fileHash(hash)
                .userId(userId)
                .fileName(name)
                .fileSize(file.getSize())
                .fileType(type)
                .storageUrl(storageUrl)
                .analyzeStatus(AnalyzeStatus.UPLOADING)
                .uploadDeadlineAt(uploadDeadlineAt)
                .createdAt(now)
                .build();

        // 短事务内串行检查配额，并同时保存简历预占和 Outbox 消息。
        transactionTemplate.executeWithoutResult(status -> {
            try {
                lockCheckAndPreAllocation(userId, resumes, hash, reusedUrl == null);
            } catch (BusinessException e) {
                status.setRollbackOnly();
                throw e;
            } catch (Exception e) {
                log.error("上传简历 - 预占或提前写入消息表兜底补偿失败，直接拦截。 error：{}", e.getMessage());
                status.setRollbackOnly();
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        });

        Long resumesId = resumes.getId();
        try {
            // 网络上传和文本解析位于预占事务之外。
            if (reusedUrl == null) {
                fileStorageService.uploadFile(file, FileSort.RESUME, newUrl);
            }

            String resumeText = fileParseService.parseText(file);
            if (StrUtil.isBlank(resumeText)) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            saveResume(resumes, resumeText);
        } catch (Exception e) {
            safeMarkUploadFailed(resumes);
            log.error("上传简历 - 上传、解析或保存失败。resumeId：{}", resumesId, e);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return ResumeUploadVO.builder()
                .id(resumesId)
                .fileName(resumes.getFileName())
                .detectedMediaType(resumes.getFileType())
                .fileSize(resumes.getFileSize())
                .createdAt(resumes.getCreatedAt())
                .analyzeStatus(AnalyzeStatus.PENDING)
                .build();
    }

    private void saveResume(Resumes resumes, String resumeText) {
        transactionTemplate.execute(status -> {
            try {
                // 条件推进状态，随后在同一事务中取消补偿消息。
                Resumes update = new Resumes();
                update.setResumeText(resumeText);
                update.setAnalyzeStatus(AnalyzeStatus.PENDING);
                update.setUpdatedBy(resumes.getUserId());
                update.setTraceId(TraceUtil.getTraceId());
                update.setUpdatedAt(OffsetDateTime.now());
                boolean updated = update(update, Wrappers.lambdaUpdate(Resumes.class)
                        .eq(Resumes::getId, resumes.getId())
                        .eq(Resumes::getAnalyzeStatus, AnalyzeStatus.UPLOADING)
                        .eq(Resumes::getIsDeleted, false));
                if (!updated) {
                    throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
                }
                if (resumes.getCleanupMessageId() != null
                        && !localMessageApi.ignorePending(resumes.getCleanupMessageId())) {
                    throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
                }
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("上传简历 - 确认事务出现异常。msg:{}", e.getMessage());
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        });
    }

    private void safeMarkUploadFailed(Resumes resumes) {
        if (resumes.getId() == null) {
            return;
        }
        try {
            // 新对象失败时提前清理时间；复用旧对象时不创建删除任务。
            MessageDTO cleanupMessage = resumes.getCleanupMessageId() == null ? null
                    : cleanupMessageFactory.create(
                            resumes, OffsetDateTime.now().plusMinutes(CLEANUP_GRACE_MINUTES)
                    );
            resumeTxService.markUploadFailed(resumes.getId(), cleanupMessage);
        } catch (Exception e) {
            log.error("标记简历上传失败异常，等待定时任务处理。resumeId：{}", resumes.getId(), e);
        }
    }

    private void lockCheckAndPreAllocation(Long userId, Resumes resumes, String hash, boolean needsCleanup) {
        // 双键 Advisory Lock 显式划分简历业务命名空间。
        resumeMapper.lockResumes(ResumeLockKey.NAMESPACE, ResumeLockKey.ownerSlot(userId));

        boolean exists = lambdaQuery()
                .eq(Resumes::getFileHash, hash)
                .eq(Resumes::getUserId, userId)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.FILE_IS_EXISTS);
        }

        Long count = lambdaQuery()
                .eq(Resumes::getUserId, userId)
                .count();
        if (count >= MAX_RESUME_COUNT) {
            throw new BusinessException(ErrorCode.RESUME_MOST_IS_FIVE);
        }

        // 提前分配简历 ID，保证消息业务键和简历记录在同一事务内建立关联。
        resumes.setId(IdWorker.getId());
        if (needsCleanup) {
            MessageDTO cleanupMessage = cleanupMessageFactory.create(
                    resumes, resumes.getUploadDeadlineAt().plusMinutes(CLEANUP_GRACE_MINUTES)
            );
            resumes.setCleanupMessageId(localMessageApi.saveInCurrentTransaction(cleanupMessage));
        }
        save(resumes);
    }

    @Override
    public IPage<ResumeListItemVO> pageResumes(
            Integer page, Integer size, String fileName,
            AnalyzeStatus analyzeStatus, String order) {
        // ① 获取当前用户
        Long userId = AuthContext.getRequiredUserId();
        // ② 构建查询条件（文件名模糊 + 解析状态精确 + 按上传时间降序）
        LambdaQueryWrapper<Resumes> wrapper = new LambdaQueryWrapper<Resumes>()
                .select(
                        Resumes::getId,Resumes::getFileName,
                        Resumes::getFileType,Resumes::getFileSize,
                        Resumes::getAnalyzeStatus, Resumes::getCreatedAt
                )
                .eq(Resumes::getUserId, userId)
                .and(StrUtil.isNotBlank(fileName), w -> w.like(Resumes::getFileName,fileName))
                .and(analyzeStatus != null,w->w.eq(Resumes::getAnalyzeStatus,analyzeStatus))
                .orderByAsc("asc".equalsIgnoreCase(order), Resumes::getCreatedAt)
                .orderByDesc(!"asc".equalsIgnoreCase(order), Resumes::getCreatedAt);
        // ③ 分页查询
        Page<Resumes> resumesPage = new Page<>(page, size);
        Page<Resumes> rawPages = baseMapper.selectPage(resumesPage, wrapper);
        List<Resumes> records = rawPages.getRecords();
        // ④ 转换为 VO
        List<ResumeListItemVO> vos = records.stream()
                .map(raw ->
                        ResumeListItemVO.builder()
                                .id(raw.getId())
                                .fileName(raw.getFileName())
                                .detectedMediaType(raw.getFileType())
                                .fileSize(raw.getFileSize())
                                .analyzeStatus(raw.getAnalyzeStatus())
                                .createdAt(raw.getCreatedAt())
                                .build()
                ).toList();

        // ⑤ 封装分页结果
        Page<ResumeListItemVO> voPage = new Page<>(rawPages.getCurrent(), rawPages.getSize(), rawPages.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public ResumeVO getResumeDetail(Long resumeId) {
        // ① 查简历

        Long userId = AuthContext.getRequiredUserId();
        Resumes resumes = lambdaQuery()
                .select(
                        Resumes::getId, Resumes::getFileName,Resumes::getFileSize,
                        Resumes::getFileType,
                        Resumes::getResumeText,Resumes::getAnalyzeStatus,Resumes::getCreatedAt,
                        Resumes::getUpdatedAt
                )
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, userId)
                .one();
        if (resumes == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }

        // ② 返回详情 VO
        return ResumeVO.builder()
                .id(resumes.getId())
                .fileName(resumes.getFileName())
                .fileSize(resumes.getFileSize())
                .detectedMediaType(resumes.getFileType())
                .resumeText(resumes.getResumeText())
                .analyzeStatus(resumes.getAnalyzeStatus())
                .createdAt(resumes.getCreatedAt())
                .updatedAt(resumes.getUpdatedAt())
                .build();
    }

    @Override
    public ResumeDownloadVO getDownloadUrl(Long resumeId) {
        Long userId = AuthContext.getRequiredUserId();

        // 只读取当前用户简历的内部对象键，不向客户端直接暴露该键。
        Resumes resume = lambdaQuery()
                .select(Resumes::getStorageUrl)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, userId)
                .one();
        if (resume == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        long expiresInSeconds = 300L;
        String downloadUrl = fileStorageService.generatePresignedDownloadUrl(
                resume.getStorageUrl(), Duration.ofSeconds(expiresInSeconds));
        return new ResumeDownloadVO(downloadUrl, expiresInSeconds);
    }

    @Override
    @Transactional
    public void deleteResume(Long resumeId) {
        // ① 校验简历存在
        Long userId = AuthContext.getRequiredUserId();
        boolean exists = lambdaQuery()
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, userId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        // ② 逻辑删除 + 审计字段
        lambdaUpdate()
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, userId)
                .set(Resumes::getIsDeleted,true)
                .set(Resumes::getUpdatedAt, OffsetDateTime.now())
                .set(Resumes::getTraceId, TraceUtil.getTraceId())
                .set(Resumes::getUpdatedBy,userId)
                .update();

        // TODO 待明确 job_applications 终态与审计期限后，通过 API 发布独立的
        // RESUME_RETENTION_CLEANUP；禁止复用上传失败补偿 Topic。
    }

    @Override
    @Transactional
    public ResumeAnalyzeTriggerVO analyzeResume(Long resumeId) {
        // 校验简历存在
        Long userId = AuthContext.getRequiredUserId();
        Resumes resumes = lambdaQuery()
                .select(Resumes::getResumeText)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getUserId, userId)
                .one();
        if (resumes == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        // 更新 resumes.analyzeStatus = AnalyzeStatus.PROCESSING
        boolean update = lambdaUpdate()
                .eq(Resumes::getId, resumeId)
                .in(Resumes::getAnalyzeStatus,AnalyzeStatus.PENDING,
                        AnalyzeStatus.FAILED)
                .set(Resumes::getAnalyzeStatus, AnalyzeStatus.PROCESSING)
                .update();

        if (!update) {
            throw new BusinessException(ErrorCode.RESUME_ANALYZE_STATUS_ERROR);
        }

        // 写入本地消息表进行兜底补偿
        MessageDTO msg = MessageDTO.builder()
                .bizKey(MsgTopic.RESUME_UPLOAD_CLEANUP.name() + ":" + ":" + userId + ":" + resumeId)
                .topic(MsgTopic.RESUME_AI_PARSER)
                .schemaVersion(1)
                .payload(resumeId.toString())
                .status(MsgStatus.PENDING)
                .priority(MsgPriority.HIGH)
                .build();
        localMessageApi.saveInCurrentTransaction(msg);
        //TODO ④ 异步/同步调用大模型解析 resumeText（从 resumes 表读取）
        String resumeText = resumes.getResumeText();
        if (StrUtil.isBlank(resumeText)) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED);
        }
        resumeAnalysisTaskHandler.handleResumeAnalysis(resumes.getResumeText());
        //TODO ⑤ 解析结果写入 resume_analyses 表（overallScore, strengthsJson, suggestionsJson）
        //TODO ⑥ 写入 candidate_skill_scores 表（每个维度一条记录）
        //TODO ⑦ 合并/更新 candidate_profile 表（按 userId 聚合各维度平均分）
        //TODO ⑧ 更新 resumes.analyzeStatus = AnalyzeStatus.COMPLETED（或 FAILED）
        //TODO ⑨ Phase 4 扩展点：写入 token_consume_logs

        // 发本地消息触发异步解析（由 MessageDispatcher 消费）

        return ResumeAnalyzeTriggerVO.builder()
                .taskId(null)
                .resumeId(resumeId)
                .analyzeStatus(AnalyzeStatus.PROCESSING)
                .build();
    }

    @Override
    public ResumeAnalysisVO getAnalysis(Long resumeId) {
        // ① 校验简历存在
        Long userId = AuthContext.getRequiredUserId();
        Resumes resume = lambdaQuery()
                .select(Resumes::getUserId)
                .eq(Resumes::getUserId, userId)
                .eq(Resumes::getId, resumeId)
                .one();
        if (resume == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }

        // ② 查询分析结果主表
        ResumeAnalyses analysis = resumeAnalysesService.lambdaQuery()
                .eq(ResumeAnalyses::getResumeId, resumeId)
                .one();
        if (analysis == null) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_NOT_FOUND);
        }

        // ③ 查询各维度打分
        List<ResumeAnalysisVO.SkillScoreItem> skillScores = candidateSkillScoresService.lambdaQuery()
                .select(CandidateSkillScores::getDimensionCode,
                        CandidateSkillScores::getScore,
                        CandidateSkillScores::getAiJustification)
                .eq(CandidateSkillScores::getResumeAnalysisId, analysis.getId())
                .list()
                .stream()
                .map(s -> ResumeAnalysisVO.SkillScoreItem.builder()
                        .dimensionCode(s.getDimensionCode())
                        .score(s.getScore())
                        .aiJustification(s.getAiJustification())
                        .build())
                .toList();

        // ④ 查询用户画像聚合
        List<ResumeAnalysisVO.CandidateProfileItem> candidateProfile = candidateProfileService.lambdaQuery()
                .select(CandidateProfile::getDimensionCode,
                        CandidateProfile::getAvgScore,
                        CandidateProfile::getLatestJustification)
                .eq(CandidateProfile::getUserId, resume.getUserId())
                .list()
                .stream()
                .map(c -> ResumeAnalysisVO.CandidateProfileItem.builder()
                        .dimensionCode(c.getDimensionCode())
                        .avgScore(c.getAvgScore())
                        .latestJustification(c.getLatestJustification())
                        .build())
                .toList();

        // ⑤ 组装嵌套 VO 返回
        return ResumeAnalysisVO.builder()
                .overallScore(analysis.getOverallScore())
                .strengthsJson(analysis.getStrengthsJson() != null ? analysis.getStrengthsJson().toString() : null)
                .suggestionsJson(analysis.getSuggestionsJson() != null ? analysis.getSuggestionsJson().toString() : null)
                .analyzedAt(analysis.getAnalyzedAt())
                .skillScores(skillScores)
                .candidateProfile(candidateProfile)
                .build();
    }
}
