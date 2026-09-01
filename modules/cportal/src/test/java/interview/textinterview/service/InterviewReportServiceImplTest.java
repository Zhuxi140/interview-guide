package interview.textinterview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.model.vo.InterviewReportCandidateListItemVO;
import interview.textinterview.service.InterviewReportServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewReportServiceImplTest {

    @Mock
    private InterviewScheduleQueryApi interviewScheduleQueryApi;
    @Mock
    private EnterpriseValidationApi enterpriseValidationApi;

    private InterviewReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InterviewReportServiceImpl(
                interviewScheduleQueryApi, enterpriseValidationApi);
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
    void getCandidateReport_shouldRejectAnotherCandidatesSchedule() {
        // 排期属于其他候选人时，不得继续读取报告表。
        when(interviewScheduleQueryApi.getSchedule(10L))
                .thenReturn(schedule(10L, 20L, 2L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getCandidateReport(1L, 10L)
        );

        assertEquals(
                ErrorCode.INTERVIEW_REPORT_NOT_FOUND.getCode(),
                exception.getCode()
        );
    }

    @Test
    void getReportBySchedule_shouldRejectAnotherEnterprisesSchedule() {
        // 企业路径中的 enterpriseId 必须与排期实际归属一致。
        when(interviewScheduleQueryApi.getSchedule(10L))
                .thenReturn(schedule(10L, 20L, 1L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getReportBySchedule(30L, 10L)
        );

        assertEquals(
                ErrorCode.INTERVIEW_SCHEDULE_NOT_FOUND.getCode(),
                exception.getCode()
        );
    }

    @Test
    void pageCandidateReports_shouldReturnEmptyPageWithoutSchedules() {
        // 没有本人排期时直接返回空分页，避免生成空 IN 查询。
        when(interviewScheduleQueryApi.listScheduleIdsByCandidate(1L))
                .thenReturn(List.of());

        IPage<InterviewReportCandidateListItemVO> result =
                service.pageCandidateReports(1L, 1, 20, null);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    private InterviewScheduleQueryDTO schedule(Long scheduleId,
                                                Long enterpriseId,
                                                Long candidateUserId) {
        return new InterviewScheduleQueryDTO(
                scheduleId,
                enterpriseId,
                100L,
                candidateUserId,
                200L,
                300L,
                (short) 1,
                "TECHNICAL",
                "技术面",
                400L,
                "候选人",
                "企业",
                "岗位",
                OffsetDateTime.now().plusDays(1),
                60,
                InterviewType.TEXT,
                InterviewScheduleStatus.CONFIRMED,
                null,
                0,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
