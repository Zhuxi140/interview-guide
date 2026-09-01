package interview.textinterview.listener;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import interview.api.aicore.InterviewQuestionAiApi;
import interview.api.aicore.dto.InterviewQuestionGeneratedResultDTO;
import interview.api.aicore.dto.InterviewQuestionGenerationReqDTO;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.textinterview.event.InterviewSessionReadyEvent;
import interview.textinterview.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 面试会话事件监听器：处理就绪事件并异步触发首题生成与落库
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionEventListener {

    private final InterviewScheduleQueryApi interviewScheduleQueryApi;
    private final InterviewQuestionAiApi interviewQuestionAiApi;
    private final InterviewSessionService interviewSessionService;

    /**
     * 监听会话就绪事件：事务提交后异步触发 AI 首题生成与数据落库
     *
     * @param event 面试会话就绪事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("interviewQuestionExecutor")
    public void handleSessionReady(InterviewSessionReadyEvent event) {
        try {
            // 步骤 1：读取排期与模板快照信息组装出题请求 DTO
            InterviewScheduleQueryDTO scheduleDTO = interviewScheduleQueryApi.getSchedule(event.scheduleId());
            String jobTitle = scheduleDTO != null ? scheduleDTO.jobTitle() : null;
            String phaseCode = scheduleDTO != null ? scheduleDTO.phaseCode() : null;

            String difficulty = null;
            String promptOverride = null;
            List<String> focusPoints = null;

            // 动态解析排期固化的模板阶段快照（完全以企业真实配置为准）
            String snapshotJson = interviewScheduleQueryApi.getTemplateSnapshot(event.scheduleId());
            if (StrUtil.isNotBlank(snapshotJson)) {
                try {
                    JSONObject snapshot = JSONUtil.parseObj(snapshotJson);
                    JSONArray stages = snapshot.getJSONArray("stages");
                    if (stages != null && !stages.isEmpty()) {
                        for (int i = 0; i < stages.size(); i++) {
                            JSONObject stage = stages.getJSONObject(i);
                            if (phaseCode != null && phaseCode.equalsIgnoreCase(stage.getStr("phaseCode"))) {
                                difficulty = stage.getStr("difficulty", stage.getStr("difficultyWeight"));
                                promptOverride = stage.getStr("promptOverride");
                                focusPoints = stage.getBeanList("focusPoints", String.class);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析排期模板快照异常: scheduleId={}", event.scheduleId(), e);
                }
            }

            // ==========================================
            // TODO RAG 增强：待知识库检索模块就绪后，按需检索专属题库知识库并注入
            // ==========================================
            String kind = event.questionKind() != null
                    ? event.questionKind().name()
                    : interview.common.enums.QuestionKind.FIRST.name();

            InterviewQuestionGenerationReqDTO reqDTO = new InterviewQuestionGenerationReqDTO(
                    event.enterpriseId(),
                    phaseCode,
                    promptOverride,
                    focusPoints,
                    difficulty,
                    jobTitle,
                    null,
                    null,
                    null,
                    kind,
                    0,
                    null,
                    null,
                    null
            );

            // 步骤 2：跨模块调用 aicore 出题服务
            InterviewQuestionGeneratedResultDTO generated = interviewQuestionAiApi.generateQuestion(reqDTO);
            if (generated == null || StrUtil.isBlank(generated.content())) {
                log.error("AI 出题服务返回空题目，终止首题落库: sessionId={}", event.sessionId());
                return;
            }

            // 步骤 3：调用 Service 代理层事务方法落库题目与时间线事件（保证 Spring @Transactional 事务生效）
            Long answerId = interviewSessionService.recordAiQuestion(
                    event.sessionId(),
                    event.enterpriseId(),
                    0,
                    null,
                    0,
                    generated
            );
            log.info("首题生成与事务落库成功: sessionId={}, answerId={}, content={}",
                    event.sessionId(), answerId, generated.content());

            // 步骤 4：若触发了兜底生成，触发通知通知 HR 接管面试
            if ("FALLBACK".equalsIgnoreCase(generated.rawModelResponse())) {
                log.warn("AI 首题生成触发兜底，准备通知 HR 面试官介入接管: sessionId={}, scheduleId={}",
                        event.sessionId(), event.scheduleId());
                // ==========================================
                // TODO：发送系统通知/WebSocket 提醒 HR 考场异常，切换为真人 HR 接管问答模式
                // ==========================================
            }

            // ==========================================
            // TODO 步骤 5：实时通道（WebSocket）广播推题（若候选人已在线）
            // ==========================================
        } catch (Exception e) {
            log.error("异步生成面试首题发生未捕获异常: sessionId={}, scheduleId={}",
                    event.sessionId(), event.scheduleId(), e);
        }
    }
}
