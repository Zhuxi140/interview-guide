package interview.textinterview.service;

import static cn.hutool.json.JSONUtil.toJsonStr;

import java.time.OffsetDateTime;
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
    private static final long FIRST_QUESTION_SEQUENCE = 1L;

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

        // 推进会话事件序号，为时间线事件分配确定序号。
        interviewSessionMapper.update(
                null,
                Wrappers.<InterviewSession>lambdaUpdate()
                        .eq(InterviewSession::getId, command.sessionId())
                        .set(InterviewSession::getLastEventSequence, FIRST_QUESTION_SEQUENCE)
                        .set(InterviewSession::getUpdatedAt, OffsetDateTime.now()));

        // 保存可供前端回放的题目完成事件。
        Map<String, Object> payload = Map.of(
                "questionId", answer.getId(),
                "sequenceNum", FIRST_QUESTION_SEQUENCE,
                "questionKind", StrUtil.nullToDefault(command.questionKind(), "FIRST"),
                "content", command.content(),
                "assessmentPoint", StrUtil.nullToDefault(command.assessmentPoint(), ""),
                "difficulty", StrUtil.nullToDefault(command.difficulty(), "MEDIUM")
        );
        InterviewTimelineEvent timelineEvent = InterviewTimelineEvent.builder()
                .sessionId(command.sessionId())
                .enterpriseId(command.enterpriseId())
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .sequenceNum(FIRST_QUESTION_SEQUENCE)
                .eventType(QUESTION_COMPLETED_EVENT)
                .actorType("AI")
                .payloadJson(toJsonStr(payload))
                .occurredAt(OffsetDateTime.now())
                .build();
        interviewTimelineEventMapper.insert(timelineEvent);

        return answer.getId();
    }
}
