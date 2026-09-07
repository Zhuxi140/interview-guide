package interview.notification.controller;

import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.notification.model.req.NotificationChannelsUpdateReq;
import interview.notification.model.vo.NotificationChannelVO;
import interview.notification.model.vo.NotificationChannelsVO;
import interview.notification.service.NotificationChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationChannelsControllerTest {

    @Mock
    private NotificationChannelService notificationChannelService;

    private NotificationChannelsController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationChannelsController(notificationChannelService);
    }

    @Nested
    class GetChannels {

        @Test
        void getChannels_success() {
            NotificationChannelsVO vo = new NotificationChannelsVO(List.of(
                    new NotificationChannelVO("IN_APP", true, null, null, 0),
                    new NotificationChannelVO("EMAIL", false, "SMTP", "{\"host\":\"smtp.example.com\",\"password\":\"******\"}", 1)
            ), 1);
            when(notificationChannelService.getChannels()).thenReturn(vo);

            Result<NotificationChannelsVO> result = controller.getChannels();

            assertNotNull(result);
            assertEquals(2, result.getData().channels().size());
            assertEquals(1, result.getData().version());
        }
    }

    @Nested
    class UpdateChannels {

        @Test
        void updateChannels_success() {
            NotificationChannelsUpdateReq req = new NotificationChannelsUpdateReq();
            req.setExpectedVersion(1);
            NotificationChannelsVO vo = new NotificationChannelsVO(List.of(), 2);
            when(notificationChannelService.updateChannels(req)).thenReturn(vo);

            Result<NotificationChannelsVO> result = controller.updateChannels(req);

            assertEquals(2, result.getData().version());
            verify(notificationChannelService).updateChannels(req);
        }

        @Test
        void updateChannels_versionConflict_fail_serviceThrows() {
            NotificationChannelsUpdateReq req = new NotificationChannelsUpdateReq();
            req.setExpectedVersion(9);
            when(notificationChannelService.updateChannels(req))
                    .thenThrow(new BusinessException(
                            ErrorCode.PARAM_VALID_ERROR, "渠道配置已被修改，请刷新后重试"));

            assertThrows(BusinessException.class, () -> controller.updateChannels(req));
        }
    }
}
