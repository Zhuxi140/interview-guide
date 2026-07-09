package interview.system.tenant.model.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Builder
public record TeamMemberItemBO(
        Long id,
        Long userId,
        String username,
        String nickname,
        String email,
        Integer roleId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
)
{}
