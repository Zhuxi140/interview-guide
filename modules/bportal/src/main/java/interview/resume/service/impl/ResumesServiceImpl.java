package interview.resume.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.FileSort;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.framework.file.FileHashService;
import interview.framework.file.FileParseService;
import interview.framework.file.FileStorageService;
import interview.resume.mapper.ResumesMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.entity.CandidateSkillScores;
import interview.resume.model.entity.ResumeAnalyses;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.req.ResumeUploadReq;
import interview.resume.model.vo.ResumeAnalysisVO;
import interview.resume.model.vo.ResumeListItemVO;
import interview.resume.model.vo.ResumeUploadVO;
import interview.resume.model.vo.ResumeVO;
import interview.resume.service.CandidateProfileService;
import interview.resume.service.CandidateSkillScoresService;
import interview.resume.service.ResumeAnalysesService;
import interview.resume.service.ResumesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 */
@RequiredArgsConstructor
@Service
public class ResumesServiceImpl extends ServiceImpl<ResumesMapper, Resumes> implements ResumesService {

    private final ResumeAnalysesService resumeAnalysesService;
    private final CandidateSkillScoresService candidateSkillScoresService;
    private final CandidateProfileService candidateProfileService;
    private final FileStorageService fileStorageService;
    private final FileHashService fileHashService;
    private final FileParseService fileParseService;


    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public ResumeUploadVO uploadResume(MultipartFile file, ResumeUploadReq metadata) {
        // ① 获取当前用户
        Long userId = AuthContext.getRequiredUserId();
        long size = file.getSize();
        String originalFilename = file.getOriginalFilename();
        String type;
        try {
            type = fileStorageService.verifyFileType(size, originalFilename, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        // ② 计算文件哈希并查重
        String hash = fileHashService.calculateHash(file);
        boolean exists = lambdaQuery()
                .eq(Resumes::getFileHash, hash)
                .eq(Resumes::getUserId, userId)
                .exists();
        if (exists) {
            throw new BusinessException(ErrorCode.FILE_IS_EXISTS);
        }

        // ③ 先上传新文件到 RustFS（S3 操作先于 DB，事务回滚时需补偿）
        // TODO[2.5]: 上传成功后插入 local_message(FILE_DELETE, storageUrl)，事务提交后异步删除，失败自动重试
        String storageUrl = fileStorageService.uploadFile(file, FileSort.RESUME);

        // ④ 旧 S3 key 留待事务提交后删除
        Resumes oldResume = lambdaQuery()
                .select(Resumes::getStorageUrl)
                .eq(Resumes::getUserId, userId)
                .one();
        String oldStorageUrl = oldResume != null ? oldResume.getStorageUrl() : null;

        try {
            String name;
            if (StrUtil.isNotBlank(metadata.getFileName())) {
                name = metadata.getFileName();
            }else{
                name = originalFilename;
            }
            // ⑤ Tika 提取文本内容
            String resumeText = fileParseService.parseText(file);

            // ⑥ 构造简历实体并入库
            OffsetDateTime now = OffsetDateTime.now();
            Resumes resumes = Resumes.builder()
                    .fileHash(hash)
                    .userId(userId)
                    .fileName(name)
                    .fileSize(file.getSize())
                    .fileType(type)
                    .storageUrl(storageUrl)
                    .resumeText(resumeText)
                    .analyzeStatus(AnalyzeStatus.PENDING)
                    .createdAt(now)
                    .build();
            save(resumes);

            // ⑦ 事务提交后删除旧 S3 文件
            // TODO[2.5]: 改为插入 local_message(FILE_DELETE, oldStorageUrl)，由调度器异步删除，失败自动重试
            if (oldStorageUrl != null) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileStorageService.deleteFile(oldStorageUrl);
                    }
                });
            }

            // ⑧ 返回上传结果
            return ResumeUploadVO.builder()
                    .id(resumes.getId())
                    .fileSize(size)
                    .fileType(type)
                    .fileName(name)
                    .analyzeStatus(AnalyzeStatus.PENDING)
                    .createdAt(now)
                    .build();
        } catch (BusinessException e) {
            // TODO[2.5]: 同步删除改为 local_message 异步删除，避免 deleteFile 失败吞掉异常
            fileStorageService.deleteFile(storageUrl);
            throw e;
        }catch (RuntimeException e){
            // TODO[2.5]: 同上，改为 local_message 异步补偿
            fileStorageService.deleteFile(storageUrl);
            log.error("上传文件触发RuntimeException异常",e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public IPage<ResumeListItemVO> pageResumes(Integer page, Integer size, String fileName, AnalyzeStatus analyzeStatus) {
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
                .orderByDesc(Resumes::getCreatedAt);
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
                                .fileType(raw.getFileType())
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
        Resumes resumes = lambdaQuery()
                .select(
                        Resumes::getUserId,Resumes::getFileName,Resumes::getFileSize,
                        Resumes::getFileType,Resumes::getFileHash,Resumes::getStorageUrl,
                        Resumes::getResumeText,Resumes::getAnalyzeStatus,Resumes::getCreatedAt,
                        Resumes::getUpdatedAt
                )
                .eq(Resumes::getId, resumeId)
                .one();
        if (resumes == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }

        // ② 返回详情 VO
        return ResumeVO.builder()
                .id(resumes.getId())
                .userId(resumes.getUserId())
                .fileName(resumes.getFileName())
                .fileSize(resumes.getFileSize())
                .fileType(resumes.getFileType())
                .fileHash(resumes.getFileHash())
                .storageUrl(resumes.getStorageUrl())
                .resumeText(resumes.getResumeText())
                .analyzeStatus(resumes.getAnalyzeStatus())
                .createdAt(resumes.getCreatedAt())
                .updatedAt(resumes.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteResume(Long resumeId) {
        // ① 校验简历存在
        Long userId = AuthContext.getRequiredUserId();
        boolean exists = lambdaQuery()
                .eq(Resumes::getId, resumeId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        // ② 逻辑删除 + 审计字段
        lambdaUpdate()
                .eq(Resumes::getId, resumeId)
                .set(Resumes::getIsDeleted,true)
                .set(Resumes::getUpdatedAt, OffsetDateTime.now())
                // TODO: traceId完善后，要传入
                .set(Resumes::getTraceId,null)
                .set(Resumes::getUpdatedBy,userId)
                .update();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public ResumeVO analyzeResume(Long resumeId) {
        //TODO ① 校验简历存在
        boolean exists = lambdaQuery()
                .eq(Resumes::getId, resumeId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        //TODO ③ 更新 resumes.analyzeStatus = AnalyzeStatus.PROCESSING

        //TODO ④ 异步/同步调用大模型解析 resumeText（从 resumes 表读取）
        //TODO ⑤ 解析结果写入 resume_analyses 表（overallScore, strengthsJson, suggestionsJson）
        //TODO ⑥ 写入 candidate_skill_scores 表（每个维度一条记录）
        //TODO ⑦ 合并/更新 candidate_profile 表（按 userId 聚合各维度平均分）
        //TODO ⑧ 更新 resumes.analyzeStatus = AnalyzeStatus.COMPLETED（或 FAILED）
        //TODO ⑨ Phase 4 扩展点：写入 token_consume_logs
        return null;
    }

    @Override
    public ResumeAnalysisVO getAnalysis(Long resumeId) {
        // ① 校验简历存在
        Resumes resume = lambdaQuery()
                .select(Resumes::getUserId)
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
