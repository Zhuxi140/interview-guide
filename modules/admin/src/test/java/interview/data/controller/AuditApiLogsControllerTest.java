package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.model.vo.ApiLogListItemVO;
import interview.data.service.ApiLogQueryService;
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
class AuditApiLogsControllerTest {

    @Mock
    private ApiLogQueryService apiLogQueryService;

    private AuditApiLogsController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditApiLogsController(apiLogQueryService);
    }

    @Nested
    class ListApiLogs {

        @Test
        void listApiLogs_success() {
            Page<ApiLogListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new ApiLogListItemVO(
                    1L, "trace-1", 100L, "hr_user", "/api/v1/jobs", "GET",
                    "127.0.0.1", 20L, 200, null, null, null, null,
                    OffsetDateTime.now())));
            when(apiLogQueryService.pageApiLogs(1, 20, 100L, null, null, null, null, null))
                    .thenReturn(page);

            Result<IPage<ApiLogListItemVO>> result =
                    controller.listApiLogs(1, 20, 100L, null, null, null, null, null);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(apiLogQueryService).pageApiLogs(1, 20, 100L, null, null, null, null, null);
        }
    }

    @Nested
    class GetApiLogDetail {

        @Test
        void getApiLogDetail_success() {
            ApiLogListItemVO detail = new ApiLogListItemVO(
                    1L, "trace-1", 100L, "hr_user", "/api/v1/jobs", "GET",
                    "127.0.0.1", 20L, 200, null, null, null, null,
                    OffsetDateTime.now());
            when(apiLogQueryService.getApiLogDetail(1L)).thenReturn(detail);

            Result<ApiLogListItemVO> result = controller.getApiLogDetail(1L);

            assertEquals("hr_user", result.getData().username());
        }

        @Test
        void getApiLogDetail_notFound_fail_serviceThrows() {
            when(apiLogQueryService.getApiLogDetail(99L)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "API 日志不存在"));

            assertThrows(BusinessException.class, () -> controller.getApiLogDetail(99L));
        }
    }

    @Nested
    class ListApiLogArchives {

        @Test
        void listApiLogArchives_success() {
            Page<ApiLogListItemVO> page = new Page<>(1, 20, 0);
            page.setRecords(List.of());
            when(apiLogQueryService.pageApiLogArchives(
                    1, 20, null, "/api/v1/jobs", "GET", 500, null, null)).thenReturn(page);

            Result<IPage<ApiLogListItemVO>> result = controller.listApiLogArchives(
                    1, 20, null, "/api/v1/jobs", "GET", 500, null, null);

            assertNotNull(result);
            assertEquals(0, result.getData().getTotal());
        }

        @Test
        void listApiLogArchives_invalidPage_fail_serviceThrows() {
            when(apiLogQueryService.pageApiLogArchives(0, 20, null, null, null, null, null, null))
                    .thenThrow(new BusinessException(ErrorCode.PAGE_PARAM_INVALID));

            assertThrows(BusinessException.class, () ->
                    controller.listApiLogArchives(0, 20, null, null, null, null, null, null));
        }
    }
}
