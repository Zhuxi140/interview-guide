package interview.matching.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import interview.api.infra.LocalMessageApi;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.AiTaskStatus;
import interview.common.enums.ScreeningRecommendation;
import interview.common.enums.UserType;
import interview.framework.context.AuthContext;
import interview.job.service.JobService;
import interview.matching.mapper.ApplicationAiScreeningMapper;
import interview.matching.mapper.JobApplicationsMapper;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.entity.JobApplications;
import interview.matching.model.enums.JobApplicationStatus;
import interview.matching.model.req.ApplicationAiReviewReq;
import interview.resume.mapper.CandidateProfileMapper;
import interview.resume.mapper.CandidateSkillScoresMapper;
import interview.resume.service.CandidateProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ApplicationAiScreeningServiceImplTest {

    private final JobApplicationsMapper applicationMapper =
            mock(JobApplicationsMapper.class);
    private final ApplicationAiScreeningMapper screeningMapper =
            mock(ApplicationAiScreeningMapper.class);
    private ApplicationAiScreeningServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new ApplicationAiScreeningServiceImpl(
                applicationMapper,
                mock(JobScreeningConfigService.class),
                mock(JobService.class),
                mock(CandidateProfileService.class),
                mock(CandidateProfileMapper.class),
                mock(CandidateSkillScoresMapper.class),
                mock(MatchingAiInputService.class),
                mock(EnterpriseValidationApi.class),
                mock(LocalMessageApi.class),
                mock(ApplicationEventPublisher.class),
                mock(ObjectMapper.class)));
        ReflectionTestUtils.setField(service, "baseMapper", screeningMapper);
        AuthContext.setAuthContext(AuthContext.AuthUser.builder()
                .userId(7L)
                .userType(UserType.HR)
                .build());
    }

    @AfterEach
    void tearDown() {
        AuthContext.remove();
    }

    @ParameterizedTest
    @CsvSource({
            "RECOMMEND_PASS,PASSED",
            "RECOMMEND_PASS,REJECTED",
            "RECOMMEND_REJECT,PASSED",
            "RECOMMEND_REJECT,REJECTED"
    })
    @SuppressWarnings("unchecked")
    void review_shouldAllowHrToConfirmOrOverrideRecommendation(
            ScreeningRecommendation recommendation,
            JobApplicationStatus decision) {
        when(applicationMapper.selectByIdForUpdate(20L)).thenReturn(
                JobApplications.builder()
                        .id(20L)
                        .enterpriseId(10L)
                        .status(JobApplicationStatus.REVIEWING)
                        .build());
        LambdaQueryChainWrapper<ApplicationAiScreening> query = mock(
                LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        org.mockito.Mockito.doReturn(query).when(service).lambdaQuery();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(ApplicationAiScreening.builder()
                .id(30L)
                .applicationId(20L)
                .status(AiTaskStatus.COMPLETED)
                .recommendation(recommendation)
                .build());
        when(applicationMapper.update(any(), any())).thenReturn(1);
        when(screeningMapper.update(any(), any())).thenReturn(1);

        ApplicationAiReviewReq req = new ApplicationAiReviewReq();
        req.setExpectedApplicationStatus(JobApplicationStatus.REVIEWING);
        req.setDecision(decision);

        var result = service.review(10L, 20L, 30L, req);

        assertEquals(decision, result.applicationStatus());
    }
}
