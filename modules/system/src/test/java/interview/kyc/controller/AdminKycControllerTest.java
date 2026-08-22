package interview.kyc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.common.constant.Result;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.kyc.model.enums.KycAuditAction;
import interview.kyc.model.enums.KycMaterialType;
import interview.kyc.model.enums.KycStatus;
import interview.kyc.model.req.KycAuditReq;
import interview.kyc.model.req.KycAuditSearchReq;
import interview.kyc.model.vo.KycAuditVO;
import interview.kyc.model.vo.KycDetailVO;
import interview.kyc.model.vo.KycListItemVO;
import interview.kyc.model.vo.KycMaterialPresignVO;
import interview.kyc.service.KycService;
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
class AdminKycControllerTest {

    @Mock
    private KycService kycService;

    private AdminKycController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminKycController(kycService);
    }

    @Nested
    class AuditKyc {

        @Test
        void auditKyc_approve_success() {
            KycAuditReq req = new KycAuditReq();
            req.setAction(KycAuditAction.APPROVE);
            KycAuditVO vo = KycAuditVO.builder()
                    .id(1L)
                    .authStatus(KycStatus.APPROVED)
                    .build();
            when(kycService.auditKyc(1L, req)).thenReturn(vo);

            Result<KycAuditVO> result = controller.auditKyc(1L, req);

            assertEquals(KycStatus.APPROVED, result.getData().authStatus());
            verify(kycService).auditKyc(1L, req);
        }

        @Test
        void auditKyc_notFound_fail() {
            KycAuditReq req = new KycAuditReq();
            req.setAction(KycAuditAction.REJECT);
            req.setRejectReason("证件照片模糊");
            when(kycService.auditKyc(99L, req))
                    .thenThrow(new BusinessException(ErrorCode.KYC_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.auditKyc(99L, req));
        }
    }

    @Nested
    class PageKyc {

        @Test
        void pageKyc_success() {
            KycAuditSearchReq req = new KycAuditSearchReq();
            req.setPage(1);
            req.setSize(20);
            req.setAuthStatus(KycStatus.PENDING);
            Page<KycListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(KycListItemVO.builder()
                    .id(1L).userId(100L).maskedRealName("张**")
                    .authStatus(KycStatus.PENDING).build()));
            when(kycService.pageKyc(req)).thenReturn(page);

            Result<IPage<KycListItemVO>> result = controller.pageKyc(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals("张**", result.getData().getRecords().get(0).maskedRealName());
        }
    }

    @Nested
    class GetKycDetail {

        @Test
        void getKycDetail_success() {
            KycDetailVO vo = KycDetailVO.builder()
                    .id(1L).userId(100L).maskedRealName("张**")
                    .maskedIdCardNo("3301**********1234")
                    .authStatus(KycStatus.PENDING)
                    .materialTypes(List.of(KycMaterialType.values()))
                    .build();
            when(kycService.getKycDetail(1L)).thenReturn(vo);

            Result<KycDetailVO> result = controller.getKycDetail(1L);

            assertEquals("3301**********1234", result.getData().maskedIdCardNo());
            verify(kycService).getKycDetail(1L);
        }
    }

    @Nested
    class GetMaterialPresign {

        @Test
        void getMaterialPresign_success() {
            KycMaterialPresignVO vo = KycMaterialPresignVO.builder()
                    .downloadUrl("https://rustfs.example/presigned")
                    .expiresInSeconds(300L)
                    .build();
            when(kycService.getMaterialPresign(1L, KycMaterialType.ID_CARD_FRONT))
                    .thenReturn(vo);

            Result<KycMaterialPresignVO> result =
                    controller.getMaterialPresign(1L, KycMaterialType.ID_CARD_FRONT);

            assertNotNull(result.getData().downloadUrl());
            verify(kycService).getMaterialPresign(1L, KycMaterialType.ID_CARD_FRONT);
        }
    }
}
