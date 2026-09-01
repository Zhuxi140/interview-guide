package interview.ai.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.ai.config.model.entity.AiGlobalRoute;
import interview.ai.config.model.req.AiGlobalRouteUpdateReq;
import interview.ai.config.model.vo.AiGlobalRouteListVO;
import interview.ai.config.model.vo.AiGlobalRouteVO;
import interview.ai.config.model.vo.AiRouteDetailVO;
import interview.ai.config.model.vo.AiRouteHealthVO;
import interview.common.enums.AiModelType;

public interface AiGlobalRouteService extends IService<AiGlobalRoute> {

    /**
     * 查询全部 AI 全局默认路由。
     * @return 全局默认路由列表
     */
    AiGlobalRouteListVO listRoutes();

    /**
     * 查询指定模型类型的路由详情
     * @param modelType 模型能力类型
     * @return 路由详情
     */
    AiRouteDetailVO getRoute(AiModelType modelType);

    /**
     * 查询指定模型类型的运行健康状态
     * @param modelType 模型能力类型
     * @return Provider 运行健康状态
     */
    AiRouteHealthVO getRouteHealth(AiModelType modelType);

    /**
     * 更新指定模型类型的全局默认路由。
     * @param modelType 模型能力类型
     * @param req 更新请求
     * @return 更新后的全局默认路由
     */
    AiGlobalRouteVO updateRoute(AiModelType modelType, AiGlobalRouteUpdateReq req);
}
