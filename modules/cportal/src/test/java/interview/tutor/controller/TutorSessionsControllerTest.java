package interview.tutor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.tutor.model.enums.TutorMessageRole;
import interview.tutor.model.req.TutorSessionCreateReq;
import interview.tutor.model.vo.TutorMessageListItemVO;
import interview.tutor.model.vo.TutorMessagePageVO;
import interview.tutor.model.vo.TutorSessionCreateVO;
import interview.tutor.model.vo.TutorSessionListItemVO;
import interview.tutor.service.AiTutorSessionService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorSessionsControllerTest {

    @Mock
    private AiTutorSessionService aiTutorSessionService;

    private TutorSessionsController controller;

    @BeforeEach
    void setUp() {
        controller = new TutorSessionsController(aiTutorSessionService);
    }

    @Nested
    class CreateSession {

        @Test
        void createSession_success() {
            TutorSessionCreateReq req = new TutorSessionCreateReq();
            req.setAssociatedReportId(1L);
            req.setSessionTitle("Java 基础复盘答疑");
            when(aiTutorSessionService.createSession(req)).thenReturn(
                    new TutorSessionCreateVO(10L, "Java 基础复盘答疑", 1L,
                            OffsetDateTime.now()));

            Result<TutorSessionCreateVO> result = controller.createSession(req);

            assertNotNull(result);
            assertEquals(10L, result.getData().id());
            verify(aiTutorSessionService).createSession(req);
        }

        @Test
        void createSession_reportNotCompleted_fail_serviceThrows() {
            TutorSessionCreateReq req = new TutorSessionCreateReq();
            req.setAssociatedReportId(1L);
            when(aiTutorSessionService.createSession(req)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "仅已完成生成的面评报告可发起答疑"));

            assertThrows(BusinessException.class, () -> controller.createSession(req));
        }
    }

    @Nested
    class ListMySessions {

        @Test
        void listMySessions_success() {
            Page<TutorSessionListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(new TutorSessionListItemVO(
                    10L, "Java 基础复盘答疑", 1L, 3L, OffsetDateTime.now())));
            when(aiTutorSessionService.pageMySessions(1, 20)).thenReturn(page);

            Result<IPage<TutorSessionListItemVO>> result =
                    controller.listMySessions(1, 20);

            assertNotNull(result);
            assertEquals(1, result.getData().getTotal());
        }
    }

    @Nested
    class ListMessages {

        @Test
        void listMessages_success_withCursorAndHasMore() {
            TutorMessagePageVO pageVO = new TutorMessagePageVO(
                    102L, true,
                    List.of(new TutorMessageListItemVO(
                            101L, TutorMessageRole.USER, "什么是幂等性？",
                            OffsetDateTime.now())));
            when(aiTutorSessionService.listMessages(10L, 100L, 50)).thenReturn(pageVO);

            Result<TutorMessagePageVO> result = controller.listMessages(10L, 100L, 50);

            assertNotNull(result);
            assertTrue(result.getData().hasMore());
            assertEquals(102L, result.getData().nextCursor());
            verify(aiTutorSessionService).listMessages(10L, 100L, 50);
        }

        @Test
        void listMessages_notMySession_fail_serviceThrows() {
            when(aiTutorSessionService.listMessages(99L, null, 50)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "答疑会话不存在"));

            assertThrows(BusinessException.class,
                    () -> controller.listMessages(99L, null, 50));
        }
    }
}
