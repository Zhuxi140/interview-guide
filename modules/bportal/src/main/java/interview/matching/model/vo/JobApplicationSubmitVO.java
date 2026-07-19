package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 候选人投递简历响应
 */
@Builder
@Schema(description = "候选人投递简历响应")
public record JobApplicationSubmitVO(
        @Schema(description = "投递ID")
        Long id,
        @Schema(description = "企业ID")
        Long enterpriseId,
        @Schema(description = "职位ID")
        Long jobId,
        @Schema(description = "投递状态")
        JobApplicationStatus status,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
