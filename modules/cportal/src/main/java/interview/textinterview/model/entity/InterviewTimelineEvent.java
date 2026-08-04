package interview.textinterview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 面试会话中可持久化和回放的语义事件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "interview_timeline_events", autoResultMap = true)
public class InterviewTimelineEvent implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long enterpriseId;

    private String eventId;

    private Long sequenceNum;

    private String eventType;

    private String actorType;

    private Long actorUserId;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String payloadJson;

    private OffsetDateTime occurredAt;

    private String traceId;
}
