package interview.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.notification.model.req.NotificationBatchReadReq;
import interview.notification.model.req.NotificationReadReq;
import interview.notification.model.vo.NotificationBatchReadVO;
import interview.notification.model.vo.NotificationListItemVO;
import interview.notification.model.vo.NotificationReadVO;
import interview.notification.model.vo.NotificationUnreadCountVO;
import interview.notification.service.NotificationService;
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
class NotificationsControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationsController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationsController(notificationService);
    }

    @Nested
    class ListMyNotifications {

        @Test
        void listMyNotifications_success() {
            Page<NotificationListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new NotificationListItemVO(
                    1L, "INTERVIEW", "面试邀请", "正文", false, OffsetDateTime.now())));
            when(notificationService.pageMyNotifications(1, 20, "INTERVIEW", false))
                    .thenReturn(page);

            Result<IPage<NotificationListItemVO>> result =
                    controller.listMyNotifications(1, 20, "INTERVIEW", false);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
            verify(notificationService).pageMyNotifications(1, 20, "INTERVIEW", false);
        }

        @Test
        void listMyNotifications_invalidNotifyType_fail_serviceThrows() {
            when(notificationService.pageMyNotifications(1, 20, "INVALID", null))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR, "不支持的通知类型"));

            assertThrows(BusinessException.class,
                    () -> controller.listMyNotifications(1, 20, "INVALID", null));
        }
    }

    @Nested
    class GetUnreadCount {

        @Test
        void getUnreadCount_success() {
            when(notificationService.getUnreadCount()).thenReturn(new NotificationUnreadCountVO(3L));

            Result<NotificationUnreadCountVO> result = controller.getUnreadCount();

            assertEquals(3L, result.getData().unreadCount());
        }
    }

    @Nested
    class MarkRead {

        @Test
        void markRead_success() {
            NotificationReadReq req = new NotificationReadReq();
            req.setIsRead(true);
            when(notificationService.markRead(1L, true))
                    .thenReturn(new NotificationReadVO(1L, true, OffsetDateTime.now()));

            Result<NotificationReadVO> result = controller.markRead(1L, req);

            assertNotNull(result);
            assertEquals(1L, result.getData().id());
            verify(notificationService).markRead(1L, true);
        }

        @Test
        void markRead_notMine_fail_serviceThrows() {
            NotificationReadReq req = new NotificationReadReq();
            req.setIsRead(true);
            when(notificationService.markRead(99L, true))
                    .thenThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.markRead(99L, req));
        }
    }

    @Nested
    class MarkAllRead {

        @Test
        void markAllRead_success() {
            NotificationBatchReadReq req = new NotificationBatchReadReq();
            req.setMarkAllRead(true);
            req.setNotifyType("INTERVIEW");
            when(notificationService.markAllRead(req)).thenReturn(new NotificationBatchReadVO(5));

            Result<NotificationBatchReadVO> result = controller.markAllRead(req);

            assertEquals(5, result.getData().updatedCount());
            verify(notificationService).markAllRead(req);
        }
    }
}
