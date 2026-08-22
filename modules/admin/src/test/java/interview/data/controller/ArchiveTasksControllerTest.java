package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.model.vo.ArchiveTaskDetailVO;
import interview.data.model.vo.ArchiveTaskListItemVO;
import interview.data.service.DataArchiveTaskQueryService;
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
class ArchiveTasksControllerTest {

    @Mock
    private DataArchiveTaskQueryService dataArchiveTaskQueryService;

    private ArchiveTasksController controller;

    @BeforeEach
    void setUp() {
        controller = new ArchiveTasksController(dataArchiveTaskQueryService);
    }

    @Nested
    class ListTasks {

        @Test
        void listTasks_success() {
            Page<ArchiveTaskListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new ArchiveTaskListItemVO(
                    1L, "API_LOG", "COMPLETED", 5000L,
                    OffsetDateTime.now(), OffsetDateTime.now().plusHours(1))));
            when(dataArchiveTaskQueryService.pageTasks(
                    1, 20, "API_LOG", "COMPLETED", null, null)).thenReturn(page);

            Result<IPage<ArchiveTaskListItemVO>> result =
                    controller.listTasks(1, 20, "API_LOG", "COMPLETED", null, null);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(dataArchiveTaskQueryService)
                    .pageTasks(1, 20, "API_LOG", "COMPLETED", null, null);
        }

        @Test
        void listTasks_invalidResourceType_fail_serviceThrows() {
            when(dataArchiveTaskQueryService.pageTasks(1, 20, "INVALID", null, null, null))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的资源类型"));

            assertThrows(BusinessException.class,
                    () -> controller.listTasks(1, 20, "INVALID", null, null, null));
        }
    }

    @Nested
    class GetTaskDetail {

        @Test
        void getTaskDetail_success() {
            ArchiveTaskDetailVO detail = new ArchiveTaskDetailVO(
                    1L, "API_LOG", OffsetDateTime.now().minusDays(30), "COMPLETED",
                    5000L, null, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
            when(dataArchiveTaskQueryService.getTaskDetail(1L)).thenReturn(detail);

            Result<ArchiveTaskDetailVO> result = controller.getTaskDetail(1L);

            assertEquals(5000L, result.getData().archivedCount());
        }

        @Test
        void getTaskDetail_notFound_fail_serviceThrows() {
            when(dataArchiveTaskQueryService.getTaskDetail(99L)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "归档任务不存在"));

            assertThrows(BusinessException.class, () -> controller.getTaskDetail(99L));
        }
    }
}
