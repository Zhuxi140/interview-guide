package interview.api.bportal;

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
}
