package interview.interviewcfg.calendar.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 面试官日历空闲时段实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "interview_calendar_slots")
public class InterviewCalendarSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private Long interviewerUserId;

    private OffsetDateTime slotStart;

    private OffsetDateTime slotEnd;

    /**
     * FALSE: 空闲开放中, TRUE: 已排期锁定。
     */
    @Builder.Default
    private Boolean isAllocated = false;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableLogic
    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
