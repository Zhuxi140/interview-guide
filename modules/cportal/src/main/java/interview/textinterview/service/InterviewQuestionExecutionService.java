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
import interview.api.system.NotificationApi;
import interview.api.system.dto.SendNotificationCommand;
import interview.common.enums.ChannelType;
import interview.common.enums.NotifyScene;
import interview.common.enums.QuestionKind;
import interview.textinterview.event.InterviewSessionReadyEvent;
import interview.textinterview.model.command.InterviewQuestionRecordCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * 面试题生成执行服务，负责组织出题上下文、调用 AI 并持久化生成结果。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewQuestionExecutionService {

    private final InterviewScheduleQueryApi interviewScheduleQueryApi;
    private final InterviewQuestionAiApi interviewQuestionAiApi;
    private final InterviewQuestionTxService interviewQuestionTxService;
    private final NotificationApi notificationApi;

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
        Long answerId = interviewQuestionTxService.recordQuestion(
                new InterviewQuestionRecordCommand(
                        event.sessionId(),
                        event.enterpriseId(),
                        0,
                        null,
                        0,
                        generated.content(),
                        generated.questionKind(),
                        generated.assessmentPoint(),
                        generated.difficulty()
                ));
        log.info("首题生成与事务落库成功: sessionId={}, answerId={}, content={}",
                event.sessionId(), answerId, generated.content());

        // AI 触发兜底时通知面试官切换为人工接管：收件人取排期面试官，
        // 无面试官排期仅告警跳过（通知为尽力而为，不阻塞首题落库结果）。
        if ("FALLBACK".equalsIgnoreCase(generated.rawModelResponse())) {
            log.warn("AI 首题生成触发兜底，准备通知 HR 面试官介入接管: sessionId={}, scheduleId={}",
                    event.sessionId(), event.scheduleId());
            if (schedule != null && schedule.interviewerUserId() != null) {
                notificationApi.send(new SendNotificationCommand(
                        event.enterpriseId(),
                        schedule.interviewerUserId(),
                        NotifyScene.SYSTEM,
                        Set.of(ChannelType.IN_APP),
                        Map.of("title", "AI 出题触发兜底",
                                "content", "面试会话 " + event.sessionId() + " 的 AI 首题生成已触发兜底，"
                                        + "请尽快进入会话切换为人工接管模式。"),
                        "INTERVIEW_FALLBACK:" + event.sessionId(),
                        null));
            } else {
                log.warn("兜底通知跳过：排期缺失或未指定面试官 scheduleId={}", event.scheduleId());
            }
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
