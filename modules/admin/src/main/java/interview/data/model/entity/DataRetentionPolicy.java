package interview.data.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.framework.mybatis.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 数据保留策略单行配置实体；policiesJson 为资源策略数组。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "data_retention_policies", autoResultMap = true)
public class DataRetentionPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 固定单行，id 恒为 1。
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 策略数组 JSON：[{resourceType, hotRetentionDays, archiveEnabled, coldRetentionDays}]。
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String policiesJson;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableLogic
    private Boolean isDeleted;
}
