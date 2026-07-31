package interview.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.resume.model.entity.Resumes;
import interview.resume.model.enums.AnalyzeStatus;
import interview.resume.model.bo.ResumeAnalysisExecutionBO;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 简历底座表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
@Mapper
public interface ResumesMapper extends BaseMapper<Resumes> {

    @Select("""
    SELECT
        storage_url
    FROM resumes
    WHERE is_deleted = true
      AND user_id = #{userId}
      AND file_hash = #{hash}
      AND analyze_status <> #{excludedStatus}
    ORDER BY updated_at DESC NULLS LAST, created_at DESC
    LIMIT 1
    """)
    String selectReusableDeletedByHash(@Param("hash") String hash,
                                       @Param("userId") Long userId,
                                       @Param("excludedStatus") AnalyzeStatus excludedStatus);

    @Select("SELECT pg_advisory_xact_lock(#{namespace}, #{ownerSlot})")
    void lockResumes(@Param("namespace") int namespace, @Param("ownerSlot") int ownerSlot);

    @Select("""
    SELECT *
    FROM resumes
    WHERE id = #{resumeId}
    """)
    Resumes selectIncludingDeletedById(@Param("resumeId") Long resumeId);

    @Select("""
    SELECT EXISTS(
        SELECT 1
        FROM resumes
        WHERE storage_url = #{storageUrl}
          AND id <> #{resumeId}
          AND is_deleted = false
    )
    """)
    boolean existsOtherAliveByStorageUrl(@Param("storageUrl") String storageUrl,
                                         @Param("resumeId") Long resumeId);

    /**
     * 将新分析消息绑定到简历。
     *
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param expectedStatus 绑定前状态
     * @param expectedMessageId 绑定前消息 ID
     * @param messageId 新消息 ID
     * @param idempotencyKeyHash 幂等键摘要
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int bindAnalysisRequest(@Param("resumeId") Long resumeId,
                            @Param("userId") Long userId,
                            @Param("expectedStatus") AnalyzeStatus expectedStatus,
                            @Param("expectedMessageId") Long expectedMessageId,
                            @Param("messageId") Long messageId,
                            @Param("idempotencyKeyHash") String idempotencyKeyHash,
                            @Param("traceId") String traceId,
                            @Param("now") OffsetDateTime now);

    /**
     * 原子领取待执行或已超时的简历分析。
     *
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param messageId 消息 ID
     * @param maxAttempts 最大执行次数
     * @param now 当前时间
     * @param deadline 新执行截止时间
     * @param traceId 调用链 ID
     * @return 执行上下文；未领取到返回 null
     */
    ResumeAnalysisExecutionBO claimAnalysis(@Param("resumeId") Long resumeId,
                                            @Param("userId") Long userId,
                                            @Param("messageId") Long messageId,
                                            @Param("maxAttempts") int maxAttempts,
                                            @Param("now") OffsetDateTime now,
                                            @Param("deadline") OffsetDateTime deadline,
                                            @Param("traceId") String traceId);

    /**
     * 按执行次数栅栏完成简历分析。
     *
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param messageId 消息 ID
     * @param attemptCount 执行次数
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int completeAnalysis(@Param("resumeId") Long resumeId,
                         @Param("userId") Long userId,
                         @Param("messageId") Long messageId,
                         @Param("attemptCount") int attemptCount,
                         @Param("traceId") String traceId,
                         @Param("now") OffsetDateTime now);

    /**
     * 将失败但仍可重试的分析恢复为待处理。
     *
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param messageId 消息 ID
     * @param attemptCount 执行次数
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int releaseAnalysisForRetry(@Param("resumeId") Long resumeId,
                                @Param("userId") Long userId,
                                @Param("messageId") Long messageId,
                                @Param("attemptCount") int attemptCount,
                                @Param("traceId") String traceId,
                                @Param("now") OffsetDateTime now);

    /**
     * 将达到重试上限的分析标记为失败。
     *
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param messageId 消息 ID
     * @param attemptCount 执行次数
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int failAnalysis(@Param("resumeId") Long resumeId,
                     @Param("userId") Long userId,
                     @Param("messageId") Long messageId,
                     @Param("attemptCount") int attemptCount,
                     @Param("traceId") String traceId,
                     @Param("now") OffsetDateTime now);

    /**
     * 收敛已达到上限且无人持有有效执行租约的分析。
     *
     * @param resumeId 简历 ID
     * @param userId 用户 ID
     * @param messageId 消息 ID
     * @param maxAttempts 最大执行次数
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int failExhaustedAnalysis(@Param("resumeId") Long resumeId,
                              @Param("userId") Long userId,
                              @Param("messageId") Long messageId,
                              @Param("maxAttempts") int maxAttempts,
                              @Param("traceId") String traceId,
                              @Param("now") OffsetDateTime now);
}
