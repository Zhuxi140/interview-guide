package interview.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.notification.model.req.NotificationTemplateCreateReq;
import interview.notification.model.req.NotificationTemplateUpdateReq;
import interview.notification.model.vo.NotificationTemplateCreateVO;
import interview.notification.model.vo.NotificationTemplateListItemVO;
import interview.notification.model.vo.NotificationTemplateUpdateVO;
import interview.notification.service.NotificationTemplateService;
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
class NotificationTemplatesControllerTest {

    @Mock
    private NotificationTemplateService notificationTemplateService;

    private NotificationTemplatesController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationTemplatesController(notificationTemplateService);
    }

    @Nested
    class ListTemplates {

        @Test
        void listTemplates_success() {
            Page<NotificationTemplateListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new NotificationTemplateListItemVO(
                    1L, "INTERVIEW_INVITE", "EMAIL", "面试邀请",
                    "${candidateName} 您好", true, 0, OffsetDateTime.now())));
            when(notificationTemplateService.pageTemplates(1, 20, "INTERVIEW_INVITE", "EMAIL"))
                    .thenReturn(page);

            Result<IPage<NotificationTemplateListItemVO>> result =
                    controller.listTemplates(1, 20, "INTERVIEW_INVITE", "EMAIL");

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
        }
    }

    @Nested
    class CreateTemplate {

        @Test
        void createTemplate_success() {
            NotificationTemplateCreateReq req = new NotificationTemplateCreateReq();
            req.setNotifyScene("INTERVIEW_INVITE");
            req.setChannelType("EMAIL");
            req.setTitle("面试邀请");
            req.setContentTemplate("${candidateName} 您好");
            when(notificationTemplateService.createTemplate(req)).thenReturn(
                    new NotificationTemplateCreateVO(1L, "INTERVIEW_INVITE", "EMAIL", 0,
                            OffsetDateTime.now()));

            Result<NotificationTemplateCreateVO> result = controller.createTemplate(req);

            assertNotNull(result);
            assertEquals(0, result.getData().version());
            verify(notificationTemplateService).createTemplate(req);
        }

        @Test
        void createTemplate_duplicate_fail_serviceThrows() {
            NotificationTemplateCreateReq req = new NotificationTemplateCreateReq();
            req.setNotifyScene("INTERVIEW_INVITE");
            req.setChannelType("EMAIL");
            req.setTitle("面试邀请");
            req.setContentTemplate("${candidateName} 您好");
            when(notificationTemplateService.createTemplate(req)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "同场景同渠道的模板已存在"));

            assertThrows(BusinessException.class, () -> controller.createTemplate(req));
        }
    }

    @Nested
    class UpdateTemplate {

        @Test
        void updateTemplate_success() {
            NotificationTemplateUpdateReq req = new NotificationTemplateUpdateReq();
            req.setExpectedVersion(0);
            req.setEnabled(true);
            when(notificationTemplateService.updateTemplate(1L, req)).thenReturn(
                    new NotificationTemplateUpdateVO(1L, 1, OffsetDateTime.now()));

            Result<NotificationTemplateUpdateVO> result = controller.updateTemplate(1L, req);

            assertEquals(1, result.getData().version());
            verify(notificationTemplateService).updateTemplate(1L, req);
        }

        @Test
        void updateTemplate_notFound_fail_serviceThrows() {
            NotificationTemplateUpdateReq req = new NotificationTemplateUpdateReq();
            req.setExpectedVersion(0);
            when(notificationTemplateService.updateTemplate(99L, req)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "通知模板不存在"));

            assertThrows(BusinessException.class, () -> controller.updateTemplate(99L, req));
        }
    }
}
