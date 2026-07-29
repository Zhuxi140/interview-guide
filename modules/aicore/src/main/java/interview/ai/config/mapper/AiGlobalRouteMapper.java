package interview.ai.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.common.enums.AiModelType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiGlobalRouteMapper extends BaseMapper<AiGlobalRoute> {

    /**
     * 锁定指定模型类型的全局路由。
     * @param modelType 模型能力类型
     * @return 全局路由
     */
    AiGlobalRoute lockByModelType(@Param("modelType") AiModelType modelType);

    /**
     * 按模型类型顺序锁定全部全局路由。
     * @return 全部全局路由
     */
    List<AiGlobalRoute> lockAll();

    /**
     * 按版本更新指定模型类型的全局路由。
     * @param route 新路由与审计信息
     * @param expectedVersion 期望版本
     * @return 受影响行数
     */
    int updateRouteByVersion(@Param("route") AiGlobalRoute route,
                             @Param("expectedVersion") Integer expectedVersion);
}
