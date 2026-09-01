package interview.system.tenant.controller;

import interview.common.constant.Result;
import interview.common.constant.SecureActionContext;
import interview.system.auth.model.vo.SecureChallengeStartVO;
import interview.system.tenant.EnterpriseConverter;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactEmailUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.*;
import interview.system.tenant.service.EnterpriseContactVerificationService;
import interview.system.tenant.service.EnterprisesService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterprisesControllerTest {

    @Mock
    private EnterprisesService enterprisesService;
    @Mock
    private EnterpriseContactVerificationService contactVerificationService;
    @Mock
    private EnterpriseConverter convert;
    @Mock
    private HttpServletRequest request;

    private EnterprisesController controller;

    @BeforeEach
    void setUp() {
        controller = new EnterprisesController(
                enterprisesService, contactVerificationService, convert, request);
    }

    @Nested
    class CreateEnterprise {

        @Test
        void createEnterprise_success() {
            EnterpriseCreateReq req = new EnterpriseCreateReq();
            req.setName("TestCorp");
            req.setShortName("TC");
            req.setIndustry("IT");
            req.setContactEmail("hr@test.com");
            req.setContactPhone("13800138000");

            EnterpriseCreateBO bo = EnterpriseCreateBO.builder()
                    .id(1L).name("TestCorp").shortName("TC")
                    .status(EnterpriseStatus.PENDING).build();
            EnterpriseCreateVO vo = EnterpriseCreateVO.builder()
                    .id(1L).name("TestCorp").shortName("TC")
                    .status(EnterpriseStatus.PENDING).build();

            when(enterprisesService.createEnterprise(req)).thenReturn(bo);
            when(convert.convertToEnterpriseCreateVO(bo)).thenReturn(vo);

            Result<EnterpriseCreateVO> result = controller.createEnterprise(req);

            assertEquals(1L, result.getData().id());
            assertEquals("TestCorp", result.getData().name());
            verify(enterprisesService).createEnterprise(req);
            verify(convert).convertToEnterpriseCreateVO(bo);
        }
    }

    @Nested
    class GetUserListEnterprises {

        @Test
        void getUserListEnterprises_success() {
            ListUserEnterprisesBO bo = ListUserEnterprisesBO.builder()
                    .enterpriseId(1L).name("Corp").shortName("C")
                    .status(EnterpriseStatus.NORMAL).roleCode("ENTERPRISE_OWNER").memberCount(5L).build();
            when(enterprisesService.listUserEnterprises()).thenReturn(List.of(bo));
            EnterpriseListItemVO vo = EnterpriseListItemVO.builder()
                    .id(1L).name("Corp").shortName("C")
                    .status(EnterpriseStatus.NORMAL).roleCode("ENTERPRISE_OWNER").memberCount(5L).build();
            when(convert.BOCovertToEnterpriseListItemVO(List.of(bo))).thenReturn(List.of(vo));

            Result<List<EnterpriseListItemVO>> result = controller.getUserListEnterprises();

            assertEquals(1, result.getData().size());
            assertEquals("ENTERPRISE_OWNER", result.getData().get(0).roleCode());
            verify(enterprisesService).listUserEnterprises();
            verify(convert).BOCovertToEnterpriseListItemVO(List.of(bo));
        }

        @Test
        void getUserListEnterprises_empty() {
            when(enterprisesService.listUserEnterprises()).thenReturn(List.of());
            when(convert.BOCovertToEnterpriseListItemVO(List.of())).thenReturn(List.of());

            Result<List<EnterpriseListItemVO>> result = controller.getUserListEnterprises();

            assertTrue(result.getData().isEmpty());
        }
    }

    @Nested
    class GetEnterprisesDetail {

        @Test
        void getEnterprisesDetail_success() {
            EnterpriseDetailVO vo = EnterpriseDetailVO.builder()
                    .id(1L).name("Corp").status(EnterpriseStatus.NORMAL).build();
            when(enterprisesService.getEnterpriseDetail(1L)).thenReturn(vo);

            Result<EnterpriseDetailVO> result = controller.getEnterprisesDetail(1L);

            assertEquals("Corp", result.getData().name());
            verify(enterprisesService).getEnterpriseDetail(1L);
        }
    }

    @Nested
    class UpdateEnterpriseBasic {

        @Test
        void updateEnterpriseBasic_success() {
            EnterpriseBasicUpdateReq req = new EnterpriseBasicUpdateReq();
            req.setName("NewName");
            EnterpriseUpdateVO vo = new EnterpriseUpdateVO(1L, "NewName", "NS", "IT");
            when(enterprisesService.updateEnterpriseBasic(1L, req)).thenReturn(vo);

            Result<EnterpriseUpdateVO> result = controller.updateEnterpriseBasic(1L, req);

            assertEquals("NewName", result.getData().name());
            verify(enterprisesService).updateEnterpriseBasic(1L, req);
        }
    }

    @Nested
    class UpdateEnterpriseContact {

        @Test
        void updateEnterpriseContactEmail_success() {
            EnterpriseContactEmailUpdateReq req = new EnterpriseContactEmailUpdateReq();
            req.setContactEmail("new@test.com");
            SecureActionContext ctx = SecureActionContext.builder().build();
            when(request.getAttribute(SecureActionContext.REQUEST_ATTRIBUTE)).thenReturn(ctx);
            EnterpriseContactEmailUpdateVO vo = new EnterpriseContactEmailUpdateVO(1L, "new@test.com");
            when(enterprisesService.updateEnterpriseContactEmail(1L, ctx, req)).thenReturn(vo);

            Result<EnterpriseContactEmailUpdateVO> result =
                    controller.updateEnterpriseContactEmail(1L, req);

            assertEquals("new@test.com", result.getData().contactEmail());
            verify(enterprisesService).updateEnterpriseContactEmail(1L, ctx, req);
            verifyNoInteractions(contactVerificationService);
        }

        @Test
        void updateEnterpriseContactPhone_success() {
            SecureActionContext ctx = SecureActionContext.builder()
                    .challengeId("flow-1")
                    .build();
            when(request.getAttribute(SecureActionContext.REQUEST_ATTRIBUTE)).thenReturn(ctx);
            EnterpriseContactPhoneUpdateVO vo =
                    new EnterpriseContactPhoneUpdateVO(1L, "13900139000");
            when(contactVerificationService.completePhoneUpdate(1L, ctx)).thenReturn(vo);

            Result<EnterpriseContactPhoneUpdateVO> result =
                    controller.updateEnterpriseContactPhone(1L);

            assertEquals("13900139000", result.getData().contactPhone());
            verify(contactVerificationService).completePhoneUpdate(1L, ctx);
            verifyNoInteractions(enterprisesService);
        }

        @Test
        void startContactEmailChallenge_success() {
            SecureChallengeStartVO vo = new SecureChallengeStartVO(
                    "challenge-1", "138****8000", 300);
            when(enterprisesService.startContactEmailChallenge(1L)).thenReturn(vo);

            Result<SecureChallengeStartVO> result =
                    controller.startContactEmailChallenge(1L);

            assertEquals("challenge-1", result.getData().challengeId());
            verify(enterprisesService).startContactEmailChallenge(1L);
        }

        @Test
        void startDeletionChallenge_success() {
            SecureChallengeStartVO vo = new SecureChallengeStartVO(
                    "challenge-2", "138****8000", 300);
            when(enterprisesService.startDeletionChallenge(1L)).thenReturn(vo);

            Result<SecureChallengeStartVO> result =
                    controller.startDeletionChallenge(1L);

            assertEquals("challenge-2", result.getData().challengeId());
            verify(enterprisesService).startDeletionChallenge(1L);
        }
    }
}
