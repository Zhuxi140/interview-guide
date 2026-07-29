package interview.ai.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.ai.config.model.entity.LlmProviderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LlmProviderConfigMapper extends BaseMapper<LlmProviderConfig> {

    /**
     * 查询包含逻辑删除数据的 Provider。
     * @param providerId Provider ID
     * @return Provider 配置
     */
    LlmProviderConfig selectByIdIncludingDeleted(@Param("providerId") String providerId);

    /**
     * 锁定包含逻辑删除数据的 Provider。
     * @param providerId Provider ID
     * @return Provider 配置
     */
    LlmProviderConfig lockByIdIncludingDeleted(@Param("providerId") String providerId);

    /**
     * 按固定顺序锁定多个 Provider。
     * @param providerIds Provider ID 集合
     * @return Provider 配置集合
     */
    List<LlmProviderConfig> lockProvidersByIds(@Param("providerIds") List<String> providerIds);

    /**
     * 使用新配置恢复逻辑删除的 Provider。
     * @param provider 新配置与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int restoreDeletedProvider(@Param("provider") LlmProviderConfig provider,
                               @Param("expectedVersion") Integer expectedVersion);

    /**
     * 按版本半量更新 Provider。
     * @param provider 半量更新字段与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int updateProviderByVersion(@Param("provider") LlmProviderConfig provider,
                                @Param("expectedVersion") Integer expectedVersion);

    /**
     * 按版本更新 Provider 启停状态。
     * @param provider 状态与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int updateProviderStatusByVersion(@Param("provider") LlmProviderConfig provider,
                                      @Param("expectedVersion") Integer expectedVersion);

    /**
     * 按版本逻辑删除 Provider。
     * @param provider Provider ID 与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int deleteProviderByVersion(@Param("provider") LlmProviderConfig provider,
                                @Param("expectedVersion") Integer expectedVersion);
}
