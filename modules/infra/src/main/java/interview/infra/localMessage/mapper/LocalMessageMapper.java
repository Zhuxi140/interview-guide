package interview.infra.localMessage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.infra.localMessage.model.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {

    /**
     * 原子领取到期消息并写入租约
     * @param priority 消息优先级
     * @param workerId 工作节点 ID
     * @param now 当前时间
     * @param leaseUntil 租约截止时间
     * @param limit 批次数量
     * @return 已领取且包含新租约版本的消息
     */
    @Select("""
            WITH candidates AS (
                SELECT id
                FROM local_message
                WHERE priority = #{priority}
                  AND (
                    (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= #{now}))
                    OR
                    (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= #{now}))
                  )
                ORDER BY created_at
                LIMIT #{limit}
                FOR UPDATE SKIP LOCKED
            )
            UPDATE local_message AS message
            SET status = 'PROCESSING',
                lease_owner = #{workerId},
                lease_until = #{leaseUntil},
                lease_version = COALESCE(message.lease_version, 0) + 1,
                updated_at = #{now}
            FROM candidates
            WHERE message.id = candidates.id
            RETURNING message.*
            """)
    List<LocalMessage> claimForDispatch(@Param("priority") String priority,
                                        @Param("workerId") String workerId,
                                        @Param("now") OffsetDateTime now,
                                        @Param("leaseUntil") OffsetDateTime leaseUntil,
                                        @Param("limit") int limit);
}
