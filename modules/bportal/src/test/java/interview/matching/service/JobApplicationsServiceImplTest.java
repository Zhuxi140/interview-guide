package interview.matching.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.UserApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.job.model.entity.Job;
import interview.job.model.enums.JobStatus;
import interview.job.service.JobService;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.JobApplicationSubmitReq;
import interview.resume.model.entity.Resumes;
import interview.resume.service.ResumesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationsServiceImplTest {

    @Mock
    private JobApplicationsMapper mapper;
    @Mock
    private EnterpriseValidationApi enterpriseValidationApi;
    @Mock
    private UserApi userApi;
    @Mock
    private JobService jobService;
    @Mock
    private ResumesService resumesService;

    private JobApplicationsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new JobApplicationsServiceImpl(
                mapper, enterpriseValidationApi, userApi, jobService, resumesService));
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(1L)
                .userType(UserType.CANDIDATE)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitApplication_shouldRejectIdempotencyKeyReusedForDifferentRequest() {
        // 岗位和简历均合法，但同一幂等键已绑定另一岗位时必须拒绝复用。
        LambdaQueryChainWrapper<Job> jobQuery =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        when(jobService.lambdaQuery()).thenReturn(jobQuery);
        when(jobQuery.select(any(SFunction[].class))).thenReturn(jobQuery);
        when(jobQuery.eq(any(), any())).thenReturn(jobQuery);
        when(jobQuery.one()).thenReturn(Job.builder()
                .enterpriseId(10L)
                .status(JobStatus.OPEN)
                .build());

        LambdaQueryChainWrapper<Resumes> resumeQuery =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        when(resumesService.lambdaQuery()).thenReturn(resumeQuery);
        when(resumeQuery.select(any(SFunction[].class))).thenReturn(resumeQuery);
        when(resumeQuery.eq(any(), any())).thenReturn(resumeQuery);
        when(resumeQuery.one()).thenReturn(Resumes.builder().userId(1L).build());

        LambdaQueryChainWrapper<JobApplications> applicationQuery =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        doReturn(applicationQuery).when(service).lambdaQuery();
        when(applicationQuery.select(any(SFunction[].class))).thenReturn(applicationQuery);
        when(applicationQuery.eq(any(), any())).thenReturn(applicationQuery);
        when(applicationQuery.one()).thenReturn(JobApplications.builder()
                .id(100L)
                .enterpriseId(10L)
                .jobId(99L)
                .resumeId(20L)
                .status(JobApplicationStatus.APPLIED)
                .createdAt(OffsetDateTime.now())
                .build());
        when(mapper.insertIgnore(any(JobApplications.class))).thenReturn(0);

        JobApplicationSubmitReq req = new JobApplicationSubmitReq();
        req.setResumeId(20L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitApplication(11L, req, "idem-1"));

        assertEquals(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.getCode(), exception.getCode());
    }
}
