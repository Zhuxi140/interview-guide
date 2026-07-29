package interview.ai.config.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "AI 场景启停响应")
public record LlmSceneStatusVO(

        @Schema(description = "场景编码", example = "RESUME_ANALYSIS")
        String sceneCode,

        @Schema(description = "场景开关")
        Boolean enabled,

        @Schema(description = "乐观锁版本")
        Integer version,

        @Schema(description = "最后更新时间")
        OffsetDateTime updatedAt

) {
}
