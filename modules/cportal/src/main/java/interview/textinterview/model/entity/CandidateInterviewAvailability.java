package interview.textinterview.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 候选人可面试时间聚合配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "candidate_interview_availability", autoResultMap = true)
public class CandidateInterviewAvailability implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "candidate_user_id", type = IdType.INPUT)
    private Long candidateUserId;

    private String timezone;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String rangesJson;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
