package interview.rag.controller;

import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.rag.model.req.TemplateKnowledgeBindReq;
import interview.rag.model.vo.TemplateKnowledgeBindVO;
import interview.rag.service.RagSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewTemplateKnowledgeControllerTest {

    @Mock
    private RagSessionService ragSessionService;

    private InterviewTemplateKnowledgeController controller;

    @BeforeEach
    void setUp() {
        controller = new InterviewTemplateKnowledgeController(ragSessionService);
    }

    @Nested
    class BindKnowledgeBases {

        @Test
        void bindKnowledgeBases_success() {
            TemplateKnowledgeBindReq req = new TemplateKnowledgeBindReq();
            req.setKnowledgeBaseIds(List.of(1001L, 1002L));
            req.setExpectedTemplateVersion(0);
            TemplateKnowledgeBindVO vo =
                    new TemplateKnowledgeBindVO(1L, List.of(1001L, 1002L), 0);
            when(ragSessionService.bindTemplateKnowledgeBases(10L, 1L, req)).thenReturn(vo);

            Result<TemplateKnowledgeBindVO> result =
                    controller.bindKnowledgeBases(10L, 1L, req);

            assertEquals(1L, result.getData().templateId());
            assertEquals(List.of(1001L, 1002L), result.getData().knowledgeBaseIds());
            verify(ragSessionService).bindTemplateKnowledgeBases(10L, 1L, req);
        }

        @Test
        void bindKnowledgeBases_fail_versionConflict() {
            TemplateKnowledgeBindReq req = new TemplateKnowledgeBindReq();
            req.setKnowledgeBaseIds(List.of(1001L));
            req.setExpectedTemplateVersion(0);
            when(ragSessionService.bindTemplateKnowledgeBases(10L, 1L, req))
                    .thenThrow(new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_VERSION_CONFLICT));

            assertThrows(BusinessException.class,
                    () -> controller.bindKnowledgeBases(10L, 1L, req));
        }

        @Test
        void bindKnowledgeBases_fail_templateNotOwned() {
            TemplateKnowledgeBindReq req = new TemplateKnowledgeBindReq();
            req.setKnowledgeBaseIds(List.of(1001L));
            req.setExpectedTemplateVersion(0);
            when(ragSessionService.bindTemplateKnowledgeBases(10L, 1L, req))
                    .thenThrow(new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.bindKnowledgeBases(10L, 1L, req));
        }
    }
}
