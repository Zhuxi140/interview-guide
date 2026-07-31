package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.common.enums.ScreeningRecommendation;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 投递列表项响应（HR 端投递列表专用）
 */
@Schema(description = "投递列表项响应（HR 端投递列表专用）")
public record JobApplicationListItemVO(
        @Schema(description = "投递ID")
        Long id,
        @Schema(description = "候选人ID")
        Long candidateId,
        @Schema(description = "候选人姓名")
        String candidateName,
        @Schema(description = "简历文件名")
        String resumeFileName,
        @Schema(description = "最新 HR AI 初筛匹配分")
        Integer aiScreeningScore,
        @Schema(description = "最新 HR AI 初筛建议")
        ScreeningRecommendation aiRecommendation,
        @Schema(description = "投递状态")
        JobApplicationStatus status,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
