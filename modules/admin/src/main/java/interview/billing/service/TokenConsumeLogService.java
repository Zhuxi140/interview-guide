package interview.billing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.billing.model.entity.TokenConsumeLog;
import interview.billing.model.vo.TokenConsumeLogListItemVO;
import interview.common.enums.BizType;

import java.time.OffsetDateTime;

public interface TokenConsumeLogService extends IService<TokenConsumeLog> {

    /**
     * 分页查询企业算力消耗明细
     * @param enterpriseId 企业 ID
     * @param page 当前页
     * @param size 每页数量
     * @param bizType 业务类型
     * @param startTime 创建时间起点
     * @param endTime 创建时间终点
     * @return 算力消耗明细分页
     */
    IPage<TokenConsumeLogListItemVO> pageLogs(Long enterpriseId, Integer page, Integer size,
                                               BizType bizType,
                                               OffsetDateTime startTime, OffsetDateTime endTime);
}
