package interview.textinterview.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import interview.api.aicore.InterviewQuestionAiApi;
import interview.api.aicore.dto.InterviewQuestionGeneratedResultDTO;
import interview.api.aicore.dto.InterviewQuestionGenerationReqDTO;
import interview.api.bportal.InterviewScheduleQueryApi;
import interview.api.bportal.dto.InterviewScheduleQueryDTO;
import interview.common.enums.QuestionKind;
import interview.textinterview.event.InterviewSessionReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 面试题生成执行服务，负责组织出题上下文、调用 AI 并持久化生成结果。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionExecutionService {

    private final InterviewScheduleQueryApi interviewScheduleQueryApi;
    private final InterviewQuestionAiApi interviewQuestionAiApi;
    private final InterviewSessionService interviewSessionService;

    /**
     * 根据会话就绪事件生成并保存首题。
     *
     * @param event 面试会话就绪事件
     */
    public void generateFirstQuestion(InterviewSessionReadyEvent event) {
        // 读取排期和模板快照，构造本轮面试的出题上下文。
        InterviewScheduleQueryDTO schedule = interviewScheduleQueryApi.getSchedule(event.scheduleId());
        String phaseCode = schedule == null ? null : schedule.phaseCode();
        QuestionContext context = resolveQuestionContext(event.scheduleId(), phaseCode);
        String questionKind = event.questionKind() == null
                ? QuestionKind.FIRST.name()
                : event.questionKind().name();

        InterviewQuestionGenerationReqDTO request = new InterviewQuestionGenerationReqDTO(
                event.enterpriseId(),
                phaseCode,
                context.promptOverride(),
                context.focusPoints(),
                context.difficulty(),
                schedule == null ? null : schedule.jobTitle(),
                null,
                null,
                null,
                questionKind,
                0,
                null,
                null,
                null
        );

        // 调用 AI 生成首题；空结果不进入持久化流程。
        InterviewQuestionGeneratedResultDTO generated = interviewQuestionAiApi.generateQuestion(request);
        if (generated == null || StrUtil.isBlank(generated.content())) {
            log.error("AI 出题服务返回空题目，终止首题落库: sessionId={}", event.sessionId());
            return;
        }

        // 通过事务 Service 保存题目并追加时间线事件。
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

        // AI 触发兜底时预留通知 HR 接管的扩展点。
        if ("FALLBACK".equalsIgnoreCase(generated.rawModelResponse())) {
            log.warn("AI 首题生成触发兜底，准备通知 HR 面试官介入接管: sessionId={}, scheduleId={}",
                    event.sessionId(), event.scheduleId());
            // TODO：发送系统通知/WebSocket 提醒 HR 切换为人工接管模式。
        }

        // TODO：通过实时通道向已在线候选人广播生成的题目。
    }

    private QuestionContext resolveQuestionContext(Long scheduleId, String phaseCode) {
        String snapshotJson = interviewScheduleQueryApi.getTemplateSnapshot(scheduleId);
        if (StrUtil.isBlank(snapshotJson)) {
            return QuestionContext.empty();
        }
        try {
            JSONObject snapshot = JSONUtil.parseObj(snapshotJson);
            JSONArray stages = snapshot.getJSONArray("stages");
            if (stages == null || stages.isEmpty()) {
                return QuestionContext.empty();
            }
            for (int i = 0; i < stages.size(); i++) {
                JSONObject stage = stages.getJSONObject(i);
                if (phaseCode != null && phaseCode.equalsIgnoreCase(stage.getStr("phaseCode"))) {
                    return new QuestionContext(
                            stage.getStr("difficulty", stage.getStr("difficultyWeight")),
                            stage.getStr("promptOverride"),
                            stage.getBeanList("focusPoints", String.class)
                    );
                }
            }
        } catch (Exception e) {
            log.warn("解析排期模板快照异常: scheduleId={}", scheduleId, e);
        }
        return QuestionContext.empty();
    }

    private record QuestionContext(
            String difficulty,
            String promptOverride,
            List<String> focusPoints
    ) {
        private static QuestionContext empty() {
            return new QuestionContext(null, null, null);
        }
    }
}
