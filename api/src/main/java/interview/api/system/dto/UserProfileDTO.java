package interview.api.system.dto;

import java.time.OffsetDateTime;

/**
 * 跨模块使用的用户基础资料快照。
 */
public record UserProfileDTO(
        Long userId,
        String displayName,
        String email,
        OffsetDateTime updatedAt
) {
}
