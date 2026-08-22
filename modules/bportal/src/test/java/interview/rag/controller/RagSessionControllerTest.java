package interview.rag.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.rag.model.enums.RagMessageType;
import interview.rag.model.req.RagMessageCursorReq;
import interview.rag.model.req.RagSessionCreateReq;
import interview.rag.model.req.RagSessionKnowledgeBindReq;
import interview.rag.model.req.RagSessionSearchReq;
import interview.rag.model.vo.RagMessagePageVO;
import interview.rag.model.vo.RagMessageVO;
import interview.rag.model.vo.RagSessionCreateVO;
import interview.rag.model.vo.RagSessionKnowledgeBindVO;
import interview.rag.model.vo.RagSessionListItemVO;
import interview.rag.service.RagSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagSessionControllerTest {

    @Mock
    private RagSessionService ragSessionService;

    private RagSessionController controller;

    @BeforeEach
    void setUp() {
        controller = new RagSessionController(ragSessionService);
    }

    @Nested
    class CreateSession {

        @Test
        void createSession_success() {
            RagSessionCreateReq req = new RagSessionCreateReq();
            req.setTitle("Java 后端岗位知识问答");
            RagSessionCreateVO vo = RagSessionCreateVO.builder()
                    .id(1L).enterpriseId(10L).title("Java 后端岗位知识问答")
                    .createdAt(OffsetDateTime.now()).build();
            when(ragSessionService.createSession(10L, req)).thenReturn(vo);

            Result<RagSessionCreateVO> result = controller.createSession(10L, req);

            assertEquals(10L, result.getData().enterpriseId());
            verify(ragSessionService).createSession(10L, req);
        }
    }

    @Nested
    class PageSessions {

        @Test
        void pageSessions_success() {
            RagSessionSearchReq req = new RagSessionSearchReq();
            Page<RagSessionListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(RagSessionListItemVO.builder()
                    .id(1L).title("Java 后端岗位知识问答")
                    .messageCount(6L).createdAt(OffsetDateTime.now()).build()));
            when(ragSessionService.pageMySessions(10L, req)).thenReturn(page);

            Result<IPage<RagSessionListItemVO>> result = controller.pageSessions(10L, req);

            assertEquals(1, result.getData().getTotal());
            assertEquals(6L, result.getData().getRecords().getFirst().messageCount());
        }
    }

    @Nested
    class BindKnowledgeBases {

        @Test
        void bindKnowledgeBases_success() {
            RagSessionKnowledgeBindReq req = new RagSessionKnowledgeBindReq();
            req.setKnowledgeBaseIds(List.of(1001L, 1002L));
            RagSessionKnowledgeBindVO vo =
                    new RagSessionKnowledgeBindVO(1L, List.of(1001L, 1002L));
            when(ragSessionService.bindSessionKnowledgeBases(10L, 1L, req)).thenReturn(vo);

            Result<RagSessionKnowledgeBindVO> result =
                    controller.bindKnowledgeBases(10L, 1L, req);

            assertEquals(List.of(1001L, 1002L), result.getData().knowledgeBaseIds());
        }

        @Test
        void bindKnowledgeBases_fail_sessionNotOwned() {
            RagSessionKnowledgeBindReq req = new RagSessionKnowledgeBindReq();
            req.setKnowledgeBaseIds(List.of(1001L));
            when(ragSessionService.bindSessionKnowledgeBases(10L, 1L, req))
                    .thenThrow(new BusinessException(ErrorCode.RAG_SESSION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.bindKnowledgeBases(10L, 1L, req));
        }
    }

    @Nested
    class ListMessages {

        @Test
        void listMessages_success_withCursor() {
            RagMessageCursorReq req = new RagMessageCursorReq();
            req.setCursor(100L);
            req.setSize(50);
            RagMessageVO message = new RagMessageVO(
                    101L, RagMessageType.USER, "公司的主要技术栈是什么？",
                    OffsetDateTime.now());
            RagMessagePageVO pageVO = new RagMessagePageVO(101L, false, List.of(message));
            when(ragSessionService.listMessages(10L, 1L, req)).thenReturn(pageVO);

            Result<RagMessagePageVO> result = controller.listMessages(10L, 1L, req);

            assertFalse(result.getData().hasMore());
            assertEquals(101L, result.getData().nextCursor());
            assertEquals(RagMessageType.USER, result.getData().records().getFirst().type());
        }
    }
}
