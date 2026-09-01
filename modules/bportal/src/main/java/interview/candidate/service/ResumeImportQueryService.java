package interview.candidate.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.candidate.model.req.ResumeImportItemPageReq;
import interview.candidate.model.vo.ResumeImportBatchVO;
import interview.candidate.model.vo.ResumeImportItemVO;

public interface ResumeImportQueryService {

    /**
     * 查询企业简历导入批次
     * @param enterpriseId 企业 ID
     * @param batchId 批次 ID
     * @return 批次详情
     */
    ResumeImportBatchVO getBatch(Long enterpriseId, Long batchId);

    /**
     * 分页查询企业简历导入明细
     * @param enterpriseId 企业 ID
     * @param batchId 批次 ID
     * @param req 分页筛选参数
     * @return 导入明细分页
     */
    IPage<ResumeImportItemVO> pageItems(
            Long enterpriseId, Long batchId, ResumeImportItemPageReq req);
}
