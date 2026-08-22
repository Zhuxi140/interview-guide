package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
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
class EnterpriseAuditControllerTest {

    @Mock
    private OperateLogQueryService operateLogQueryService;

    private EnterpriseAuditController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseAuditController(operateLogQueryService);
    }

    @Nested
    class ListEnterpriseOperateLogs {

        @Test
        void listEnterpriseOperateLogs_success() {
            Page<OperateLogListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new OperateLogListItemVO(
                    1L, "trace-1", 100L, "hr_user", "job", "UPDATE", "jobs", 123L,
                    OffsetDateTime.now())));
            when(operateLogQueryService.pageEnterpriseOperateLogs(
                    9L, 1, 20, "job", null, null, null, null, null)).thenReturn(page);

            Result<IPage<OperateLogListItemVO>> result = controller.listEnterpriseOperateLogs(
                    9L, 1, 20, "job", null, null, null, null, null);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(operateLogQueryService).pageEnterpriseOperateLogs(
                    9L, 1, 20, "job", null, null, null, null, null);
        }

        @Test
        void listEnterpriseOperateLogs_notEnterpriseMember_fail_serviceThrows() {
            when(operateLogQueryService.pageEnterpriseOperateLogs(
                    9L, 1, 20, null, null, null, null, null, null))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG));

            assertThrows(BusinessException.class, () -> controller.listEnterpriseOperateLogs(
                    9L, 1, 20, null, null, null, null, null, null));
        }
    }

    @Nested
    class GetEnterpriseOperateLogDetail {

        @Test
        void getEnterpriseOperateLogDetail_success() {
            OperateLogDetailVO detail = new OperateLogDetailVO(
                    1L, "trace-1", 100L, "hr_user", "job", "UPDATE", "jobs", 123L,
                    List.of(), OffsetDateTime.now());
            when(operateLogQueryService.getEnterpriseOperateLogDetail(9L, 1L))
                    .thenReturn(detail);

            Result<OperateLogDetailVO> result =
                    controller.getEnterpriseOperateLogDetail(9L, 1L);

            assertEquals(100L, result.getData().userId());
            verify(operateLogQueryService).getEnterpriseOperateLogDetail(9L, 1L);
        }

        @Test
        void getEnterpriseOperateLogDetail_outOfDomain_fail_serviceThrows() {
            when(operateLogQueryService.getEnterpriseOperateLogDetail(9L, 99L))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG));

            assertThrows(BusinessException.class,
                    () -> controller.getEnterpriseOperateLogDetail(9L, 99L));
        }
    }
}
