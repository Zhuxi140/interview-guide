package interview.matching.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.job.model.entity.Job;
import interview.job.service.JobService;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.bo.JobApplicationBO;
import interview.matching.model.bo.JobApplicationListBO;
import interview.matching.model.bo.MyApplicationListBO;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.JobApplicationStatusReq;
import interview.matching.model.req.JobApplicationSubmitReq;
import interview.matching.model.vo.JobApplicationListItemVO;
import interview.matching.model.vo.JobApplicationVO;
import interview.matching.model.vo.MyApplicationListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @author zhuxi
 */
@RequiredArgsConstructor
@Service
public class JobApplicationsServiceImpl extends ServiceImpl<JobApplicationsMapper, JobApplications> implements JobApplicationsService {

    private final JobApplicationsMapper jobApplicationsMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;
    private final UserApi userApi;
    private final JobService jobService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public JobApplicationVO submitApplication(Long jobId, JobApplicationSubmitReq req) {
        //TODO 【提交投递】
        // ① 校验岗位：查 jobs 表 id=jobId，不存在抛 40001，status!=OPEN 抛 40002
        // ② 校验简历：查 resumes 表 id=req.resumeId 且 isDeleted=false，不存在抛 40004
        // ③ 校验简历归属：resumes.userId 必须等于当前登录用户（不可投递他人简历）
        // ④ 防重复投递：同一 candidateId + jobId + isDeleted=false → 抛 40010
        // ⑤ 校验当前用户 userType = CANDIDATE，否则抛权限异常
        // ⑥ Phase 8 预留：校验 sys_user_kyc.auth_status = PASSED
        // ⑦ Phase 4 预留：校验钱包余额充足
        // ⑧ 构造 JobApplications（id=雪花id, enterpriseId从job获取, candidateId取当前用户, status=APPLIED）
        // ⑨ INSERT job_applications
        // ⑩ 异步 AI 人岗匹配：读取 resumeText + jdContent → AiRouter.chat → 更新 aiMatchScore
        // ⑪ Phase 4 预留：写入 token_consume_logs
        return null;
    }

    @Override
    public IPage<JobApplicationListItemVO> pageApplications(Long enterpriseId, Long jobId, Integer page, Integer size, JobApplicationStatus status) {
        // 纯CRUD
        //TODO 【HR 端分页查询投递列表】
        // ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        Long userId = AuthContext.getRequiredUserId();
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,userId);
        // ② 校验：verifyJobExists(enterpriseId, jobId)，岗位不存在抛 40001
        boolean exists = jobService.lambdaQuery()
                .eq(Job::getEnterpriseId, enterpriseId)
                .eq(Job::getId, jobId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND);
        }
        // ③ 构建 Page 分页对象
        Page<JobApplications> pageObj = new Page<>(page, size);
        // ④ 调用 jobApplicationsMapper.pageApplicationsWithJoin(IPage, enterpriseId, jobId, status)
        IPage<JobApplicationListBO> boPage = jobApplicationsMapper.pageApplicationsWithJoin(pageObj, enterpriseId, jobId, status);
        List<JobApplicationListBO> records = boPage.getRecords();
        List<Long> userIds = records.stream()
                .map(JobApplicationListBO::candidateId)
                .toList();

        Map<Long, String> nameMap = userApi.getUserNamesByIds(userIds);

        List<JobApplicationListItemVO> vos = records.stream()
                                                .map(bo ->
                                                        JobApplicationListItemVO.builder()
                                                                .id(bo.id())
                                                                .candidateId(bo.candidateId())
                                                                .candidateName(nameMap.get(bo.candidateId()))
                                                                .resumeFileName(bo.resumeFileName())
                                                                .aiMatchScore(bo.aiMatchScore())
                                                                .status(bo.status())
                                                                .createdAt(bo.createdAt())
                                                                .build()
                                                ).toList();
        // ⑤ 返回 IPage<JobApplicationListItemVO>
        Page<JobApplicationListItemVO> voPage = new Page<>(boPage.getCurrent(), boPage.getSize(), boPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public JobApplicationVO getApplicationDetail(Long enterpriseId, Long applicationId) {
        // 纯CRUD
        //TODO 【查询投递详情】
        // ① 校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId,AuthContext.getRequiredUserId());

        boolean exists = lambdaQuery()
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .exists();
        if (!exists){
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        // ② 调用 jobApplicationsMapper.getApplicationWithJoin(applicationId, enterpriseId)
        JobApplicationBO bo = jobApplicationsMapper.getApplicationWithJoin(enterpriseId, applicationId);

        String candidateName = userApi.getUserNameById(bo.candidateId());

        // ④ 返回 JobApplicationVO
        return JobApplicationVO.builder()
                .id(applicationId)
                .enterpriseId(enterpriseId)
                .jobId(bo.jobId())
                .candidateId(bo.candidateId())
                .candidateName(candidateName)
                .resumeId(bo.resumeId())
                .resumeFileName(bo.resumeFileName())
                .aiMatchScore(bo.aiMatchScore())
                .status(bo.status())
                .createdAt(bo.createdAt())
                .updatedAt(bo.updatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void updateApplicationStatus(Long enterpriseId, Long applicationId, JobApplicationStatusReq req) {
        // 纯CRUD
        //TODO 【HR 更新投递状态】

        // ①排除 APPLIED：不允许回退到"已投递"状态，否则抛 40011（非法值由 Jackson 枚举反序列化兜底）
        if (req.getStatus().equals(JobApplicationStatus.APPLIED)){
            throw new BusinessException(ErrorCode.JOB_APPLICATION_STATUS_INVALID);
        }

        // ②校验：enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId())
        enterpriseValidationApi.validateEnterpriseBelong(enterpriseId, AuthContext.getRequiredUserId());

        // 检查记录是否存在
        boolean exists = lambdaQuery()
                .eq(JobApplications::getId, applicationId)
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .exists();

        if (!exists){
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND);
        }

        // ③ 构造条件更新：id + enterpriseId + isDeleted=false，set status + updatedBy + updatedAt
        lambdaUpdate()
                .eq(JobApplications::getEnterpriseId, enterpriseId)
                .eq(JobApplications::getId, applicationId)
                .set(JobApplications::getStatus, req.getStatus())
                .update();
    }

    @Override
    public IPage<MyApplicationListItemVO> pageMyApplications(Integer page, Integer size, JobApplicationStatus status) {
        // 纯CRUD
        //TODO 【C 端分页查询我的投递记录】
        // ① 从 AuthContext.getRequiredUserId() 获取当前用户 ID 作为 candidateId
        Long candidateId = AuthContext.getRequiredUserId();
        // ② 构建 Page 分页对象
        Page<JobApplications> rawPage = new Page<>(page,size);
        // ③ 调用 jobApplicationsMapper.pageMyApplicationsWithJoin(IPage, candidateId, status)
        IPage<MyApplicationListBO> boPage = jobApplicationsMapper.pageMyApplicationsWithJoin(rawPage, candidateId, status);

        List<MyApplicationListBO> records = boPage.getRecords();
        List<Long> enterpriseIds = records.stream()
                .map(MyApplicationListBO::enterpriseId)
                .toList();
        // 查询EnterpriseName
        Map<Long, String> nameMap = enterpriseValidationApi.getNameList(enterpriseIds);
        // ④ 返回 IPage<MyApplicationListItemVO>
        List<MyApplicationListItemVO> vos = records.stream()
                .map(bo ->
                        MyApplicationListItemVO
                                .builder()
                                .id(bo.id())
                                .jobId(bo.jobId())
                                .jobTitle(bo.jobTitle())
                                .enterpriseName(nameMap.get(bo.enterpriseId()))
                                .aiMatchScore(bo.aiMatchScore())
                                .status(bo.status())
                                .createdAt(bo.createdAt())
                                .build()
                ).toList();

        Page<MyApplicationListItemVO> voPage = new Page<>(boPage.getCurrent(), boPage.getSize(), boPage.getTotal());
        voPage.setRecords(vos);
        return voPage;

    }

}
