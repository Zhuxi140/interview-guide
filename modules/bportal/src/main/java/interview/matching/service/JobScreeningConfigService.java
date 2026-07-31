package interview.matching.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.matching.model.entity.JobScreeningConfig;
import interview.matching.model.req.JobScreeningConfigUpdateReq;
import interview.matching.model.vo.JobScreeningConfigVO;

/**
 * 岗位 AI 初筛配置服务接口。
 */
public interface JobScreeningConfigService extends IService<JobScreeningConfig> {

    /**
     * 查询岗位 AI 初筛配置。
     *
     * @param enterpriseId 企业 ID
     * @param jobId 岗位 ID
     * @return 初筛配置
     */
    JobScreeningConfigVO getConfig(Long enterpriseId, Long jobId);

    /**
     * 创建或按版本更新岗位 AI 初筛配置。
     *
     * @param enterpriseId 企业 ID
     * @param jobId 岗位 ID
     * @param req 更新请求
     * @return 更新后的配置
     */
    JobScreeningConfigVO saveConfig(
            Long enterpriseId, Long jobId, JobScreeningConfigUpdateReq req);
}
