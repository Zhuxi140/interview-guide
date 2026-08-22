package interview.data.service;

import interview.data.model.req.DataRetentionPoliciesUpdateReq;
import interview.data.model.vo.DataRetentionPoliciesVO;

/**
 * 数据保留策略单行配置服务。
 *
 * @author zhuxi
 */
public interface DataRetentionPolicyService {

    /**
     * 查询各类在线数据保留期限和归档开关；无配置时返回内置默认策略。
     *
     * @return 保留策略配置
     */
    DataRetentionPoliciesVO getPolicies();

    /**
     * 按版本完整更新数据保留策略（CAS）。
     *
     * @param req 全量更新请求
     * @return 更新后的保留策略配置
     */
    DataRetentionPoliciesVO updatePolicies(DataRetentionPoliciesUpdateReq req);
}
