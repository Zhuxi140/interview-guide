package interview.api.system;

import java.util.List;
import java.util.Map;

/**
 * 企业/租户对外校验 API
 * 由 system 模块实现，供其他模块（bportal, cportal 等）调用
 * @author zhuxi
 */
public interface EnterpriseValidationApi {

    /**
     * 校验企业存在且当前用户为企业成员
     * @param enterpriseId 企业 ID
     */
    void validateEnterpriseBelong(Long enterpriseId,Long userId);

    Map<Long,String> getNameList(List<Long> enterpriseId);
}
