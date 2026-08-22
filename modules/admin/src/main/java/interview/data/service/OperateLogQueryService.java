package interview.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.data.model.vo.OperateLogDetailVO;
import interview.data.model.vo.OperateLogListItemVO;

import java.util.List;

/**
 * 业务操作审计日志查询服务（平台全局 + 企业域）。
 *
 * @author zhuxi
 */
public interface OperateLogQueryService {

    /**
     * 平台分页查询操作审计日志。
     *
     * @param page        页码
     * @param size        每页条数
     * @param userId      操作人筛选，可为空
     * @param module      业务模块筛选，可为空
     * @param operateType 操作类型筛选，可为空
     * @param resourceType 业务资源类型筛选，可为空
     * @param resourceId  业务资源 ID 筛选，可为空
     * @param startTime   开始时间（ISO-8601 带时区），可为空
     * @param endTime     结束时间（ISO-8601 带时区），可为空
     * @return 操作日志分页结果
     */
    IPage<OperateLogListItemVO> pageOperateLogs(Integer page, Integer size, Long userId,
                                                String module, String operateType,
                                                String resourceType, Long resourceId,
                                                String startTime, String endTime);

    /**
     * 平台查询单条脱敏操作审计详情。
     *
     * @param id 日志 ID
     * @return 操作日志详情
     */
    OperateLogDetailVO getOperateLogDetail(Long id);

    /**
     * 按 traceId 查询全部脱敏操作变更。
     *
     * @param traceId 链路 ID
     * @return 操作日志详情列表
     */
    List<OperateLogDetailVO> listByTraceId(String traceId);

    /**
     * 企业域分页查询本企业操作日志。
     *
     * @param enterpriseId 企业 ID
     * @param page         页码
     * @param size         每页条数
     * @param module       业务模块筛选，可为空
     * @param operateType  操作类型筛选，可为空
     * @param resourceType 业务资源类型筛选，可为空
     * @param resourceId   业务资源 ID 筛选，可为空
     * @param startTime    开始时间（ISO-8601 带时区），可为空
     * @param endTime      结束时间（ISO-8601 带时区），可为空
     * @return 企业域操作日志分页结果
     */
    IPage<OperateLogListItemVO> pageEnterpriseOperateLogs(Long enterpriseId, Integer page,
                                                          Integer size, String module,
                                                          String operateType, String resourceType,
                                                          Long resourceId, String startTime,
                                                          String endTime);

    /**
     * 企业域查询单条审计详情。
     *
     * @param enterpriseId 企业 ID
     * @param id           日志 ID
     * @return 操作日志详情
     */
    OperateLogDetailVO getEnterpriseOperateLogDetail(Long enterpriseId, Long id);
}
