package interview.offer.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "Offer 草稿修改请求")
public record OfferUpdateReq(
        @NotNull(message = "乐观锁版本号不能为空")
        @Min(value = 0, message = "乐观锁版本号不能小于0")
        @Schema(description = "客户端读取到的版本号", example = "0")
        Integer expectedVersion,

        @Size(min = 1, max = 128, message = "Offer标题长度必须为1到128个字符")
        @Schema(description = "Offer标题")
        String title,

        @DecimalMin(value = "0", message = "最低薪资不能小于0")
        @Schema(description = "最低薪资")
        BigDecimal salaryMin,

        @DecimalMin(value = "0", message = "最高薪资不能小于0")
        @Schema(description = "最高薪资")
        BigDecimal salaryMax,

        @Pattern(regexp = "^[A-Z]{3}$", message = "币种必须使用三位大写ISO代码")
        @Schema(description = "币种代码", example = "CNY")
        String currency,

        @FutureOrPresent(message = "计划入职日期不能早于今天")
        @Schema(description = "计划入职日期")
        LocalDate plannedStartDate,

        @Future(message = "Offer过期时间必须晚于当前时间")
        @Schema(description = "Offer过期时间")
        OffsetDateTime expiresAt,

        @Size(min = 1, max = 20000, message = "Offer内容长度必须为1到20000个字符")
        @Schema(description = "Offer正文")
        String content
) {
}
