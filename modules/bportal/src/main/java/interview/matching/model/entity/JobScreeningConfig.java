package interview.matching.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 岗位 AI 初筛开关与阈值配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "job_screening_configs", autoResultMap = true)
public class JobScreeningConfig implements Serializable {

    @TableId
    private Long jobId;

    private Long enterpriseId;

    private Boolean enabled;

    private Integer overallThreshold;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String dimensionThresholds;

    @Version
    private Integer version;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
