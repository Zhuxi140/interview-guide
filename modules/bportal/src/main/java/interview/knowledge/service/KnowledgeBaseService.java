package interview.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.knowledge.model.entity.KnowledgeBase;
import interview.knowledge.model.req.AdminKnowledgeBaseSearchReq;
import interview.knowledge.model.req.EnterpriseKnowledgeBaseSearchReq;
import interview.knowledge.model.vo.KnowledgeBaseDetailVO;
import interview.knowledge.model.vo.KnowledgeBaseListItemVO;

/**
 * 知识库文档服务：企业私有/可见全局文档查询与删除、平台全局文档管理。
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 分页查询企业可见知识库（本企业私有文档 + 可用全局文档）
     * @param enterpriseId 企业 ID
     * @param req 分页查询参数
     * @return 知识库分页
     */
    IPage<KnowledgeBaseListItemVO> pageEnterpriseKnowledgeBases(
            Long enterpriseId, EnterpriseKnowledgeBaseSearchReq req);

    /**
     * 查询企业有权访问的知识库详情（含切片数量统计）
     * @param enterpriseId 企业 ID
     * @param kbId 知识库 ID
     * @return 知识库详情
     */
    KnowledgeBaseDetailVO getEnterpriseKnowledgeBaseDetail(Long enterpriseId, Long kbId);

    /**
     * 删除企业私有知识库文档（乐观锁 + 逻辑删除）
     * @param enterpriseId 企业 ID
     * @param kbId 知识库 ID
     * @param expectedVersion 期望版本号（If-Match）
     */
    void deleteEnterpriseKnowledgeBase(Long enterpriseId, Long kbId, Integer expectedVersion);

    /**
     * 分页查询平台全局知识库文档
     * @param req 分页查询参数
     * @return 全局知识库分页
     */
    IPage<KnowledgeBaseListItemVO> pageGlobalKnowledgeBases(AdminKnowledgeBaseSearchReq req);

    /**
     * 查询平台全局知识库详情（含切片数量统计）
     * @param kbId 知识库 ID
     * @return 知识库详情
     */
    KnowledgeBaseDetailVO getGlobalKnowledgeBaseDetail(Long kbId);

    /**
     * 删除平台全局知识库文档（乐观锁 + 逻辑删除）
     * @param kbId 知识库 ID
     * @param expectedVersion 期望版本号（If-Match）
     */
    void deleteGlobalKnowledgeBase(Long kbId, Integer expectedVersion);
}
