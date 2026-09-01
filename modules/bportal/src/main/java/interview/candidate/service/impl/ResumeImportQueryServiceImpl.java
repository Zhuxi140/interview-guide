package interview.candidate.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.api.system.EnterpriseValidationApi;
import interview.candidate.mapper.ResumeImportBatchMapper;
import interview.candidate.mapper.ResumeImportItemMapper;
import interview.candidate.model.entity.ResumeImportBatch;
import interview.candidate.model.entity.ResumeImportItem;
import interview.candidate.model.req.ResumeImportItemPageReq;
import interview.candidate.model.vo.ResumeImportBatchVO;
import interview.candidate.model.vo.ResumeImportItemVO;
import interview.candidate.service.ResumeImportQueryService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeImportQueryServiceImpl implements ResumeImportQueryService {

    private final ResumeImportBatchMapper resumeImportBatchMapper;
    private final ResumeImportItemMapper resumeImportItemMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    public ResumeImportBatchVO getBatch(Long enterpriseId, Long batchId) {
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        ResumeImportBatch batch = resumeImportBatchMapper.selectOne(
                Wrappers.<ResumeImportBatch>lambdaQuery()
                        .eq(ResumeImportBatch::getId, batchId)
                        .eq(ResumeImportBatch::getEnterpriseId, enterpriseId));
        if (batch == null) {
            throw new BusinessException(ErrorCode.RESUME_IMPORT_BATCH_NOT_FOUND);
        }
        return new ResumeImportBatchVO(
                batch.getId(), batch.getStatus(), batch.getTotalCount(),
                batch.getSuccessCount(), batch.getFailedCount(),
                batch.getCreatedAt(), batch.getCompletedAt());
    }

    @Override
    public IPage<ResumeImportItemVO> pageItems(
            Long enterpriseId, Long batchId, ResumeImportItemPageReq req) {
        getBatch(enterpriseId, batchId);
        IPage<ResumeImportItem> itemPage = resumeImportItemMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                Wrappers.<ResumeImportItem>lambdaQuery()
                        .select(
                                ResumeImportItem::getId,
                                ResumeImportItem::getOriginalFilename,
                                ResumeImportItem::getStatus,
                                ResumeImportItem::getEnterpriseCandidateId,
                                ResumeImportItem::getErrorMessage)
                        .eq(ResumeImportItem::getEnterpriseId, enterpriseId)
                        .eq(ResumeImportItem::getBatchId, batchId)
                        .eq(req.getStatus() != null,
                                ResumeImportItem::getStatus, req.getStatus())
                        .orderByAsc(ResumeImportItem::getCreatedAt)
                        .orderByAsc(ResumeImportItem::getId));
        return itemPage.convert(item -> new ResumeImportItemVO(
                item.getId(), item.getOriginalFilename(), item.getStatus(),
                item.getEnterpriseCandidateId(), item.getErrorMessage()));
    }
}
