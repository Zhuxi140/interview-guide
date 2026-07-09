package interview.system.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @apiNote 团队成员列表项响应
 */
@Builder
public record TeamMemberItemVO(
    Long id,
    Long userId,
    String username,
    String nickname,
    String email,
    String roleCode,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    OffsetDateTime createdAt
) {}
