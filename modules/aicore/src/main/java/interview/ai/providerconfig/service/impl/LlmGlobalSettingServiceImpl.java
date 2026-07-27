package interview.ai.providerconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.ai.providerconfig.mapper.LlmGlobalSettingMapper;
import interview.ai.providerconfig.model.entity.LlmGlobalSetting;
import interview.ai.providerconfig.model.req.LlmSettingUpdateReq;
import interview.ai.providerconfig.model.vo.LlmSettingVO;
import interview.ai.providerconfig.service.LlmGlobalSettingService;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LlmGlobalSettingServiceImpl
        extends ServiceImpl<LlmGlobalSettingMapper, LlmGlobalSetting>
        implements LlmGlobalSettingService {

    @Override
    @Transactional(readOnly = true)
    public LlmSettingVO getSetting() {
        // 纯 CRUD：读取固定 id=1 的全局路由单例。
        LlmGlobalSetting setting = baseMapper.selectOne(
                new LambdaQueryWrapper<LlmGlobalSetting>()
                        .select(
                                LlmGlobalSetting::getId,
                                LlmGlobalSetting::getDefaultChatProviderId,
                                LlmGlobalSetting::getDefaultEmbeddingProviderId,
                                LlmGlobalSetting::getVersion,
                                LlmGlobalSetting::getUpdatedAt
                        )
                        .eq(LlmGlobalSetting::getId, 1L)
        );
        if (setting == null) {
            throw new BusinessException(ErrorCode.AI_GLOBAL_SETTING_NOT_FOUND);
        }

        // 将数据库单例转换为接口响应。
        return new LlmSettingVO(
                setting.getId().toString(),
                setting.getDefaultChatProviderId(),
                setting.getDefaultEmbeddingProviderId(),
                setting.getVersion(),
                setting.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public LlmSettingVO updateSetting(LlmSettingUpdateReq req) {
        // TODO ① 通过 Mapper XML 对 llm_global_setting(id=1) 执行 FOR UPDATE；不存在返回 120009，并保持其为唯一单例。
        // TODO ② 收集默认 Chat 与 Embedding Provider ID，去重并排序后依次加行锁，保持与 Provider 启停、删除相同的锁顺序。
        // TODO ③ 校验默认 Chat Provider 存在、未删除且 enabled=true；否则分别返回 120001 或 120002。
        // TODO ④ defaultEmbeddingProviderId 非空时执行相同校验；当前表无能力类型字段，暂不能在数据库层确认其一定支持 Embedding。
        // TODO ⑤ 比较 setting.version 与 expectedVersion，不一致返回 120017。
        // TODO ⑥ 使用实体半量更新两个默认路由和 expectedVersion，让 @Version 完成 CAS、version+1 与审计字段自动填充。
        // TODO ⑦ 检查 updateById 返回值；零行返回 120017，成功后重新查询并构造 LlmSettingVO。
        // TODO ⑧ 注册事务提交后的配置变更事件，由监听器失效 AICore 路由缓存；缓存处理失败只告警并依靠短 TTL 收敛。
        return null;
    }
}
