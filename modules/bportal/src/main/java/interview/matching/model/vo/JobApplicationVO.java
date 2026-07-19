package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 投递记录详情响应
 */
@Builder
@Schema(description = "投递记录详情响应")
public record JobApplicationVO(
        @Schema(description = "投递ID")
        Long id,
        @Schema(description = "企业ID")
        Long enterpriseId,
        @Schema(description = "职位ID")
        Long jobId,
        @Schema(description = "候选人ID")
        Long candidateId,
        @Schema(description = "候选人姓名")
        String candidateName,
        @Schema(description = "简历ID")
        Long resumeId,
        @Schema(description = "简历文件名")
        String resumeFileName,
        @Schema(description = "AI 匹配评分")
        Integer aiMatchScore,
        @Schema(description = "投递状态")
        JobApplicationStatus status,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @Schema(description = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
