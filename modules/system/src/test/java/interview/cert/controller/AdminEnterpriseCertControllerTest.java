package interview.cert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.cert.model.enums.CertAuditAction;
import interview.cert.model.enums.CertStatus;
import interview.cert.model.req.EnterpriseCertAuditReq;
import interview.cert.model.req.EnterpriseCertSearchReq;
import interview.cert.model.vo.EnterpriseCertAuditVO;
import interview.cert.model.vo.EnterpriseCertDetailVO;
import interview.cert.model.vo.EnterpriseCertLicensePresignVO;
import interview.cert.model.vo.EnterpriseCertListItemVO;
import interview.cert.service.EnterpriseCertService;
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
class AdminEnterpriseCertControllerTest {

    @Mock
    private EnterpriseCertService enterpriseCertService;

    private AdminEnterpriseCertController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminEnterpriseCertController(enterpriseCertService);
    }

    @Nested
    class AuditCertification {

        @Test
        void auditCertification_reject_success() {
            EnterpriseCertAuditReq req = new EnterpriseCertAuditReq();
            req.setAction(CertAuditAction.REJECT);
            req.setRejectReason("执照与信用代码不一致");
            EnterpriseCertAuditVO vo = EnterpriseCertAuditVO.builder()
                    .id(1L)
                    .auditStatus(CertStatus.REJECTED)
                    .build();
            when(enterpriseCertService.auditCertification(1L, req)).thenReturn(vo);

            Result<EnterpriseCertAuditVO> result = controller.auditCertification(1L, req);

            assertEquals(CertStatus.REJECTED, result.getData().auditStatus());
            verify(enterpriseCertService).auditCertification(1L, req);
        }

        @Test
        void auditCertification_notFound_fail() {
            EnterpriseCertAuditReq req = new EnterpriseCertAuditReq();
            req.setAction(CertAuditAction.APPROVE);
            when(enterpriseCertService.auditCertification(99L, req))
                    .thenThrow(new BusinessException(ErrorCode.ENTERPRISE_CERT_NOT_FOUND));

            assertThrows(BusinessException.class, () -> controller.auditCertification(99L, req));
        }
    }

    @Nested
    class PageCertifications {

        @Test
        void pageCertifications_success() {
            EnterpriseCertSearchReq req = new EnterpriseCertSearchReq();
            req.setPage(1);
            req.setSize(20);
            req.setAuditStatus(CertStatus.PENDING);
            Page<EnterpriseCertListItemVO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(EnterpriseCertListItemVO.builder()
                    .id(1L).enterpriseId(10L).companyName("杭州示例科技有限公司")
                    .auditStatus(CertStatus.PENDING).build()));
            when(enterpriseCertService.pageCertifications(req)).thenReturn(page);

            Result<IPage<EnterpriseCertListItemVO>> result = controller.pageCertifications(req);

            assertEquals(1, result.getData().getTotal());
            assertEquals("杭州示例科技有限公司",
                    result.getData().getRecords().get(0).companyName());
        }
    }

    @Nested
    class GetCertificationDetail {

        @Test
        void getCertificationDetail_success() {
            EnterpriseCertDetailVO vo = EnterpriseCertDetailVO.builder()
                    .id(1L).enterpriseId(10L)
                    .creditCodeMasked("9133**********01X")
                    .legalPersonMasked("张**")
                    .auditStatus(CertStatus.PENDING)
                    .build();
            when(enterpriseCertService.getCertificationDetail(1L)).thenReturn(vo);

            Result<EnterpriseCertDetailVO> result = controller.getCertificationDetail(1L);

            assertEquals("9133**********01X", result.getData().creditCodeMasked());
            verify(enterpriseCertService).getCertificationDetail(1L);
        }
    }

    @Nested
    class GetLicensePresign {

        @Test
        void getLicensePresign_success() {
            EnterpriseCertLicensePresignVO vo = EnterpriseCertLicensePresignVO.builder()
                    .downloadUrl("https://rustfs.example/presigned")
                    .expiresInSeconds(300L)
                    .build();
            when(enterpriseCertService.getLicensePresign(1L)).thenReturn(vo);

            Result<EnterpriseCertLicensePresignVO> result = controller.getLicensePresign(1L);

            assertNotNull(result.getData().downloadUrl());
            verify(enterpriseCertService).getLicensePresign(1L);
        }
    }
}
