package interview.compliance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.compliance.model.entity.UserApiPolicy;
import interview.compliance.model.req.ApiPolicyCreateReq;
import interview.compliance.model.req.ApiPolicySearchReq;
import interview.compliance.model.req.ApiPolicyUpdateReq;
import interview.compliance.model.vo.ApiPolicyListItemVO;
import interview.compliance.model.vo.ApiPolicyMutationVO;

/**
 * @author zhuxi
 * @apiNote 平台端用户 API 策略管理服务（限流/黑白名单）
 */
public interface ApiPolicyService extends IService<UserApiPolicy> {

    /**
     * 创建用户 API 策略（限流参数与名单互斥等校验）
     * @param req 创建请求
     * @return 创建结果
     */
    ApiPolicyMutationVO createPolicy(ApiPolicyCreateReq req);

    /**
     * 分页查询用户 API 策略列表
     * @param req 分页与策略类型筛选条件
     * @return 策略分页
     */
    IPage<ApiPolicyListItemVO> pagePolicies(ApiPolicySearchReq req);

    /**
     * 更新策略（半量更新 + 乐观锁）
     * @param policyId 策略 ID
     * @param req 更新请求
     * @return 更新结果
     */
    ApiPolicyMutationVO updatePolicy(Long policyId, ApiPolicyUpdateReq req);

    /**
     * 删除策略（逻辑删除 + If-Match 版本校验）
     * @param policyId 策略 ID
     * @param expectedVersion 期望版本号
     */
    void deletePolicy(Long policyId, Integer expectedVersion);
}
