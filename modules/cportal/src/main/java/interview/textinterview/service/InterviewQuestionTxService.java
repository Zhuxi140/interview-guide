package interview.textinterview.service;

import static cn.hutool.json.JSONUtil.toJsonStr;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import cn.hutool.core.util.StrUtil;
import interview.textinterview.mapper.InterviewAnswerMapper;
import interview.textinterview.mapper.InterviewSessionMapper;
import interview.textinterview.mapper.InterviewTimelineEventMapper;
import interview.textinterview.model.command.InterviewQuestionRecordCommand;
import interview.textinterview.model.entity.InterviewAnswer;
import interview.textinterview.model.entity.InterviewSession;
import interview.textinterview.model.entity.InterviewTimelineEvent;
import lombok.RequiredArgsConstructor;

/**
 * AI 面试题短事务服务，负责写入题目、会话序号和时间线事实。
 */
@Service
@RequiredArgsConstructor
public class InterviewQuestionTxService {

    private static final String QUESTION_COMPLETED_EVENT = "question.completed";

    private final InterviewAnswerMapper interviewAnswerMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final InterviewTimelineEventMapper interviewTimelineEventMapper;

    /**
     * 在同一事务中记录 AI 题目及对应时间线事件。
     *
     * @param command 题目写入命令
     * @return 题目记录 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recordQuestion(InterviewQuestionRecordCommand command) {
        // 先分配会话内严格递增的事件序号，首题与追问题共用同一分配器。
        Long sequenceNum = nextEventSequence(command.sessionId());

        // 保存 AI 生成的题目。
        InterviewAnswer answer = InterviewAnswer.builder()
                .sessionId(command.sessionId())
                .enterpriseId(command.enterpriseId())
                .questionIndex(command.questionIndex())
                .questionText(command.content())
                .parentMessageId(command.parentAnswerId())
                .followUpDepth(command.followUpDepth())
                .build();
        interviewAnswerMapper.insert(answer);

        // 保存可供前端回放的题目完成事件；parentAnswerId 仅在追问场景有值，
        // 序列化为 JSON 时为空值会被剔除，非追问题目不出现该字段。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("questionId", answer.getId());
        payload.put("sequenceNum", sequenceNum);
        payload.put("questionKind", StrUtil.nullToDefault(command.questionKind(), "FIRST"));
        payload.put("content", command.content());
        payload.put("assessmentPoint", StrUtil.nullToDefault(command.assessmentPoint(), ""));
        payload.put("difficulty", StrUtil.nullToDefault(command.difficulty(), "MEDIUM"));
        payload.put("parentAnswerId", command.parentAnswerId());
        InterviewTimelineEvent timelineEvent = InterviewTimelineEvent.builder()
                .sessionId(command.sessionId())
                .enterpriseId(command.enterpriseId())
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .sequenceNum(sequenceNum)
                .eventType(QUESTION_COMPLETED_EVENT)
                .actorType("AI")
                .payloadJson(toJsonStr(payload))
                .occurredAt(OffsetDateTime.now())
                .build();
        interviewTimelineEventMapper.insert(timelineEvent);

        return answer.getId();
    }

    /**
     * 原子分配会话内下一个事件序号。
     *
     * <p>用数据库侧自增（{@code last_event_sequence = last_event_sequence + 1}）
     * 而不是先读后写：并发分配会在会话行锁上串行化，避免两个请求拿到同一序号后
     * 触发 {@code uk_interview_timeline_session_sequence} 唯一约束冲突。
     * 随后的回查处于同一事务内，读到的即本事务刚推进的序号。</p>
     *
     * @param sessionId 会话 ID
     * @return 本次分配的事件序号
     */
    private Long nextEventSequence(Long sessionId) {
        interviewSessionMapper.update(
                null,
                Wrappers.<InterviewSession>lambdaUpdate()
                        .eq(InterviewSession::getId, sessionId)
                        .setSql("last_event_sequence = last_event_sequence + 1")
                        .set(InterviewSession::getUpdatedAt, OffsetDateTime.now()));
        InterviewSession session = interviewSessionMapper.selectOne(
                Wrappers.<InterviewSession>lambdaQuery()
                        .select(InterviewSession::getLastEventSequence)
                        .eq(InterviewSession::getId, sessionId));
        return session.getLastEventSequence();
    }
}
