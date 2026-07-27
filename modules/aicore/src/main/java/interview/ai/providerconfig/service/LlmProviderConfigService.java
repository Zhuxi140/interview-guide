package interview.ai.providerconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.ai.providerconfig.model.entity.LlmProviderConfig;
import interview.ai.providerconfig.model.req.LlmProviderCreateReq;
import interview.ai.providerconfig.model.req.LlmProviderQueryReq;
import interview.ai.providerconfig.model.req.LlmProviderStatusReq;
import interview.ai.providerconfig.model.req.LlmProviderUpdateReq;
import interview.ai.providerconfig.model.vo.LlmProviderDeleteVO;
import interview.ai.providerconfig.model.vo.LlmProviderPageVO;
import interview.ai.providerconfig.model.vo.LlmProviderStatusVO;
import interview.ai.providerconfig.model.vo.LlmProviderTestConnectionVO;
import interview.ai.providerconfig.model.vo.LlmProviderVO;

public interface LlmProviderConfigService extends IService<LlmProviderConfig> {

    /**
     * 创建大模型提供商配置
     * @param req 创建请求
     * @return 提供商配置（API Key 掩码）
     */
    LlmProviderVO createProvider(LlmProviderCreateReq req);

    /**
     * 分页查询大模型提供商配置
     * @param req 分页查询请求
     * @return 分页结果
     */
    LlmProviderPageVO pageProviders(LlmProviderQueryReq req);

    /**
     * 查询大模型提供商配置详情
     * @param providerId 提供商配置 ID
     * @return 提供商配置（API Key 掩码）
     */
    LlmProviderVO getProvider(String providerId);

    /**
     * 更新大模型提供商配置（CAS 乐观锁）
     * @param providerId 提供商配置 ID
     * @param req 更新请求
     * @return 更新后的提供商配置（API Key 掩码）
     */
    LlmProviderVO updateProvider(String providerId, LlmProviderUpdateReq req);

    /**
     * 启停大模型提供商（CAS 乐观锁）
     * @param providerId 提供商配置 ID
     * @param req 启停请求
     * @return 启停结果
     */
    LlmProviderStatusVO updateProviderStatus(String providerId, LlmProviderStatusReq req);

    /**
     * 逻辑删除大模型提供商配置（CAS 乐观锁）
     * @param providerId 提供商配置 ID
     * @param expectedVersion 期望版本号
     * @return 已删除的提供商配置 ID
     */
    LlmProviderDeleteVO deleteProvider(String providerId, Integer expectedVersion);

    /**
     * 测试大模型提供商连接
     * @param providerId 提供商配置 ID
     * @return 连接测试结果
     */
    LlmProviderTestConnectionVO testConnection(String providerId);
}
