package interview.matching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.matching.model.entity.ApplicationAiScreening;
import interview.matching.model.bo.ApplicationScreeningExecutionBO;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * HR AI 初筛 Mapper。
 */
public interface ApplicationAiScreeningMapper extends BaseMapper<ApplicationAiScreening> {

    ApplicationScreeningExecutionBO claimScreening(
            @Param("screeningId") Long screeningId,
            @Param("maxAttempts") int maxAttempts,
            @Param("now") OffsetDateTime now,
            @Param("deadline") OffsetDateTime deadline,
            @Param("traceId") String traceId);

    int completeScreening(@Param("screeningId") Long screeningId,
                          @Param("attemptCount") int attemptCount,
                          @Param("overallMatchScore") Integer overallMatchScore,
                          @Param("dimensionMatchesJson") String dimensionMatchesJson,
                          @Param("recommendation") String recommendation,
                          @Param("snapshotJson") String snapshotJson,
                          @Param("traceId") String traceId,
                          @Param("now") OffsetDateTime now);

    int releaseScreeningForRetry(@Param("screeningId") Long screeningId,
                                 @Param("attemptCount") int attemptCount,
                                 @Param("failureReason") String failureReason,
                                 @Param("traceId") String traceId,
                                 @Param("now") OffsetDateTime now);

    int failScreening(@Param("screeningId") Long screeningId,
                      @Param("attemptCount") int attemptCount,
                      @Param("failureReason") String failureReason,
                      @Param("traceId") String traceId,
                      @Param("now") OffsetDateTime now);

    int failWaitingScreening(@Param("screeningId") Long screeningId,
                             @Param("failureReason") String failureReason,
                             @Param("traceId") String traceId,
                             @Param("now") OffsetDateTime now);

    int failExhaustedScreening(@Param("screeningId") Long screeningId,
                               @Param("maxAttempts") int maxAttempts,
                               @Param("failureReason") String failureReason,
                               @Param("traceId") String traceId,
                               @Param("now") OffsetDateTime now);
}
