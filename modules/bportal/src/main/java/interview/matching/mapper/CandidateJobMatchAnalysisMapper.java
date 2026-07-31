package interview.matching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.matching.model.entity.CandidateJobMatchAnalysis;
import interview.matching.model.bo.CandidateJobMatchExecutionBO;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/**
 * 候选人岗位适配预测 Mapper。
 */
public interface CandidateJobMatchAnalysisMapper extends BaseMapper<CandidateJobMatchAnalysis> {

    CandidateJobMatchExecutionBO claimAnalysis(
            @Param("analysisId") Long analysisId,
            @Param("maxAttempts") int maxAttempts,
            @Param("now") OffsetDateTime now,
            @Param("deadline") OffsetDateTime deadline,
            @Param("traceId") String traceId);

    int completeAnalysis(@Param("analysisId") Long analysisId,
                         @Param("attemptCount") int attemptCount,
                         @Param("matchScore") Integer matchScore,
                         @Param("passProbability") Integer passProbability,
                         @Param("strengthsJson") String strengthsJson,
                         @Param("gapsJson") String gapsJson,
                         @Param("snapshotJson") String snapshotJson,
                         @Param("traceId") String traceId,
                         @Param("now") OffsetDateTime now);

    int releaseAnalysisForRetry(@Param("analysisId") Long analysisId,
                                @Param("attemptCount") int attemptCount,
                                @Param("failureReason") String failureReason,
                                @Param("traceId") String traceId,
                                @Param("now") OffsetDateTime now);

    int failAnalysis(@Param("analysisId") Long analysisId,
                     @Param("attemptCount") int attemptCount,
                     @Param("failureReason") String failureReason,
                     @Param("traceId") String traceId,
                     @Param("now") OffsetDateTime now);

    int failWaitingAnalysis(@Param("analysisId") Long analysisId,
                            @Param("failureReason") String failureReason,
                            @Param("traceId") String traceId,
                            @Param("now") OffsetDateTime now);

    int failExhaustedAnalysis(@Param("analysisId") Long analysisId,
                              @Param("maxAttempts") int maxAttempts,
                              @Param("failureReason") String failureReason,
                              @Param("traceId") String traceId,
                              @Param("now") OffsetDateTime now);
}
