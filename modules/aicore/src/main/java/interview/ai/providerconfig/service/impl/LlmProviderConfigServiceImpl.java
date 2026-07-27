package interview.ai.providerconfig.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.ai.providerconfig.mapper.LlmProviderConfigMapper;
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
import interview.ai.providerconfig.service.LlmProviderConfigService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmProviderConfigServiceImpl
        extends ServiceImpl<LlmProviderConfigMapper, LlmProviderConfig>
        implements LlmProviderConfigService {

    private static final String MASKED_API_KEY = "******";

    @Override
    @Transactional
    public LlmProviderVO createProvider(LlmProviderCreateReq req) {
        // TODO ① 规范化 providerId、baseUrl 和 model，并校验 ID 格式、URL 协议及字段长度。
        // TODO ② 通过 Mapper XML 查询包含逻辑删除数据的同名 providerId；主键已占用时返回 120013，禁止直接复用旧密钥记录。
        // TODO ③ 调用专用密钥组件使用 AES/GCM 加密 apiKey；密钥版本、随机 IV 与密文一并封装，禁止记录明文日志。
        // TODO ④ 构建 LlmProviderConfig 实体，首次创建固定 version=0；enabled 未传时使用 false。
        // TODO ⑤ 在当前事务插入实体，并将并发插入产生的主键冲突转换为 120013。
        // TODO ⑥ 重新查询已保存记录，返回只包含固定掩码的 LlmProviderVO，不向 Controller 暴露密文。
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public LlmProviderPageVO pageProviders(LlmProviderQueryReq req) {
        // 纯 CRUD：组合启停状态、关键字和白名单排序条件。
        String keyword = StrUtil.isBlank(req.getKeyword()) ? null : req.getKeyword().trim();
        LambdaQueryWrapper<LlmProviderConfig> wrapper = new LambdaQueryWrapper<LlmProviderConfig>()
                .select(
                        LlmProviderConfig::getId,
                        LlmProviderConfig::getBaseUrl,
                        LlmProviderConfig::getModel,
                        LlmProviderConfig::getEnabled,
                        LlmProviderConfig::getVersion,
                        LlmProviderConfig::getCreatedAt,
                        LlmProviderConfig::getUpdatedAt
                )
                .eq(req.getEnabled() != null, LlmProviderConfig::getEnabled, req.getEnabled())
                .and(StrUtil.isNotBlank(keyword), query -> query
                        .like(LlmProviderConfig::getId, keyword)
                        .or()
                        .like(LlmProviderConfig::getBaseUrl, keyword)
                        .or()
                        .like(LlmProviderConfig::getModel, keyword));
        applySorting(wrapper, req.getSort(), req.getOrder());

        // 执行分页查询，并将密钥字段转换为固定掩码。
        Page<LlmProviderConfig> rawPage = baseMapper.selectPage(
                new Page<>(req.getPage(), req.getSize()),
                wrapper
        );
        List<LlmProviderVO> records = rawPage.getRecords().stream()
                .map(this::toProviderVO)
                .toList();

        return new LlmProviderPageVO(
                rawPage.getCurrent(),
                Math.toIntExact(rawPage.getSize()),
                rawPage.getTotal(),
                rawPage.getPages(),
                records
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LlmProviderVO getProvider(String providerId) {
        // 纯 CRUD：只查询详情响应需要的字段，逻辑删除条件由 MyBatis-Plus 自动追加。
        LlmProviderConfig provider = baseMapper.selectOne(
                new LambdaQueryWrapper<LlmProviderConfig>()
                        .select(
                                LlmProviderConfig::getId,
                                LlmProviderConfig::getBaseUrl,
                                LlmProviderConfig::getModel,
                                LlmProviderConfig::getEnabled,
                                LlmProviderConfig::getVersion,
                                LlmProviderConfig::getCreatedAt,
                                LlmProviderConfig::getUpdatedAt
                        )
                        .eq(LlmProviderConfig::getId, providerId)
        );
        if (provider == null) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_NOT_FOUND);
        }

        // 返回固定密钥掩码，避免密文进入接口响应。
        return toProviderVO(provider);
    }

    @Override
    @Transactional
    public LlmProviderVO updateProvider(String providerId, LlmProviderUpdateReq req) {
        // TODO ① 校验 baseUrl、apiKey、model 至少提交一项，并规范化非空字符串；空字符串不得覆盖原配置。
        // TODO ② 按 providerId 查询当前记录；不存在返回 120001，当前 version 与 expectedVersion 不同返回 120014。
        // TODO ③ 若修改 apiKey，调用密钥组件生成新的 AES/GCM 密文；整个过程中禁止输出明文或密文日志。
        // TODO ④ 使用实体承载半量更新字段和 expectedVersion，使 @Version 生成 id + version 条件并自动执行 version+1。
        // TODO ⑤ 检查 updateById 返回值；更新零行说明并发版本已变化，返回 120014，事务内不保留任何局部修改。
        // TODO ⑥ 重新查询最新记录并返回 LlmProviderVO；apiKeyMasked 只能表示“已配置”，不得从密文截取。
        return null;
    }

    @Override
    @Transactional
    public LlmProviderStatusVO updateProviderStatus(String providerId, LlmProviderStatusReq req) {
        // TODO ① 通过 Mapper XML 先 FOR UPDATE 锁定 llm_global_setting(id=1)，再锁定目标 Provider，统一所有路由变更的加锁顺序。
        // TODO ② Provider 不存在时返回 120001；version 与 expectedVersion 不匹配时返回 120014。
        // TODO ③ 当 enabled=false 时，检查默认对话和默认向量路由是否引用该 Provider；仍被引用则返回 120015。
        // TODO ④ 使用只设置 enabled 的实体执行 updateById，让 @Version 完成 CAS 与 version+1，并触发审计字段自动填充。
        // TODO ⑤ 更新零行返回 120014；成功后返回最新 enabled、version 和 updatedAt。
        // TODO ⑥ 事务提交后发布路由配置变更事件，使运行时路由缓存收敛到新状态。
        return null;
    }

    @Override
    @Transactional
    public LlmProviderDeleteVO deleteProvider(String providerId, Integer expectedVersion) {
        // TODO ① 通过 Mapper XML 先锁定 llm_global_setting(id=1)，再锁定目标 Provider，保持与启停、全局设置更新相同的锁顺序。
        // TODO ② Provider 不存在时返回 120001；version 与 expectedVersion 不匹配时返回 120014。
        // TODO ③ 检查默认对话和默认向量路由是否仍引用该 Provider；存在引用时返回 120015。
        // TODO ④ 使用实体半量更新 isDeleted=true，并携带 expectedVersion 完成 CAS、version+1 和审计字段填充。
        // TODO ⑤ 更新零行返回 120014；成功后返回 providerId，并在事务提交后失效运行时路由缓存。
        return null;
    }

    @Override
    public LlmProviderTestConnectionVO testConnection(String providerId) {
        // TODO ① 查询 Provider 必要字段；不存在返回 120001。允许测试尚未启用的配置，以便管理员先测试后启用。
        // TODO ② 调用密钥组件校验密文格式和密钥版本，再在最小作用域内解密 API Key；解密失败返回 120003。
        // TODO ③ 根据 baseUrl、model 和临时明文 Key 构建一次性 Spring AI Client，不污染当前全局默认客户端缓存。
        // TODO ④ 设置独立的短超时并发送最小测试请求，使用单调时钟计算 latencyMs。
        // TODO ⑤ 将鉴权失败、模型不存在、超时和其他连接失败分别映射为 120003、120004、120006、120016。
        // TODO ⑥ 成功返回 reachable=true、latencyMs 和 checkedAt；失败直接抛业务异常，不伪造 reachable=false 的成功响应。
        // TODO ⑦ finally 中清理临时引用；日志只允许记录 providerId、模型和错误分类，严禁记录请求头、明文 Key 或密文。
        return null;
    }

    /**
     * 按受支持的字段应用稳定排序。
     */
    private void applySorting(LambdaQueryWrapper<LlmProviderConfig> wrapper,
                              String requestedSort,
                              String requestedOrder) {
        String sort = StrUtil.isBlank(requestedSort) ? "createdAt" : requestedSort;
        boolean ascending = "asc".equalsIgnoreCase(requestedOrder);

        switch (sort) {
            case "providerId" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getId);
            case "baseUrl" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getBaseUrl);
            case "model" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getModel);
            case "enabled" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getEnabled);
            case "updatedAt" -> wrapper.orderBy(true, ascending, LlmProviderConfig::getUpdatedAt);
            default -> wrapper.orderBy(true, ascending, LlmProviderConfig::getCreatedAt);
        }
        if (!"providerId".equals(sort)) {
            wrapper.orderByAsc(LlmProviderConfig::getId);
        }
    }

    /**
     * 将 Provider 实体转换为不暴露密文的响应。
     */
    private LlmProviderVO toProviderVO(LlmProviderConfig provider) {
        return LlmProviderVO.builder()
                .providerId(provider.getId())
                .baseUrl(provider.getBaseUrl())
                .apiKeyMasked(MASKED_API_KEY)
                .model(provider.getModel())
                .enabled(provider.getEnabled())
                .version(provider.getVersion())
                .createdAt(provider.getCreatedAt())
                .updatedAt(provider.getUpdatedAt())
                .build();
    }
}
