package interview.candidate.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "候选人脱敏联系方式")
public record CandidateContactMaskedVO(
        @Schema(description = "脱敏手机号") String phone,
        @Schema(description = "脱敏邮箱") String email
) {
}
