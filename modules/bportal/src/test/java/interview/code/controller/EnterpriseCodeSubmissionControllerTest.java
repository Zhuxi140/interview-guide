package interview.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.code.model.enums.CodeExecutionStatus;
import interview.code.model.req.EnterpriseCodeSubmissionSearchReq;
import interview.code.model.vo.EnterpriseCodeSubmissionDetailVO;
import interview.code.model.vo.EnterpriseCodeSubmissionListItemVO;
import interview.code.service.CodeSubmissionQueryService;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
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
class EnterpriseCodeSubmissionControllerTest {

    private static final Long ENTERPRISE_ID = 100L;

    @Mock
    private CodeSubmissionQueryService codeSubmissionQueryService;

    private EnterpriseCodeSubmissionController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterpriseCodeSubmissionController(codeSubmissionQueryService);
    }

    @Nested
    class PageCandidateSubmissions {

        @Test
        void pageCandidateSubmissions_success() {
            EnterpriseCodeSubmissionSearchReq req = new EnterpriseCodeSubmissionSearchReq();
            Page<EnterpriseCodeSubmissionListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(EnterpriseCodeSubmissionListItemVO.builder()
                    .submissionId(9L).sessionId(500L).candidateId(7L)
                    .questionId(1L).questionTitle("两数之和")
                    .executionStatus(CodeExecutionStatus.FAIL)
                    .passedCount(8L).totalCount(10L).attemptNumber(2L).build()));
            when(codeSubmissionQueryService.pageEnterpriseCandidateSubmissions(
                    ENTERPRISE_ID, 7L, req)).thenReturn(page);

            Result<IPage<EnterpriseCodeSubmissionListItemVO>> result =
                    controller.pageCandidateSubmissions(ENTERPRISE_ID, 7L, req);

            assertEquals(1, result.getData().getRecords().size());
            assertEquals(7L, result.getData().getRecords().getFirst().candidateId());
        }

        @Test
        void pageCandidateSubmissions_fail_notEnterpriseMember() {
            EnterpriseCodeSubmissionSearchReq req = new EnterpriseCodeSubmissionSearchReq();
            when(codeSubmissionQueryService.pageEnterpriseCandidateSubmissions(
                    ENTERPRISE_ID, 7L, req))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG));

            assertThrows(BusinessException.class,
                    () -> controller.pageCandidateSubmissions(ENTERPRISE_ID, 7L, req));
        }
    }

    @Nested
    class GetSubmissionDetail {

        @Test
        void getSubmissionDetail_success() {
            EnterpriseCodeSubmissionDetailVO vo = EnterpriseCodeSubmissionDetailVO.builder()
                    .submissionId(9L).sessionId(500L).candidateId(7L)
                    .candidateName("张三").questionTitle("两数之和")
                    .submittedCode("class Solution {}").build();
            when(codeSubmissionQueryService.getEnterpriseSubmissionDetail(ENTERPRISE_ID, 9L))
                    .thenReturn(vo);

            Result<EnterpriseCodeSubmissionDetailVO> result =
                    controller.getSubmissionDetail(ENTERPRISE_ID, 9L);

            assertEquals("张三", result.getData().candidateName());
            verify(codeSubmissionQueryService)
                    .getEnterpriseSubmissionDetail(ENTERPRISE_ID, 9L);
        }

        @Test
        void getSubmissionDetail_fail_crossTenantProbe() {
            // 提交冗余的 enterprise_id 与路径企业不一致时按不存在处理，防止跨企业探测。
            when(codeSubmissionQueryService.getEnterpriseSubmissionDetail(ENTERPRISE_ID, 9L))
                    .thenThrow(new BusinessException(ErrorCode.CODE_SUBMISSION_NOT_FOUND));

            assertThrows(BusinessException.class,
                    () -> controller.getSubmissionDetail(ENTERPRISE_ID, 9L));
        }
    }
}
