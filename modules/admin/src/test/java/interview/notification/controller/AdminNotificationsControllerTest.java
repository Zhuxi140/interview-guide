package interview.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.notification.model.vo.NotificationSendRecordListItemVO;
import interview.notification.service.NotificationSendRecordService;
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
class AdminNotificationsControllerTest {

    @Mock
    private NotificationSendRecordService notificationSendRecordService;

    private AdminNotificationsController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminNotificationsController(notificationSendRecordService);
    }

    @Nested
    class ListSendRecords {

        @Test
        void listSendRecords_success() {
            Page<NotificationSendRecordListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new NotificationSendRecordListItemVO(
                    1L, 100L, "INTERVIEW_INVITE", "EMAIL", "SENT", null,
                    OffsetDateTime.now())));
            when(notificationSendRecordService.pageRecords(
                    1, 20, "EMAIL", "FAILED", null, null)).thenReturn(page);

            Result<IPage<NotificationSendRecordListItemVO>> result =
                    controller.listSendRecords(1, 20, "EMAIL", "FAILED", null, null);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(notificationSendRecordService)
                    .pageRecords(1, 20, "EMAIL", "FAILED", null, null);
        }

        @Test
        void listSendRecords_invalidTimeRange_fail_serviceThrows() {
            when(notificationSendRecordService.pageRecords(
                    1, 20, null, null, "2026-08-31T00:00:00+08:00", "2026-08-01T00:00:00+08:00"))
                    .thenThrow(new BusinessException(ErrorCode.TIME_RANGE_INVALID));

            assertThrows(BusinessException.class, () -> controller.listSendRecords(
                    1, 20, null, null, "2026-08-31T00:00:00+08:00", "2026-08-01T00:00:00+08:00"));
        }
    }
}
