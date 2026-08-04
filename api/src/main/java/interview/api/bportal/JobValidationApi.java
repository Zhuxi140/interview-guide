package interview.api.bportal;

import interview.api.aicore.dto.JobApplicationSnapshotDTO;
import interview.common.exception.BusinessException;

/**
 * 岗位对外校验 API
 * 由 bportal 模块实现，供其他模块（system 等）调用
 * @author zhuxi
 */
public interface JobValidationApi {

    /**
     * 校验企业下是否存在 OPEN 状态的活跃岗位
     * @param enterpriseId 企业 ID
     * @return true 存在活跃岗位，false 无活跃岗位
     */
    boolean hasActiveJobs(Long enterpriseId);


    /**
     * 校验投递存在、属于指定企业且状态为 PASSED，并返回排期所需的候选人/岗位快照
     * @param applicationId 投递 ID
     * @param enterpriseId 企业 ID（租户隔离校验）
     * @return 投递快照（enterpriseId/jobId/candidateId）
     * @throws BusinessException 投递不存在、不属于该企业或状态非 PASSED
     */
    JobApplicationSnapshotDTO requirePassedApplication(Long applicationId, Long enterpriseId);

    /**
     * 校验投递存在、属于指定企业且状态为 PASSED，返回包含 candidateUserId 的完整快照
     * （用于 Offer 创建等需要候选人用户 ID 的场景）
     * @param applicationId 投递 ID
     * @param enterpriseId 企业 ID
     * @return 投递快照（enterpriseId/jobId/candidateUserId）
     * @throws BusinessException 投递不存在、不属于该企业或状态非 PASSED
     */
    JobApplicationSnapshotDTO requirePassedApplicationWithInfo(Long applicationId, Long enterpriseId);
}
