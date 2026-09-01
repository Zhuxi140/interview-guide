package interview.ai.question.api;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import interview.ai.llm.client.UnifiedChatClient;
import interview.ai.llm.model.AiResult;
import interview.api.aicore.InterviewQuestionAiApi;
import interview.api.aicore.dto.InterviewQuestionGeneratedResultDTO;
import interview.api.aicore.dto.InterviewQuestionGenerationReqDTO;
import interview.common.enums.AiSceneCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试 AI 出题跨模块接口实现（自适应各行业岗位）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionAiApiImpl implements InterviewQuestionAiApi {

    private final UnifiedChatClient unifiedChatClient;

    @Override
    public InterviewQuestionGeneratedResultDTO generateQuestion(InterviewQuestionGenerationReqDTO req) {
        String questionKind = StrUtil.nullToDefault(req.questionKind(), "FIRST");

        // ==========================================
        // TODO RAG 增强：待知识库向量检索模块就绪后，接入 enterprise 专属题库向量检索并注入上下文
        // ==========================================

        // 步骤 1：组装动态上下文入参 JSON（严格按实际传入参数，不强行注入假设）
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("enterpriseId", req.enterpriseId() != null ? req.enterpriseId() : 0L);
        inputMap.put("phaseCode", StrUtil.nullToDefault(req.phaseCode(), "INTERVIEW"));
        inputMap.put("promptOverride", StrUtil.nullToEmpty(req.promptOverride()));
        inputMap.put("focusPoints", req.focusPoints() != null ? req.focusPoints() : java.util.List.of());
        inputMap.put("difficulty", StrUtil.nullToDefault(req.difficulty(), "MEDIUM"));
        inputMap.put("jobTitle", StrUtil.nullToDefault(req.jobTitle(), "应聘岗位"));
        inputMap.put("jobRequirements", StrUtil.nullToEmpty(req.jobRequirements()));
        inputMap.put("candidateResumeSummary", StrUtil.nullToEmpty(req.candidateResumeSummary()));
        inputMap.put("questionKind", questionKind);
        inputMap.put("questionIndex", req.questionIndex() != null ? req.questionIndex() : 0);
        inputMap.put("previousQuestionText", StrUtil.nullToEmpty(req.previousQuestionText()));
        inputMap.put("previousUserAnswer", StrUtil.nullToEmpty(req.previousUserAnswer()));

        String userPrompt = JSONUtil.toJsonStr(inputMap);

        // 步骤 2：调用 UnifiedChatClient 发起大模型结构化生成
        try {
            AiResult<InterviewQuestionGeneratedResultDTO> result = unifiedChatClient.callStructured(
                    AiSceneCode.TEXT_INTERVIEW_QUESTION_GEN,
                    userPrompt,
                    InterviewQuestionGeneratedResultDTO.class
            );
            if (result != null && result.data() != null && StrUtil.isNotBlank(result.data().content())) {
                return result.data();
            }
        } catch (Exception e) {
            log.error("AI 生成面试题目异常，启用通用安全兜底. enterpriseId={}, jobTitle={}, questionKind={}",
                    req.enterpriseId(), req.jobTitle(), questionKind, e);
        }

        // 步骤 3：通用兜底保障（适配任意技术/非技术岗位）
        String fallbackContent = buildFallbackQuestion(req);
        String fallbackPoint = (req.focusPoints() != null && !req.focusPoints().isEmpty())
                ? req.focusPoints().get(0)
                : "岗位核心专业能力与过往项目实践";
        return new InterviewQuestionGeneratedResultDTO(
                fallbackContent,
                fallbackPoint,
                StrUtil.nullToDefault(req.difficulty(), "MEDIUM"),
                questionKind,
                "FALLBACK"
        );
    }

    private String buildFallbackQuestion(InterviewQuestionGenerationReqDTO req) {
        if (req.focusPoints() != null && !req.focusPoints().isEmpty()) {
            return String.format("请结合你的实际工作经验，详细谈谈你在 %s 方面的核心实践经验与处理过的典型场景。",
                    String.join("、", req.focusPoints()));
        }
        if (StrUtil.isNotBlank(req.jobTitle()) && !"应聘岗位".equals(req.jobTitle())) {
            return String.format("请结合你过往应聘【%s】相关的实际工作与项目经历，重点介绍一个最具代表性的核心成果，并阐述你在其中承担的关键职责与突破。",
                    req.jobTitle());
        }
        return "请结合你的实际工作与项目经历，简要介绍一个你深度参与的核心成果，重点说明你的角色职责、解决的关键问题以及取得的最终成效。";
    }
}
