package interview.api.system;

import interview.api.system.dto.EnterprisePublicProfileDTO;

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
     *
     * @param enterpriseId 企业 ID
     * @param userId       当前用户 ID
     */
    void validateEnterpriseBelong(Long enterpriseId,Long userId);

    /**
     * 校验企业存在且处于正常经营(已认证)状态，且当前用户仍为企业成员
     *
     * @param enterpriseId 企业 ID
     * @param userId       当前用户 ID
     */
    void validateActiveEnterpriseBelong(Long enterpriseId, Long userId);

    /**
     * 批量查询企业名称
     *
     * @param enterpriseId 企业 ID 列表
     * @return 企业 ID 与名称映射
     */
    Map<Long,String> getNameList(List<Long> enterpriseId);

    /**
     * 查询允许对 C 端展示的正常企业
     *
     * @param industry 行业筛选，可为空
     * @return 企业公开信息
     */
    List<EnterprisePublicProfileDTO> listPublicEnterprises(String industry);

    /**
     * 查询单个允许对 C 端展示的正常企业
     *
     * @param enterpriseId 企业 ID
     * @return 企业公开信息；企业不可用时返回 null
     */
    EnterprisePublicProfileDTO getPublicEnterprise(Long enterpriseId);


    /**
     *  批量校验企业成员是否存在，且在同一个企业
     * @param enterpriseId 企业 ID
     * @param userIds 用户 ID 列表
     */
    void validateEnterpriseMembers(Long enterpriseId, List<Long> userIds);
}
