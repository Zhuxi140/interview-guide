package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 候选人画像（雷达图单项维度）响应
 */
@Builder
public record CandidateProfileVO(
        Long userId,
        String dimensionCode,
        Integer avgScore,
        String latestJustification,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime updatedAt
) {
}
