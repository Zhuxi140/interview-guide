package interview.resume.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
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
import interview.resume.model.vo.ResumeVO;
import interview.resume.service.CandidateProfileService;
import interview.resume.service.CandidateSkillScoresService;
import interview.resume.service.ResumeAnalysesService;
import interview.resume.service.ResumesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author zhuxi
 */
@RequiredArgsConstructor
@Service
public class ResumesServiceImpl extends ServiceImpl<ResumesMapper, Resumes> implements ResumesService {

    private final EnterpriseValidationApi enterpriseValidationApi;
    private final ResumeAnalysesService resumeAnalysesService;
    private final CandidateSkillScoresService candidateSkillScoresService;
    private final CandidateProfileService candidateProfileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO uploadResume(Long enterpriseId, MultipartFile file, ResumeUploadReq metadata) {
        //TODO ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        //TODO ② 校验文件格式：仅 pdf/doc/docx，超 10MB 抛 BusinessException(10008)
        //TODO ③ 计算文件 SHA-256 哈希
        //TODO ④ 查重：enterpriseId + fileHash 唯一约束，若已存在则返回错误 10008 附带已有记录 ID
        //TODO ⑤ 上传文件至 RustFS / OSS，获取 storageUrl
        //TODO ⑥ 提取文件元信息：fileSize、fileType（根据扩展名）
        //TODO ⑦ INSERT resumes 记录，设置 analyzeStatus = AnalyzeStatus.PENDING、createdAt = now
        //TODO ⑧ 构造 ResumeVO 返回
        return null;
    }

    @Override
    public IPage<ResumeListItemVO> pageResumes(Long enterpriseId, Integer page, Integer size, String fileName, AnalyzeStatus analyzeStatus) {
        // 纯CRUD
        //TODO ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId());
        //TODO ② 构建 LambdaQueryWrapper，条件：enterpriseId + fileName 模糊 + analyzeStatus 精确
        LambdaQueryWrapper<Resumes> wrapper = new LambdaQueryWrapper<Resumes>()
                .select(
                        Resumes::getId,Resumes::getFileName,
                        Resumes::getFileType,Resumes::getFileSize,
                        Resumes::getAnalyzeStatus, Resumes::getCreatedAt
                )
                .eq(Resumes::getEnterpriseId,enterpriseId)
                .and(StrUtil.isNotBlank(fileName), w -> w.like(Resumes::getFileName,fileName))
                .and(analyzeStatus != null,w->w.eq(Resumes::getAnalyzeStatus,analyzeStatus))
                .orderByDesc(Resumes::getCreatedAt);
        //TODO ③ baseMapper.selectPage + 类型转换至 ResumeListItemVO
        Page<Resumes> resumesPage = new Page<>(page, size);
        Page<Resumes> rawPages = baseMapper.selectPage(resumesPage, wrapper);
        //TODO ④ 返回 IPage<ResumeListItemVO>
        List<Resumes> records = rawPages.getRecords();
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

        Page<ResumeListItemVO> voPage = new Page<>(rawPages.getCurrent(), rawPages.getSize(), rawPages.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public ResumeVO getResumeDetail(Long enterpriseId, Long resumeId) {
        // 纯CRUD
        //TODO ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId());

        boolean exists = lambdaQuery()
                .eq(Resumes::getEnterpriseId, enterpriseId)
                .eq(Resumes::getId, resumeId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        //TODO ② lambdaQuery().eq(Resumes::getEnterpriseId, enterpriseId).eq(Resumes::getId, resumeId).one()
        Resumes resumes = lambdaQuery()
                .select(
                        Resumes::getUserId,Resumes::getFileName,Resumes::getFileSize,
                        Resumes::getFileType,Resumes::getFileHash,Resumes::getStorageUrl,
                        Resumes::getResumeText,Resumes::getAnalyzeStatus,Resumes::getCreatedAt,
                        Resumes::getUpdatedAt
                )
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getEnterpriseId, enterpriseId)
                .one();

        resumes.setId(resumeId);
        resumes.setEnterpriseId(enterpriseId);

        //TODO ④ 构造 ResumeVO 返回（含 resumeText）
        return ResumeVO.builder()
                .id(resumes.getId())
                .enterpriseId(resumes.getEnterpriseId())
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
    public void deleteResume(Long enterpriseId, Long resumeId) {
        // 纯CRUD
        //TODO ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,userId );

        boolean exists = lambdaQuery()
                .eq(Resumes::getEnterpriseId, enterpriseId)
                .eq(Resumes::getId, resumeId)
                .exists();

        if (!exists){
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        //TODO ② lambdaUpdate().eq(enterpriseId).eq(id).set(isDeleted=true).update()
        lambdaUpdate()
                .eq(Resumes::getEnterpriseId, enterpriseId)
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
    public ResumeVO analyzeResume(Long enterpriseId, Long resumeId) {
        //TODO ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        //TODO ② 校验简历存在 + 归属 enterpriseId
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
    public ResumeAnalysisVO getAnalysis(Long enterpriseId, Long resumeId) {
        // ① 校验
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId());

        // ② 校验简历存在 + 获取 userId
        Resumes resume = lambdaQuery()
                .select(Resumes::getUserId)
                .eq(Resumes::getId, resumeId)
                .eq(Resumes::getEnterpriseId, enterpriseId)
                .one();
        if (resume == null) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }

        // ③ 查询 resume_analyses
        ResumeAnalyses analysis = resumeAnalysesService.lambdaQuery()
                .eq(ResumeAnalyses::getResumeId, resumeId)
                .eq(ResumeAnalyses::getEnterpriseId, enterpriseId)
                .one();
        if (analysis == null) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_NOT_FOUND);
        }

        // ④ 查询 skillScores
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

        // ⑤ 查询 candidateProfile
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

        // ⑥ 构造返回
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
