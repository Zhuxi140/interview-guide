package interview.offer.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "Offer 草稿创建请求")
public record OfferCreateReq(
        @NotBlank(message = "Offer标题不能为空")
        @Size(max = 128, message = "Offer标题长度不能超过128个字符")
        @Schema(description = "Offer标题", example = "Java后端工程师录用通知")
        String title,

        @DecimalMin(value = "0", message = "最低薪资不能小于0")
        @Schema(description = "最低薪资", example = "18000")
        BigDecimal salaryMin,

        @DecimalMin(value = "0", message = "最高薪资不能小于0")
        @Schema(description = "最高薪资", example = "25000")
        BigDecimal salaryMax,

        @Pattern(regexp = "^[A-Z]{3}$", message = "币种必须使用三位大写ISO代码")
        @Schema(description = "币种代码", example = "CNY")
        String currency,

        @FutureOrPresent(message = "计划入职日期不能早于今天")
        @Schema(description = "计划入职日期", example = "2026-09-01")
        LocalDate plannedStartDate,

        @NotNull(message = "Offer过期时间不能为空")
        @Future(message = "Offer过期时间必须晚于当前时间")
        @Schema(description = "Offer过期时间")
        OffsetDateTime expiresAt,

        @NotBlank(message = "Offer内容不能为空")
        @Size(max = 20000, message = "Offer内容长度不能超过20000个字符")
        @Schema(description = "Offer正文")
        String content
) {
}
