package interview.compliance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.compliance.model.enums.PolicyAction;
import interview.compliance.model.enums.PolicyType;
import interview.compliance.model.req.ApiPolicyCreateReq;
import interview.compliance.model.req.ApiPolicySearchReq;
import interview.compliance.model.req.ApiPolicyUpdateReq;
import interview.compliance.model.vo.ApiPolicyListItemVO;
import interview.compliance.model.vo.ApiPolicyMutationVO;
import interview.compliance.service.ApiPolicyService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiPoliciesControllerTest {

    @Mock
    private ApiPolicyService apiPolicyService;

    private ApiPoliciesController controller;

    @BeforeEach
    void setUp() {
        controller = new ApiPoliciesController(apiPolicyService);
    }

    @Nested
    class CreatePolicy {

        @Test
        void createPolicy_success() {
            ApiPolicyCreateReq req = new ApiPolicyCreateReq();
            req.setUserId(100L);
            req.setPolicyType(PolicyType.RATE_LIMIT);
            req.setLimitCount(60);
            req.setLimitSeconds(60);
            req.setActionType(PolicyAction.BLOCK);
            req.setReason("接口刷量监控触发");
            ApiPolicyMutationVO vo = ApiPolicyMutationVO.builder()
                    .id(1L).userId(100L)
                    .policyType(PolicyType.RATE_LIMIT)
                    .actionType(PolicyAction.BLOCK)
                    .version(0)
                    .build();
            when(apiPolicyService.createPolicy(req)).thenReturn(vo);

            Result<ApiPolicyMutationVO> result = controller.createPolicy(req);

            assertEquals(PolicyType.RATE_LIMIT, result.getData().policyType());
            verify(apiPolicyService).createPolicy(req);
        }

        @Test
        void createPolicy_rateLimitMissingWindow_fail() {
            ApiPolicyCreateReq req = new ApiPolicyCreateReq();
            req.setUserId(100L);
            req.setPolicyType(PolicyType.RATE_LIMIT);
            req.setActionType(PolicyAction.BLOCK);
            req.setReason("接口刷量监控触发");
            when(apiPolicyService.createPolicy(req))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                            "限流策略必须同时提供正整数 limitCount 与 limitSeconds"));

            assertThrows(BusinessException.class, () -> controller.createPolicy(req));
        }

        @Test
        void createPolicy_listTypeMutex_fail() {
            ApiPolicyCreateReq req = new ApiPolicyCreateReq();
            req.setUserId(100L);
            req.setPolicyType(PolicyType.WHITELIST);
            req.setActionType(PolicyAction.ALLOW);
            req.setReason("白名单放行");
            when(apiPolicyService.createPolicy(req))
                    .thenThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                            "该用户已存在互斥的黑名单策略，黑白名单不能同时生效"));

            assertThrows(BusinessException.class, () -> controller.createPolicy(req));
        }
    }

    @Nested
    class PagePolicies {

        @Test
        void pagePolicies_success() {
            ApiPolicySearchReq req = new ApiPolicySearchReq();
            req.setPage(1);
            req.setSize(20);
            req.setPolicyType(PolicyType.BLACKLIST);
            Page<ApiPolicyListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(ApiPolicyListItemVO.builder()
                    .id(1L).userId(100L)
                    .policyType(PolicyType.BLACKLIST)
                    .actionType(PolicyAction.BLOCK)
                    .reason("风控拦截").version(0).build()));
            when(apiPolicyService.pagePolicies(req)).thenReturn(page);

            Result<IPage<ApiPolicyListItemVO>> result = controller.pagePolicies(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals(PolicyType.BLACKLIST,
                    result.getData().getRecords().get(0).policyType());
        }
    }

    @Nested
    class UpdatePolicy {

        @Test
        void updatePolicy_success() {
            ApiPolicyUpdateReq req = new ApiPolicyUpdateReq();
            req.setLimitCount(30);
            req.setExpectedVersion(0);
            ApiPolicyMutationVO vo = ApiPolicyMutationVO.builder()
                    .id(1L).userId(100L)
                    .policyType(PolicyType.RATE_LIMIT)
                    .actionType(PolicyAction.BLOCK)
                    .version(1)
                    .build();
            when(apiPolicyService.updatePolicy(1L, req)).thenReturn(vo);

            Result<ApiPolicyMutationVO> result = controller.updatePolicy(1L, req);

            assertEquals(1, result.getData().version());
            verify(apiPolicyService).updatePolicy(1L, req);
        }
    }

    @Nested
    class DeletePolicy {

        @Test
        void deletePolicy_success() {
            Result<Void> result = controller.deletePolicy(1L, 2);

            assertNotNull(result);
            verify(apiPolicyService).deletePolicy(1L, 2);
        }

        @Test
        void deletePolicy_conflict_fail() {
            doThrow(new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                    "API 策略已被其他请求修改，请刷新后重试"))
                    .when(apiPolicyService).deletePolicy(1L, 2);

            assertThrows(BusinessException.class, () -> controller.deletePolicy(1L, 2));
        }
    }
}
