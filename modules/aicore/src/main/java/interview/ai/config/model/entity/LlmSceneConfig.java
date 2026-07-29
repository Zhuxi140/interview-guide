package interview.ai.config.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import interview.common.enums.AiModelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "llm_scene_config")
public class LlmSceneConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "scene_code", type = IdType.INPUT)
    private String sceneCode;

    private AiModelType modelType;

    private String providerId;

    private BigDecimal temperature;

    private BigDecimal topP;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    @Builder.Default
    private Integer timeoutSeconds = 60;

    private String promptVersion;

    @Builder.Default
    private String extraOptions = "{}";

    @Builder.Default
    private Boolean enabled = true;

    @Version
    @Builder.Default
    private Integer version = 0;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String traceId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
