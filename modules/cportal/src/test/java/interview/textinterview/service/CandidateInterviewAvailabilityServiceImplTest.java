package interview.textinterview.service;

import interview.common.enums.ErrorCode;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.textinterview.mapper.CandidateInterviewAvailabilityMapper;
import interview.textinterview.model.entity.CandidateInterviewAvailability;
import interview.textinterview.model.req.CandidateInterviewAvailabilityUpdateReq;
import interview.textinterview.model.vo.CandidateInterviewAvailabilityVO;
import interview.textinterview.service.impl.CandidateInterviewAvailabilityServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateInterviewAvailabilityServiceImplTest {

    @Mock
    private CandidateInterviewAvailabilityMapper mapper;

    private CandidateInterviewAvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CandidateInterviewAvailabilityServiceImpl(new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
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
    void getMyAvailability_shouldReturnEmptyVersionZeroWhenUnset() {
        when(mapper.selectById(1L)).thenReturn(null);

        CandidateInterviewAvailabilityVO result = service.getMyAvailability();

        assertEquals("Asia/Shanghai", result.timezone());
        assertEquals(0, result.version());
        assertEquals(List.of(), result.ranges());
        assertNull(result.updatedAt());
    }

    @Test
    void updateMyAvailability_shouldRejectOverlappingRanges() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-05T10:00:00+08:00");
        CandidateInterviewAvailabilityUpdateReq req =
                new CandidateInterviewAvailabilityUpdateReq(
                        0,
                        "Asia/Shanghai",
                        List.of(
                                new CandidateInterviewAvailabilityUpdateReq.Range(
                                        start, start.plusHours(2)),
                                new CandidateInterviewAvailabilityUpdateReq.Range(
                                        start.plusHours(1), start.plusHours(3))));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateMyAvailability(req));

        assertEquals(
                ErrorCode.CANDIDATE_INTERVIEW_AVAILABILITY_INVALID.getCode(),
                exception.getCode());
    }

    @Test
    void updateMyAvailability_shouldIncrementVersionAtomically() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-05T10:00:00+08:00");
        CandidateInterviewAvailability current = CandidateInterviewAvailability.builder()
                .candidateUserId(1L)
                .timezone("Asia/Shanghai")
                .rangesJson("[]")
                .version(3)
                .build();
        when(mapper.selectById(1L)).thenReturn(current);
        when(mapper.updateByExpectedVersion(any(), eq(3))).thenReturn(1);

        CandidateInterviewAvailabilityVO result = service.updateMyAvailability(
                new CandidateInterviewAvailabilityUpdateReq(
                        3,
                        "Asia/Shanghai",
                        List.of(new CandidateInterviewAvailabilityUpdateReq.Range(
                                start, start.plusHours(2)))));

        assertEquals(4, result.version());
        assertEquals(1, result.ranges().size());
    }
}
