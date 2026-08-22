package interview.data.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.model.vo.SparkTaskDetailVO;
import interview.data.model.vo.SparkTaskListItemVO;
import interview.data.service.SparkTaskQueryService;
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
class SparkTasksControllerTest {

    @Mock
    private SparkTaskQueryService sparkTaskQueryService;

    private SparkTasksController controller;

    @BeforeEach
    void setUp() {
        controller = new SparkTasksController(sparkTaskQueryService);
    }

    @Nested
    class ListTasks {

        @Test
        void listTasks_success() {
            Page<SparkTaskListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new SparkTaskListItemVO(
                    1L, "SPARK-20260822-01", "SUCCESS", 1000, 900, 8500,
                    OffsetDateTime.now())));
            when(sparkTaskQueryService.pageTasks(1, 20, "SUCCESS")).thenReturn(page);

            Result<IPage<SparkTaskListItemVO>> result = controller.listTasks(1, 20, "SUCCESS");

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(sparkTaskQueryService).pageTasks(1, 20, "SUCCESS");
        }

        @Test
        void listTasks_invalidStatus_fail_serviceThrows() {
            when(sparkTaskQueryService.pageTasks(1, 20, "INVALID"))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的任务状态"));

            assertThrows(BusinessException.class, () -> controller.listTasks(1, 20, "INVALID"));
        }
    }

    @Nested
    class GetTaskDetail {

        @Test
        void getTaskDetail_success() {
            SparkTaskDetailVO detail = new SparkTaskDetailVO(
                    1L, "SPARK-20260822-01", "s3://corpus/2026/08", "SUCCESS",
                    1000, 900, 8500, 120, 300, OffsetDateTime.now());
            when(sparkTaskQueryService.getTaskDetail(1L)).thenReturn(detail);

            Result<SparkTaskDetailVO> result = controller.getTaskDetail(1L);

            assertEquals("SPARK-20260822-01", result.getData().taskNo());
        }

        @Test
        void getTaskDetail_notFound_fail_serviceThrows() {
            when(sparkTaskQueryService.getTaskDetail(99L))
                    .thenThrow(new BusinessException(ErrorCode.SPARK_TASK_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.getTaskDetail(99L));
        }
    }
}
