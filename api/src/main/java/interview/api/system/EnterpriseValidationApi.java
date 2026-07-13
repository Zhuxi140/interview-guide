package interview.api.system;

/**
 * 企业/租户对外校验 API
 * 由 system 模块实现，供其他模块（b-portal, c-portal 等）调用
 * @author zhuxi
 */
public interface EnterpriseValidationApi {

    /**
     * 校验企业存在且当前用户为企业成员
     * @param enterpriseId 企业 ID
     */
    void validateEnterpriseBelong(Long enterpriseId,Long userId);
}
