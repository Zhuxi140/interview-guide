package interview.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.system.EnterpriseValidationApi;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.framework.context.AuthContext;
import interview.interviewcfg.mapper.InterviewStageTemplateMapper;
import interview.interviewcfg.model.entity.InterviewStageTemplate;
import interview.knowledge.mapper.KnowledgeBaseMapper;
import interview.knowledge.model.entity.KnowledgeBase;
import interview.knowledge.model.enums.KnowledgeVisibility;
import interview.knowledge.model.enums.VectorStatus;
import interview.rag.mapper.InterviewTemplateKnowledgeBaseMapper;
import interview.rag.mapper.RagChatMessageMapper;
import interview.rag.mapper.RagChatSessionMapper;
import interview.rag.mapper.RagSessionKnowledgeBaseMapper;
import interview.rag.model.entity.InterviewTemplateKnowledgeBase;
import interview.rag.model.entity.RagChatMessage;
import interview.rag.model.entity.RagChatSession;
import interview.rag.model.entity.RagSessionKnowledgeBase;
import interview.rag.model.req.RagMessageCursorReq;
import interview.rag.model.req.RagSessionCreateReq;
import interview.rag.model.req.RagSessionKnowledgeBindReq;
import interview.rag.model.req.RagSessionSearchReq;
import interview.rag.model.req.TemplateKnowledgeBindReq;
import interview.rag.model.vo.RagMessagePageVO;
import interview.rag.model.vo.RagMessageVO;
import interview.rag.model.vo.RagSessionCreateVO;
import interview.rag.model.vo.RagSessionKnowledgeBindVO;
import interview.rag.model.vo.RagSessionListItemVO;
import interview.rag.model.vo.TemplateKnowledgeBindVO;
import interview.rag.service.RagSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RAG 对话服务实现。
 */
@Service
@RequiredArgsConstructor
public class RagSessionServiceImpl extends ServiceImpl<RagChatSessionMapper, RagChatSession>
        implements RagSessionService {

    private final RagChatMessageMapper ragChatMessageMapper;
    private final RagSessionKnowledgeBaseMapper ragSessionKnowledgeBaseMapper;
    private final InterviewTemplateKnowledgeBaseMapper interviewTemplateKnowledgeBaseMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final InterviewStageTemplateMapper interviewStageTemplateMapper;
    private final EnterpriseValidationApi enterpriseValidationApi;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public RagSessionCreateVO createSession(Long enterpriseId, RagSessionCreateReq req) {
        // 校验当前用户属于该企业，归属企业与创建人由服务端确定。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 创建会话：仅元信息落库，知识库绑定通过替换接口单独配置。
        RagChatSession session = RagChatSession.builder()
                .enterpriseId(enterpriseId)
                .userId(AuthContext.getRequiredUserId())
                .title(req.getTitle())
                .build();
        save(session);
        return RagSessionCreateVO.builder()
                .id(session.getId())
                .enterpriseId(session.getEnterpriseId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .build();
    }

    @Override
    public IPage<RagSessionListItemVO> pageMySessions(Long enterpriseId, RagSessionSearchReq req) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 会话仅对创建者本人可见：按企业 + 本人过滤，按 id 倒序。
        LambdaQueryWrapper<RagChatSession> wrapper = new LambdaQueryWrapper<RagChatSession>()
                .select(RagChatSession::getId, RagChatSession::getTitle,
                        RagChatSession::getCreatedAt)
                .eq(RagChatSession::getEnterpriseId, enterpriseId)
                .eq(RagChatSession::getUserId, AuthContext.getRequiredUserId())
                .orderByDesc(RagChatSession::getId);
        Page<RagChatSession> sessionPage =
                baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);

        // 单表 IN 批量查询消息归属，按会话分组计数补齐 messageCount，避免 N+1。
        List<Long> sessionIds = sessionPage.getRecords().stream()
                .map(RagChatSession::getId)
                .toList();
        Map<Long, Long> messageCounts = countMessagesBySessionIds(sessionIds);

        Page<RagSessionListItemVO> voPage = new Page<>(
                sessionPage.getCurrent(), sessionPage.getSize(), sessionPage.getTotal());
        voPage.setRecords(sessionPage.getRecords().stream()
                .map(session -> RagSessionListItemVO.builder()
                        .id(session.getId())
                        .title(session.getTitle())
                        .messageCount(messageCounts.getOrDefault(session.getId(), 0L))
                        .createdAt(session.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public RagSessionKnowledgeBindVO bindSessionKnowledgeBases(
            Long enterpriseId, Long sessionId, RagSessionKnowledgeBindReq req) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 校验会话归属：仅创建者本人在本企业下的会话可绑定。
        getOwnedSession(enterpriseId, sessionId);

        // 校验知识库可绑定：归属本企业或 GLOBAL，且向量化完成。
        List<Long> distinctIds =
                validateBindableKnowledgeBases(enterpriseId, req.getKnowledgeBaseIds());

        // 同事务完整替换：先逻辑删旧绑定，再批量插入新绑定。
        replaceSessionBindings(sessionId, distinctIds);
        return new RagSessionKnowledgeBindVO(sessionId, distinctIds);
    }

    @Override
    public RagMessagePageVO listMessages(
            Long enterpriseId, Long sessionId, RagMessageCursorReq req) {
        // 校验当前用户属于该企业与会话归属。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());
        getOwnedSession(enterpriseId, sessionId);

        // 游标分页：多取一条判断 hasMore，避免额外执行 count 查询。
        long cursor = req.getCursor() == null ? 0L : req.getCursor();
        List<RagChatMessage> rows = ragChatMessageMapper.selectPage(
                new Page<>(1, req.getSize() + 1L, false),
                Wrappers.<RagChatMessage>lambdaQuery()
                        .select(RagChatMessage::getId, RagChatMessage::getType,
                                RagChatMessage::getContent, RagChatMessage::getCreatedAt)
                        .eq(RagChatMessage::getSessionId, sessionId)
                        .gt(RagChatMessage::getId, cursor)
                        .orderByAsc(RagChatMessage::getId)
        ).getRecords();
        boolean hasMore = rows.size() > req.getSize();
        List<RagMessageVO> records = rows.stream()
                .limit(req.getSize())
                .map(message -> new RagMessageVO(
                        message.getId(),
                        message.getType(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
        Long nextCursor = hasMore ? records.getLast().id() : null;

        // TODO [Phase7] 异步回答流程落地后，AI 消息补充 answerStatus/failureReason/citations 引用溯源字段。
        return new RagMessagePageVO(nextCursor, hasMore, records);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public TemplateKnowledgeBindVO bindTemplateKnowledgeBases(
            Long enterpriseId, Long templateId, TemplateKnowledgeBindReq req) {
        // 校验当前用户属于该企业。
        enterpriseValidationApi.validateEnterpriseBelong(
                enterpriseId, AuthContext.getRequiredUserId());

        // 校验模板归属本企业：复用既有 interviewcfg 模板实体单表 LambdaQuery，不改动其文件。
        InterviewStageTemplate template = interviewStageTemplateMapper.selectOne(
                Wrappers.<InterviewStageTemplate>lambdaQuery()
                        .select(InterviewStageTemplate::getId,
                                InterviewStageTemplate::getEnterpriseId,
                                InterviewStageTemplate::getVersion)
                        .eq(InterviewStageTemplate::getId, templateId)
                        .eq(InterviewStageTemplate::getEnterpriseId, enterpriseId));
        if (template == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_NOT_FOUND);
        }

        // 版本校验防并发覆盖；绑定集合为独立聚合，不回写模板 version。
        if (!Objects.equals(template.getVersion(), req.getExpectedTemplateVersion())) {
            throw new BusinessException(ErrorCode.INTERVIEW_TEMPLATE_VERSION_CONFLICT);
        }

        // 校验知识库可绑定：归属本企业或 GLOBAL，且向量化完成。
        List<Long> distinctIds =
                validateBindableKnowledgeBases(enterpriseId, req.getKnowledgeBaseIds());

        // 同事务完整替换：先逻辑删旧绑定，再批量插入新绑定。
        replaceTemplateBindings(templateId, distinctIds);
        return new TemplateKnowledgeBindVO(templateId, distinctIds, template.getVersion());
    }

    private RagChatSession getOwnedSession(Long enterpriseId, Long sessionId) {
        // 会话归属 = 本企业 + 创建者本人，三元条件单表 LambdaQuery 完成。
        RagChatSession session = lambdaQuery()
                .eq(RagChatSession::getId, sessionId)
                .eq(RagChatSession::getEnterpriseId, enterpriseId)
                .eq(RagChatSession::getUserId, AuthContext.getRequiredUserId())
                .one();
        if (session == null) {
            throw new BusinessException(ErrorCode.RAG_SESSION_NOT_FOUND);
        }
        return session;
    }

    private List<Long> validateBindableKnowledgeBases(Long enterpriseId, List<Long> knowledgeBaseIds) {
        // 去重后单表 IN 查询，防止重复绑定同一文档；行数不一致说明存在无效 ID。
        List<Long> distinctIds = knowledgeBaseIds.stream().distinct().toList();
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(
                Wrappers.<KnowledgeBase>lambdaQuery()
                        .select(KnowledgeBase::getId, KnowledgeBase::getVisibility,
                                KnowledgeBase::getEnterpriseId, KnowledgeBase::getVectorStatus)
                        .in(KnowledgeBase::getId, distinctIds));
        if (knowledgeBases.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            // 归属校验：仅允许本企业私有文档与平台全局文档。
            boolean accessible = knowledgeBase.getVisibility() == KnowledgeVisibility.GLOBAL
                    || enterpriseId.equals(knowledgeBase.getEnterpriseId());
            if (!accessible) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
            }
            // 状态校验：仅 COMPLETED 文档可绑定和检索。
            if (knowledgeBase.getVectorStatus() != VectorStatus.COMPLETED) {
                // 90xxx 缺少"知识库未完成向量化不可绑定"专用错误码，暂用参数校验错误携带提示。
                throw new BusinessException(ErrorCode.PARAM_VALID_ERROR,
                        "仅向量化完成的知识库可绑定");
            }
        }
        return distinctIds;
    }

    private void replaceSessionBindings(Long sessionId, List<Long> knowledgeBaseIds) {
        // @TableLogic 生效：delete 转为逻辑删除，部分唯一索引放行后续重复绑定。
        ragSessionKnowledgeBaseMapper.delete(
                Wrappers.<RagSessionKnowledgeBase>lambdaQuery()
                        .eq(RagSessionKnowledgeBase::getSessionId, sessionId));
        knowledgeBaseIds.forEach(kbId -> ragSessionKnowledgeBaseMapper.insert(
                RagSessionKnowledgeBase.builder()
                        .sessionId(sessionId)
                        .knowledgeBaseId(kbId)
                        .build()));
    }

    private void replaceTemplateBindings(Long templateId, List<Long> knowledgeBaseIds) {
        // @TableLogic 生效：delete 转为逻辑删除，部分唯一索引放行后续重复绑定。
        interviewTemplateKnowledgeBaseMapper.delete(
                Wrappers.<InterviewTemplateKnowledgeBase>lambdaQuery()
                        .eq(InterviewTemplateKnowledgeBase::getTemplateId, templateId));
        knowledgeBaseIds.forEach(kbId -> interviewTemplateKnowledgeBaseMapper.insert(
                InterviewTemplateKnowledgeBase.builder()
                        .templateId(templateId)
                        .knowledgeBaseId(kbId)
                        .build()));
    }

    private Map<Long, Long> countMessagesBySessionIds(List<Long> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }
        return ragChatMessageMapper.selectList(
                        Wrappers.<RagChatMessage>lambdaQuery()
                                .select(RagChatMessage::getSessionId)
                                .in(RagChatMessage::getSessionId, sessionIds))
                .stream()
                .collect(Collectors.groupingBy(
                        RagChatMessage::getSessionId, Collectors.counting()));
    }
}
