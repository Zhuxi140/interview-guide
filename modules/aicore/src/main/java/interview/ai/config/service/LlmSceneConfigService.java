package interview.ai.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.ai.config.model.entity.LlmSceneConfig;
import interview.ai.config.model.req.LlmSceneQueryReq;
import interview.ai.config.model.req.LlmSceneStatusReq;
import interview.ai.config.model.req.LlmSceneUpdateReq;
import interview.ai.config.model.vo.LlmScenePageVO;
import interview.ai.config.model.vo.LlmSceneStatusVO;
import interview.ai.config.model.vo.LlmSceneVO;

public interface LlmSceneConfigService extends IService<LlmSceneConfig> {

    /**
     * 分页查询 AI 场景执行参数。
     * @param req 分页查询请求
     * @return 分页结果
     */
    LlmScenePageVO pageScenes(LlmSceneQueryReq req);

    /**
     * 查询 AI 场景执行参数详情。
     * @param sceneCode 场景编码
     * @return 场景执行参数
     */
    LlmSceneVO getScene(String sceneCode);

    /**
     * 更新 AI 场景执行参数（CAS 乐观锁）。
     * @param sceneCode 场景编码
     * @param req 更新请求
     * @return 更新后的场景执行参数
     */
    LlmSceneVO updateScene(String sceneCode, LlmSceneUpdateReq req);

    /**
     * 启停 AI 场景（CAS 乐观锁）。
     * @param sceneCode 场景编码
     * @param req 启停请求
     * @return 启停结果
     */
    LlmSceneStatusVO updateSceneStatus(String sceneCode, LlmSceneStatusReq req);
}
