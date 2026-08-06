package interview.interviewcfg.api;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import interview.api.bportal.dto.InterviewScheduleCommandResultDTO;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.enums.InterviewScheduleStatus;
import interview.common.enums.InterviewType;
import interview.common.exception.BusinessException;
import interview.interviewcfg.mapper.InterviewScheduleMapper;
import interview.interviewcfg.model.bo.InterviewScheduleQueryBO;
import interview.interviewcfg.model.entity.InterviewSchedule;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewScheduleCommandApiImplTest {

    private static final long SCHEDULE_ID = 37001L;
    private static final long ENTERPRISE_ID = 10L;
    private static final long CANDIDATE_USER_ID = 5001L;

    @Mock
    private InterviewScheduleMapper mapper;

    private InterviewScheduleCommandApiImpl api;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                InterviewSchedule.class);
    }

    private InterviewScheduleCommandApiImpl newApi(InterviewScheduleQueryBO schedule) {
        InterviewScheduleCommandApiImpl impl = new InterviewScheduleCommandApiImpl(mapper, null);
        when(mapper.getScheduleWithApplicationById(SCHEDULE_ID)).thenReturn(schedule);
        return impl;
    }

    private InterviewScheduleQueryBO schedule(InterviewScheduleStatus status, int version) {
        return new InterviewScheduleQueryBO(
                SCHEDULE_ID, ENTERPRISE_ID, 200L, CANDIDATE_USER_ID, 300L, 400L,
                (short) 1, "TECHNICAL", "技术面", 600L,
                OffsetDateTime.now().plusDays(1), 60, InterviewType.TEXT,
                status, null, version,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void startScheduleTransitionsConfirmedToInProgress() {
        InterviewScheduleCommandApiImpl impl = newApi(schedule(InterviewScheduleStatus.CONFIRMED, 1));
        doReturn(1).when(mapper).update(any(), any());

        InterviewScheduleCommandResultDTO result = impl.startSchedule(SCHEDULE_ID, ENTERPRISE_ID);

        assertEquals(InterviewScheduleStatus.IN_PROGRESS, result.status());
        assertEquals(2, result.version());
        verify(mapper).update(any(), any());
    }

    @Test
    void startScheduleIsIdempotentWhenAlreadyInProgress() {
        InterviewScheduleCommandApiImpl impl = newApi(schedule(InterviewScheduleStatus.IN_PROGRESS, 2));

        InterviewScheduleCommandResultDTO result = impl.startSchedule(SCHEDULE_ID, ENTERPRISE_ID);

        assertEquals(InterviewScheduleStatus.IN_PROGRESS, result.status());
        assertEquals(2, result.version());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void startScheduleRejectsWrongEnterprise() {
        InterviewScheduleCommandApiImpl impl = newApi(schedule(InterviewScheduleStatus.CONFIRMED, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> impl.startSchedule(SCHEDULE_ID, 99L));

        assertEquals(ErrorCode.INTERVIEW_SCHEDULE_NOT_YOUR_ENTERPRISE.getCode(), ex.getCode());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void startScheduleRejectsInvalidSourceStatus() {
        InterviewScheduleCommandApiImpl impl = newApi(schedule(InterviewScheduleStatus.COMPLETED, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> impl.startSchedule(SCHEDULE_ID, ENTERPRISE_ID));

        assertEquals(ErrorCode.INTERVIEW_SCHEDULE_STATUS_INVALID.getCode(), ex.getCode());
        verify(mapper, never()).update(any(), any());
    }
}
