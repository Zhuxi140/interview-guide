package interview.system.tenant.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @apiNote 团队成员列表项响应
 * @since 2026/5/27 14:05
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
    LocalDateTime createdAt
) {}
