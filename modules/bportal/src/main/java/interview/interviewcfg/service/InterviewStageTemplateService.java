package interview.interviewcfg.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.interviewcfg.model.entity.InterviewStageTemplate;
import interview.interviewcfg.model.bo.InterviewTemplateSnapshot;
import interview.interviewcfg.model.req.InterviewTemplateCreateReq;
import interview.interviewcfg.model.req.InterviewTemplateUpdateReq;
import interview.interviewcfg.model.req.PhaseConfigUpsertReq;
import interview.interviewcfg.model.vo.*;

import java.util.List;

public interface InterviewStageTemplateService extends IService<InterviewStageTemplate> {

    /**
     * 创建面试阶段模板
     * @param enterpriseId 企业ID
     * @param req 创建请求
     * @return 创建结果
     */
    InterviewTemplateCreateVO createTemplate(Long enterpriseId, InterviewTemplateCreateReq req);

    /**
     * 查询面试阶段模板列表（分页）
     * @param enterpriseId 企业ID
     * @param page 页码
     * @param size 每页条数
     * @param sort 排序字段
     * @param order 排序方向
     * @return 模板分页
     */
    IPage<InterviewTemplateListItemVO> pageTemplates(Long enterpriseId, Integer page, Integer size, String sort, String order);

    /**
     * 查询模板详情（含阶段序列与组卷策略）
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @return 模板详情
     */
    InterviewTemplateDetailVO getTemplateDetail(Long enterpriseId, Long templateId);

    /**
     * 更新面试阶段模板
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @param req 更新请求
     * @return 更新结果
     */
    InterviewTemplateUpdateVO updateTemplate(Long enterpriseId, Long templateId, InterviewTemplateUpdateReq req);

    /**
     * 删除模板
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @param expectedVersion 期望版本号
     */
    void deleteTemplate(Long enterpriseId, Long templateId, Integer expectedVersion);

    /**
     * 创建或完整替换某阶段组卷策略
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @param phaseCode 阶段编码
     * @param req 策略配置
     * @return 策略结果
     */
    PhaseConfigVO upsertPhaseConfig(Long enterpriseId, Long templateId, String phaseCode, PhaseConfigUpsertReq req);

    /**
     * 查询阶段组卷策略列表
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @return 策略列表
     */
    List<PhaseConfigVO> listPhaseConfigs(Long enterpriseId, Long templateId);

    /**
     * 生成用于排期的完整模板快照
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @return 模板快照
     */
    InterviewTemplateSnapshot buildTemplateSnapshot(Long enterpriseId, Long templateId);

    /**
     * 免鉴权生成完整模板快照；调用方已完成企业归属与模板校验。
     *
     * @param enterpriseId 企业ID
     * @param templateId 模板ID
     * @return 模板快照；模板不存在时返回 null
     */
    InterviewTemplateSnapshot buildTemplateSnapshotWithoutAuth(Long enterpriseId, Long templateId);
}
