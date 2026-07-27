package interview.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.dto.EnterprisePublicProfileDTO;
import interview.common.enums.EducationLevel;
import interview.common.enums.ExperienceLevel;
import interview.common.exception.BusinessException;
import interview.job.mapper.JobMapper;
import interview.job.model.entity.Job;
import interview.job.model.enums.JobStatus;
import interview.job.model.req.CandidateJobSearchReq;
import interview.job.model.vo.CandidateJobDetailVO;
import interview.job.model.vo.CandidateJobListItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private EnterpriseValidationApi enterpriseValidationApi;

    private JobServiceImpl jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobServiceImpl(enterpriseValidationApi, new ObjectMapper());
        ReflectionTestUtils.setField(jobService, "baseMapper", jobMapper);
    }

    @Test
    void pageCandidateJobs_shouldReturnOnlyPublicEnterpriseData() {
        // 构造 system 模块返回的可公开企业及开放岗位分页数据。
        EnterprisePublicProfileDTO enterprise = enterprise();
        CandidateJobSearchReq req = new CandidateJobSearchReq();
        req.setIndustry("互联网");
        Job job = job();
        Page<Job> jobPage = new Page<>(1, 20, 1);
        jobPage.setRecords(List.of(job));
        when(enterpriseValidationApi.listPublicEnterprises("互联网"))
                .thenReturn(List.of(enterprise));
        when(jobMapper.selectPage(any(), any()))
                .thenReturn(jobPage);

        var result = jobService.pageCandidateJobs(req);

        CandidateJobListItemVO vo = result.getRecords().getFirst();
        assertEquals(1, result.getTotal());
        assertEquals("示例科技", vo.enterpriseName());
        assertEquals("Java 后端工程师", vo.title());
        verify(enterpriseValidationApi).listPublicEnterprises("互联网");
    }

    @Test
    void getCandidateJobDetail_shouldHideUnavailableEnterprise() {
        // 岗位存在但企业不可公开时，对 C 端统一表现为岗位不存在。
        when(jobMapper.selectOne(any())).thenReturn(job());
        when(enterpriseValidationApi.getPublicEnterprise(2L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> jobService.getCandidateJobDetail(1L));
    }

    @Test
    void getCandidateJobDetail_shouldReturnPublicDetail() {
        // 岗位和企业均可公开时返回组合后的详情。
        when(jobMapper.selectOne(any())).thenReturn(job());
        when(enterpriseValidationApi.getPublicEnterprise(2L))
                .thenReturn(enterprise());

        CandidateJobDetailVO vo = jobService.getCandidateJobDetail(1L);

        assertEquals("示例科技", vo.enterpriseName());
        assertEquals("互联网", vo.industry());
        assertEquals("Java 后端工程师", vo.title());
    }

    private Job job() {
        return Job.builder()
                .id(1L)
                .enterpriseId(2L)
                .title("Java 后端工程师")
                .jdContent("负责核心服务开发")
                .department("技术研发部")
                .location("上海")
                .minSalary(new BigDecimal("15000"))
                .maxSalary(new BigDecimal("25000"))
                .experienceReq(ExperienceLevel.ONE_TO_THREE)
                .educationReq(EducationLevel.BACHELOR)
                .skillsJson("[\"Java\",\"PostgreSQL\"]")
                .status(JobStatus.OPEN)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private EnterprisePublicProfileDTO enterprise() {
        return new EnterprisePublicProfileDTO(
                2L,
                "示例科技有限公司",
                "示例科技",
                "互联网",
                "100-499人",
                "https://example.com/logo.png"
        );
    }
}
