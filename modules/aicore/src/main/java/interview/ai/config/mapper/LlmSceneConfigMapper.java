package interview.ai.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.ai.config.model.entity.LlmSceneConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LlmSceneConfigMapper extends BaseMapper<LlmSceneConfig> {

    /**
     * 锁定场景配置。
     * @param sceneCode 场景编码
     * @return 场景配置
     */
    LlmSceneConfig lockBySceneCode(@Param("sceneCode") String sceneCode);

    /**
     * 按场景编码顺序锁定直接引用指定 Provider 的启用场景。
     * @param providerId Provider ID
     * @return 启用场景
     */
    List<LlmSceneConfig> lockEnabledScenesByProviderId(@Param("providerId") String providerId);

    /**
     * 按版本更新场景配置。
     * @param scene 更新字段与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int updateSceneByVersion(@Param("scene") LlmSceneConfig scene,
                             @Param("expectedVersion") Integer expectedVersion);

    /**
     * 按版本更新场景启停状态。
     * @param scene 状态与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int updateSceneStatusByVersion(@Param("scene") LlmSceneConfig scene,
                                   @Param("expectedVersion") Integer expectedVersion);
}
