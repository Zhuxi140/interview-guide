package interview.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.knowledge.model.enums.KnowledgeVisibility;
import interview.knowledge.model.enums.VectorStatus;
import interview.knowledge.model.req.EnterpriseKnowledgeBaseSearchReq;
import interview.knowledge.model.vo.KnowledgeBaseDetailVO;
import interview.knowledge.model.vo.KnowledgeBaseListItemVO;
import interview.knowledge.service.KnowledgeBaseService;
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
class EnterpriseKnowledgeBaseControllerTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private EnterpriseKnowledgeBaseController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseKnowledgeBaseController(knowledgeBaseService);
    }

    @Nested
    class PageKnowledgeBases {

        @Test
        void pageKnowledgeBases_success() {
            EnterpriseKnowledgeBaseSearchReq req = new EnterpriseKnowledgeBaseSearchReq();
            Page<KnowledgeBaseListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(KnowledgeBaseListItemVO.builder()
                    .id(1L).name("Java 岗位知识库").visibility(KnowledgeVisibility.PRIVATE)
                    .vectorStatus(VectorStatus.COMPLETED).chunkCount(128)
                    .uploadedAt(OffsetDateTime.now()).build()));
            when(knowledgeBaseService.pageEnterpriseKnowledgeBases(10L, req)).thenReturn(page);

            Result<IPage<KnowledgeBaseListItemVO>> result =
                    controller.pageKnowledgeBases(10L, req);

            assertEquals(1, result.getData().getTotal());
            assertEquals(1, result.getData().getRecords().size());
            assertEquals(KnowledgeVisibility.PRIVATE,
                    result.getData().getRecords().getFirst().visibility());
        }
    }

    @Nested
    class GetKnowledgeBase {

        @Test
        void getKnowledgeBase_success() {
            KnowledgeBaseDetailVO vo = KnowledgeBaseDetailVO.builder()
                    .id(1L).name("Java 岗位知识库")
                    .visibility(KnowledgeVisibility.PRIVATE)
                    .vectorStatus(VectorStatus.COMPLETED)
                    .chunkCount(128).version(0)
                    .uploadedAt(OffsetDateTime.now()).build();
            when(knowledgeBaseService.getEnterpriseKnowledgeBaseDetail(10L, 1L)).thenReturn(vo);

            Result<KnowledgeBaseDetailVO> result = controller.getKnowledgeBase(10L, 1L);

            assertEquals(128, result.getData().chunkCount());
            assertEquals("Java 岗位知识库", result.getData().name());
        }

        @Test
        void getKnowledgeBase_fail_notFound() {
            when(knowledgeBaseService.getEnterpriseKnowledgeBaseDetail(10L, 1L))
                    .thenThrow(new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.getKnowledgeBase(10L, 1L));
        }
    }

    @Nested
    class DeleteKnowledgeBase {

        @Test
        void deleteKnowledgeBase_success() {
            Result<Void> result = controller.deleteKnowledgeBase(10L, 1L, 0);

            assertNotNull(result);
            verify(knowledgeBaseService).deleteEnterpriseKnowledgeBase(10L, 1L, 0);
        }
    }
}
