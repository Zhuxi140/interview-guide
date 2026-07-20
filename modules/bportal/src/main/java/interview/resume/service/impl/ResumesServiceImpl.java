package interview.resume.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.infra.FileHashApi;
import interview.api.infra.FileParseApi;
import interview.api.infra.FileStorageApi;
import interview.api.infra.LocalMessageApi;
import interview.api.infra.dto.MessageDTO;
import interview.common.enums.*;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.OffsetDateTime;
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
    private final FileStorageApi fileStorageService;
    private final FileHashApi fileHashService;
    private final FileParseApi fileParseService;
    private final LocalMessageApi localMessageApi;
    private final TransactionTemplate transactionTemplate;
    private final ResumesMapper resumeMapper;


    @Override
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

        // 对逻辑删除的记录进行查询  并筛选是否哈希重复
        String url = resumeMapper.selectDeletedByHash(hash, userId);


        // 若重复 直接复用url。 若不重复则进行上传操作
        // ③ 先插入消息表，提前兜底补偿。如果无异常无回滚，再上传文件
        String newUrl = fileStorageService.generateFileKey(originalFilename, FileSort.RESUME);
        Long id;
        try {
            MessageDTO msg = MessageDTO.builder()
                    .topic(MsgTopic.FILE_DELETE)
                    .status(MsgStatus.PENDING)
                    .payload(newUrl)
                    .lastError("上传简历，提前兜底补偿")
                    .priority(MsgPriority.LOW)
                    .maxRetries(2)
                    .build();
            id = localMessageApi.saveMsgNewTransaction(msg);
        }catch (Exception e){
            log.error("上传简历 - 提前写入消息表兜底补偿失败，直接拦截。 error：{}",e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        final String storageUrl = StrUtil.isNotBlank(url) ? url : newUrl;
        fileStorageService.uploadFile(file, FileSort.RESUME,newUrl);

        return transactionTemplate.execute(status -> {
            // ⑤ Tika 提取文本内容
            String resumeText;
            try {
                resumeText = fileParseService.parseText(file);

            String name = StrUtil.isNotBlank(metadata.getFileName()) ?
                    metadata.getFileName() : originalFilename;

            Resumes oldResume = lambdaQuery()
                    .select(Resumes::getStorageUrl)
                    .eq(Resumes::getUserId, userId)
                    .one();
            String oldStorageUrl = oldResume != null ? oldResume.getStorageUrl() : null;

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

            if (oldStorageUrl != null) {
                MessageDTO msg1 = MessageDTO.builder()
                        .topic(MsgTopic.FILE_DELETE)
                        .status(MsgStatus.PENDING)
                        .payload(oldStorageUrl)
                        .lastError("冗余旧文件，需删除:" + oldStorageUrl)
                        .priority(MsgPriority.LOW)
                        .maxRetries(2)
                        .build();
                localMessageApi.saveMsg(msg1);
            }

            // 更新提前兜底补偿的消息记录  更新状态为IGNORED
            localMessageApi.updateStatus(id,MsgStatus.IGNORED);

            return ResumeUploadVO.builder()
                    .id(resumes.getId())
                    .fileName(resumes.getFileName())
                    .fileType(resumes.getFileType())
                    .fileSize(resumes.getFileSize())
                    .createdAt(resumes.getCreatedAt())
                    .analyzeStatus(resumes.getAnalyzeStatus())
                    .build();

            }catch (Exception e){
                status.setRollbackOnly();
                log.error("上传简历 - 主事务内出现异常。 msg:{}",e.getMessage());
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        });
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
