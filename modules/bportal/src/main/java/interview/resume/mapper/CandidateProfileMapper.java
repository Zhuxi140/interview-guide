package interview.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.resume.model.entity.CandidateProfile;
import interview.resume.model.bo.CandidateProfileExecutionBO;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * <p>
 * 候选人画像头表 Mapper 接口
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
public interface CandidateProfileMapper extends BaseMapper<CandidateProfile> {

    /**
     * 原子领取待执行或已超时的人才画像任务。
     *
     * @param profileId 画像 ID
     * @param resumeId 简历 ID
     * @param candidateId 候选人 ID
     * @param maxAttempts 最大执行次数
     * @param now 当前时间
     * @param deadline 新执行截止时间
     * @param traceId 调用链 ID
     * @return 执行上下文；未领取到返回 null
     */
    CandidateProfileExecutionBO claimProfile(
            @Param("profileId") Long profileId,
            @Param("resumeId") Long resumeId,
            @Param("candidateId") Long candidateId,
            @Param("maxAttempts") int maxAttempts,
            @Param("now") OffsetDateTime now,
            @Param("deadline") OffsetDateTime deadline,
            @Param("traceId") String traceId);

    /**
     * 按执行栅栏完成人才画像。
     *
     * @param profileId 画像 ID
     * @param attemptCount 执行次数
     * @param summaryJson 画像摘要 JSON
     * @param snapshotJson 模型配置快照 JSON
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int completeProfile(@Param("profileId") Long profileId,
                        @Param("attemptCount") int attemptCount,
                        @Param("summaryJson") String summaryJson,
                        @Param("snapshotJson") String snapshotJson,
                        @Param("traceId") String traceId,
                        @Param("now") OffsetDateTime now);

    /**
     * 将失败但仍可重试的人才画像恢复为待处理。
     *
     * @param profileId 画像 ID
     * @param attemptCount 执行次数
     * @param failureReason 失败摘要
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int releaseProfileForRetry(@Param("profileId") Long profileId,
                               @Param("attemptCount") int attemptCount,
                               @Param("failureReason") String failureReason,
                               @Param("traceId") String traceId,
                               @Param("now") OffsetDateTime now);

    /**
     * 将达到重试上限的人才画像标记为失败。
     *
     * @param profileId 画像 ID
     * @param attemptCount 执行次数
     * @param failureReason 失败摘要
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int failProfile(@Param("profileId") Long profileId,
                    @Param("attemptCount") int attemptCount,
                    @Param("failureReason") String failureReason,
                    @Param("traceId") String traceId,
                    @Param("now") OffsetDateTime now);

    /**
     * 收敛已达到上限且无人持有有效租约的人才画像。
     *
     * @param profileId 画像 ID
     * @param maxAttempts 最大执行次数
     * @param failureReason 失败摘要
     * @param traceId 调用链 ID
     * @param now 当前时间
     * @return 更新行数
     */
    int failExhaustedProfile(@Param("profileId") Long profileId,
                             @Param("maxAttempts") int maxAttempts,
                             @Param("failureReason") String failureReason,
                             @Param("traceId") String traceId,
                             @Param("now") OffsetDateTime now);

}
