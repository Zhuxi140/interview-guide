package interview.candidate.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "候选人基础资料更新请求")
public class CandidateProfileUpdateReq {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能小于 0")
    @Schema(description = "当前资料版本", example = "0")
    private Integer expectedVersion;

    @Size(min = 1, max = 64, message = "展示名称长度应为 1~64")
    @Schema(description = "展示名称")
    private String displayName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    @Schema(description = "联系邮箱；空字符串表示清空")
    private String email;

    @Size(max = 64, message = "期望城市长度不能超过 64")
    @Schema(description = "期望工作城市")
    private String city;

    @Size(max = 128, message = "教育背景长度不能超过 128")
    @Schema(description = "教育背景摘要")
    private String education;

    @Size(max = 1000, message = "个人简介长度不能超过 1000")
    @Schema(description = "个人简介")
    private String summary;
}
