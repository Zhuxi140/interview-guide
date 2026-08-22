package interview.rag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.rag.model.entity.RagChatSession;
import interview.rag.model.req.RagMessageCursorReq;
import interview.rag.model.req.RagSessionCreateReq;
import interview.rag.model.req.RagSessionKnowledgeBindReq;
import interview.rag.model.req.RagSessionSearchReq;
import interview.rag.model.req.TemplateKnowledgeBindReq;
import interview.rag.model.vo.RagMessagePageVO;
import interview.rag.model.vo.RagSessionCreateVO;
import interview.rag.model.vo.RagSessionKnowledgeBindVO;
import interview.rag.model.vo.RagSessionListItemVO;
import interview.rag.model.vo.TemplateKnowledgeBindVO;

/**
 * RAG 对话服务：会话创建与查询、会话/模板知识库绑定、消息游标分页。
 */
public interface RagSessionService extends IService<RagChatSession> {

    /**
     * 企业成员创建 RAG 对话会话
     * @param enterpriseId 企业 ID（服务端确定，不接受请求体传入）
     * @param req 创建请求（标题可选）
     * @return 创建结果
     */
    RagSessionCreateVO createSession(Long enterpriseId, RagSessionCreateReq req);

    /**
     * 分页查询本人在该企业下的 RAG 会话
     * @param enterpriseId 企业 ID
     * @param req 分页查询参数
     * @return 会话分页（含消息数量统计）
     */
    IPage<RagSessionListItemVO> pageMySessions(Long enterpriseId, RagSessionSearchReq req);

    /**
     * 完整替换会话绑定的知识库集合
     * @param enterpriseId 企业 ID
     * @param sessionId 会话 ID
     * @param req 绑定请求
     * @return 绑定生效结果
     */
    RagSessionKnowledgeBindVO bindSessionKnowledgeBases(
            Long enterpriseId, Long sessionId, RagSessionKnowledgeBindReq req);

    /**
     * 游标分页查询会话消息（按 id 升序）
     * @param enterpriseId 企业 ID
     * @param sessionId 会话 ID
     * @param req 游标分页参数
     * @return 消息分页（含 nextCursor 与 hasMore）
     */
    RagMessagePageVO listMessages(Long enterpriseId, Long sessionId, RagMessageCursorReq req);

    /**
     * 完整替换面试模板允许检索的知识库集合
     * @param enterpriseId 企业 ID
     * @param templateId 模板 ID
     * @param req 绑定请求（含期望模板版本号）
     * @return 绑定生效结果
     */
    TemplateKnowledgeBindVO bindTemplateKnowledgeBases(
            Long enterpriseId, Long templateId, TemplateKnowledgeBindReq req);
}
