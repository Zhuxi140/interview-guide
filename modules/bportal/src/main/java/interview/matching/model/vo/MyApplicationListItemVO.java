package interview.matching.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import interview.matching.model.enums.JobApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 我的投递列表项响应（C 端投递记录专用）
 */
@Schema(description = "我的投递列表项响应（C 端投递记录专用）")
public record MyApplicationListItemVO(
        @Schema(description = "投递ID")
        Long id,
        @Schema(description = "职位ID")
        Long jobId,
        @Schema(description = "职位标题")
        String jobTitle,
        @Schema(description = "企业名称")
        String enterpriseName,
        @Schema(description = "本人最新岗位适配分")
        Integer matchScore,
        @Schema(description = "本人最新初筛通过率预测")
        Integer passProbability,
        @Schema(description = "投递状态")
        JobApplicationStatus status,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
