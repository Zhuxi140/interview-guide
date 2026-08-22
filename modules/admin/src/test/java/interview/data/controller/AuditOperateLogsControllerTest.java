package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.model.vo.OperateLogChangeVO;
import interview.data.model.vo.OperateLogDetailVO;
import interview.data.model.vo.OperateLogListItemVO;
import interview.data.service.OperateLogQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditOperateLogsControllerTest {

    @Mock
    private OperateLogQueryService operateLogQueryService;

    private AuditOperateLogsController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditOperateLogsController(operateLogQueryService);
    }

    @Nested
    class ListOperateLogs {

        @Test
        void listOperateLogs_success() {
            Page<OperateLogListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new OperateLogListItemVO(
                    1L, "trace-1", 100L, "hr_user", "job", "UPDATE", "jobs", 123L,
                    OffsetDateTime.now())));
            when(operateLogQueryService.pageOperateLogs(
                    1, 20, null, "job", "UPDATE", "jobs", 123L, null, null)).thenReturn(page);

            Result<IPage<OperateLogListItemVO>> result = controller.listOperateLogs(
                    1, 20, null, "job", "UPDATE", "jobs", 123L, null, null);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(operateLogQueryService).pageOperateLogs(
                    1, 20, null, "job", "UPDATE", "jobs", 123L, null, null);
        }
    }

    @Nested
    class GetOperateLogDetail {

        @Test
        void getOperateLogDetail_success_withChanges() {
            OperateLogDetailVO detail = new OperateLogDetailVO(
                    1L, "trace-1", 100L, "hr_user", "job", "UPDATE", "jobs", 123L,
                    List.of(new OperateLogChangeVO("title", "Java 后端", "Java 高级后端")),
                    OffsetDateTime.now());
            when(operateLogQueryService.getOperateLogDetail(1L)).thenReturn(detail);

            Result<OperateLogDetailVO> result = controller.getOperateLogDetail(1L);

            assertEquals(1, result.getData().changes().size());
            assertEquals("title", result.getData().changes().getFirst().field());
        }

        @Test
        void getOperateLogDetail_notFound_fail_serviceThrows() {
            when(operateLogQueryService.getOperateLogDetail(99L)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "审计日志不存在"));

            assertThrows(BusinessException.class, () -> controller.getOperateLogDetail(99L));
        }
    }

    @Nested
    class ListByTraceId {

        @Test
        void listByTraceId_success() {
            List<OperateLogDetailVO> details = List.of(new OperateLogDetailVO(
                    1L, "trace-1", 100L, "hr_user", "job", "UPDATE", "jobs", 123L,
                    List.of(), OffsetDateTime.now()));
            when(operateLogQueryService.listByTraceId("trace-1")).thenReturn(details);

            Result<List<OperateLogDetailVO>> result = controller.listByTraceId("trace-1");

            assertEquals(1, result.getData().size());
            verify(operateLogQueryService).listByTraceId("trace-1");
        }

        @Test
        void listByTraceId_blankTraceId_fail_serviceThrows() {
            when(operateLogQueryService.listByTraceId(" ")).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "traceId 不能为空"));

            assertThrows(BusinessException.class, () -> controller.listByTraceId(" "));
        }
    }
}
