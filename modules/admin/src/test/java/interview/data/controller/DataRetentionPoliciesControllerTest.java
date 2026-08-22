package interview.data.controller;

import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.data.model.req.DataRetentionPoliciesUpdateReq;
import interview.data.model.req.RetentionPolicyItemReq;
import interview.data.model.vo.DataRetentionPoliciesVO;
import interview.data.model.vo.RetentionPolicyItemVO;
import interview.data.service.DataRetentionPolicyService;
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
class DataRetentionPoliciesControllerTest {

    @Mock
    private DataRetentionPolicyService dataRetentionPolicyService;

    private DataRetentionPoliciesController controller;

    @BeforeEach
    void setUp() {
        controller = new DataRetentionPoliciesController(dataRetentionPolicyService);
    }

    @Nested
    class GetPolicies {

        @Test
        void getPolicies_success() {
            DataRetentionPoliciesVO vo = new DataRetentionPoliciesVO(0, List.of(
                    new RetentionPolicyItemVO("API_LOG", 30, true, 365)),
                    OffsetDateTime.now());
            when(dataRetentionPolicyService.getPolicies()).thenReturn(vo);

            Result<DataRetentionPoliciesVO> result = controller.getPolicies();

            assertNotNull(result);
            assertEquals(1, result.getData().policies().size());
            assertEquals("API_LOG", result.getData().policies().getFirst().resourceType());
        }
    }

    @Nested
    class UpdatePolicies {

        @Test
        void updatePolicies_success() {
            DataRetentionPoliciesUpdateReq req = new DataRetentionPoliciesUpdateReq();
            req.setExpectedVersion(0);
            RetentionPolicyItemReq item = new RetentionPolicyItemReq();
            item.setResourceType("API_LOG");
            item.setHotRetentionDays(15);
            item.setArchiveEnabled(true);
            item.setColdRetentionDays(180);
            req.setPolicies(List.of(item));
            DataRetentionPoliciesVO vo = new DataRetentionPoliciesVO(1,
                    List.of(new RetentionPolicyItemVO("API_LOG", 15, true, 180)),
                    OffsetDateTime.now());
            when(dataRetentionPolicyService.updatePolicies(req)).thenReturn(vo);

            Result<DataRetentionPoliciesVO> result = controller.updatePolicies(req);

            assertEquals(1, result.getData().version());
            verify(dataRetentionPolicyService).updatePolicies(req);
        }

        @Test
        void updatePolicies_versionConflict_fail_serviceThrows() {
            DataRetentionPoliciesUpdateReq req = new DataRetentionPoliciesUpdateReq();
            req.setExpectedVersion(9);
            when(dataRetentionPolicyService.updatePolicies(req)).thenThrow(
                    new BusinessException(ErrorCode.PARAM_VALID_ERROR, "保留策略已被修改，请刷新后重试"));

            assertThrows(BusinessException.class, () -> controller.updatePolicies(req));
        }
    }
}
