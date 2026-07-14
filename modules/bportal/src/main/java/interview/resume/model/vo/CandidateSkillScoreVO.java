package interview.resume.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 技能评分项响应
 */
@Builder
public record CandidateSkillScoreVO(
        Long id,
        String dimensionCode,
        Integer score,
        String aiJustification,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {
}
