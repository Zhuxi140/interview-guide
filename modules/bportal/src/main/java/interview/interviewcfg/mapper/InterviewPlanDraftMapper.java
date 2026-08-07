package interview.interviewcfg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.interviewcfg.model.bo.InterviewPlanGenerationExecutionBO;
import interview.interviewcfg.model.entity.InterviewPlanDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

@Mapper
public interface InterviewPlanDraftMapper extends BaseMapper<InterviewPlanDraft> {

    /**
     * 原子领取一次草案生成执行权，超时或未耗尽尝试次数时才可领取。
     *
     * @param draftId 草案 ID
     * @param messageId 生成消息 ID
     * @param maxAttempts 最大尝试次数
     * @param now 当前时间
     * @param deadline 本次执行截止时间
     * @param traceId 链路 ID
     * @return 执行上下文；未领取到返回 null
     */
    InterviewPlanGenerationExecutionBO claimGeneration(@Param("draftId") Long draftId,
                                                       @Param("messageId") Long messageId,
                                                       @Param("maxAttempts") int maxAttempts,
                                                       @Param("now") OffsetDateTime now,
                                                       @Param("deadline") OffsetDateTime deadline,
                                                       @Param("traceId") String traceId);

    /**
     * 按执行栅栏将草案写为 READY 并落地生成计划与失效时间。
     *
     * @param draftId 草案 ID
     * @param messageId 生成消息 ID
     * @param attemptCount 本次执行次数
     * @param planJson 生成计划
     * @param snapshotJson LLM 配置快照 JSON
     * @param expiresAt 到期时间
     * @param traceId 链路 ID
     * @param now 当前时间
     * @return 影响行数
     */
    int completeGeneration(@Param("draftId") Long draftId,
                           @Param("messageId") Long messageId,
                           @Param("attemptCount") Integer attemptCount,
                           @Param("planJson") String planJson,
                           @Param("snapshotJson") String snapshotJson,
                           @Param("expiresAt") OffsetDateTime expiresAt,
                           @Param("traceId") String traceId,
                           @Param("now") OffsetDateTime now);

    /**
     * 尝试次数耗尽后标记 FAILED。
     *
     * @param draftId 草案 ID
     * @param messageId 生成消息 ID
     * @param maxAttempts 最大尝试次数
     * @param failureReason 失败原因
     * @param traceId 链路 ID
     * @param now 当前时间
     * @return 影响行数
     */
    int failExhaustedGeneration(@Param("draftId") Long draftId,
                                @Param("messageId") Long messageId,
                                @Param("maxAttempts") int maxAttempts,
                                @Param("failureReason") String failureReason,
                                @Param("traceId") String traceId,
                                @Param("now") OffsetDateTime now);

    /**
     * 本次执行失败按栅栏标记 FAILED。
     *
     * @param draftId 草案 ID
     * @param messageId 生成消息 ID
     * @param attemptCount 本次执行次数
     * @param failureReason 失败原因
     * @param traceId 链路 ID
     * @param now 当前时间
     * @return 影响行数
     */
    int failGeneration(@Param("draftId") Long draftId,
                       @Param("messageId") Long messageId,
                       @Param("attemptCount") Integer attemptCount,
                       @Param("failureReason") String failureReason,
                       @Param("traceId") String traceId,
                       @Param("now") OffsetDateTime now);

    /**
     * 本次执行失败后释放租约等待后续重试。
     *
     * @param draftId 草案 ID
     * @param messageId 生成消息 ID
     * @param attemptCount 本次执行次数
     * @param traceId 链路 ID
     * @param now 当前时间
     * @return 影响行数
     */
    int releaseGenerationForRetry(@Param("draftId") Long draftId,
                                  @Param("messageId") Long messageId,
                                  @Param("attemptCount") Integer attemptCount,
                                  @Param("traceId") String traceId,
                                  @Param("now") OffsetDateTime now);

    /**
     * 超过有效期的活动草案原子推进为 EXPIRED，释放同投递的活动草案位。
     *
     * @param draftId 草案 ID
     * @param traceId 链路 ID
     * @param now 当前时间
     * @return 影响行数
     */
    int expireDraft(@Param("draftId") Long draftId,
                    @Param("traceId") String traceId,
                    @Param("now") OffsetDateTime now);
}