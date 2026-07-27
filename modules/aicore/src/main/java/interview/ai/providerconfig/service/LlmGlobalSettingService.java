package interview.ai.providerconfig.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.ai.providerconfig.model.entity.LlmGlobalSetting;
import interview.ai.providerconfig.model.req.LlmSettingUpdateReq;
import interview.ai.providerconfig.model.vo.LlmSettingVO;

public interface LlmGlobalSettingService extends IService<LlmGlobalSetting> {

    /**
     * 查询 LLM 全局设置
     * @return 全局设置
     */
    LlmSettingVO getSetting();

    /**
     * 更新 LLM 全局设置（CAS 乐观锁）
     * @param req 更新请求
     * @return 更新后的全局设置
     */
    LlmSettingVO updateSetting(LlmSettingUpdateReq req);
}
