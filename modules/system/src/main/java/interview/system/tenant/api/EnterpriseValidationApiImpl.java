package interview.system.tenant.api;

import cn.hutool.core.util.StrUtil;
import interview.api.system.EnterpriseValidationApi;
import interview.api.system.dto.EnterprisePublicProfileDTO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.service.EnterpriseTeamMembersService;
import interview.system.tenant.service.EnterprisesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhuxi
 */

@RequiredArgsConstructor
@Service
public class EnterpriseValidationApiImpl implements EnterpriseValidationApi {

    private final EnterprisesService enterprisesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;

    @Override
    public void validateEnterpriseBelong(Long enterpriseId,Long userId) {
        boolean exists = enterprisesService.lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        boolean exists1 = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();

        if (!exists1) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_ENTERPRISE_MEMBER);
        }

    }


    @Override
    public Map<Long,String> getNameList(List<Long> enterpriseId) {
        if (enterpriseId == null || enterpriseId.isEmpty()) {
            return Collections.emptyMap();
        }
        return enterprisesService.lambdaQuery()
                .select(Enterprise::getId,Enterprise::getName)
                .in(Enterprise::getId, enterpriseId)
                .list()
                .stream()
                .collect(Collectors.toMap(Enterprise::getId, Enterprise::getName));
    }

    @Override
    public void validateActiveEnterpriseBelong(Long enterpriseId, Long userId) {
        // 先校验企业和成员关系，避免向无关用户暴露企业经营状态。
        Enterprise enterprise = enterprisesService.lambdaQuery()
                .select(Enterprise::getId, Enterprise::getStatus)
                .eq(Enterprise::getId, enterpriseId)
                .one();
        if (enterprise == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }

        boolean member = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();
        if (!member) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_ENTERPRISE_MEMBER);
        }

        // 正式经营接口只允许已认证且未暂停的企业调用。
        if (enterprise.getStatus() == EnterpriseStatus.PENDING) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_CERTIFIED);
        }
        if (enterprise.getStatus() == EnterpriseStatus.PAUSED) {
            throw new BusinessException(ErrorCode.ENTERPRISE_FROZEN);
        }
        if (enterprise.getStatus() != EnterpriseStatus.NORMAL) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
    }

    @Override
    public List<EnterprisePublicProfileDTO> listPublicEnterprises(String industry) {
        // 只暴露正常、未删除的企业，并在 system 模块内完成行业筛选。
        return enterprisesService.lambdaQuery()
                .select(
                        Enterprise::getId,
                        Enterprise::getName,
                        Enterprise::getShortName,
                        Enterprise::getIndustry,
                        Enterprise::getScale,
                        Enterprise::getLogoUrl
                )
                .eq(Enterprise::getStatus, EnterpriseStatus.NORMAL)
                .eq(StrUtil.isNotBlank(industry), Enterprise::getIndustry, industry)
                .list()
                .stream()
                .map(enterprise -> new EnterprisePublicProfileDTO(
                        enterprise.getId(),
                        enterprise.getName(),
                        enterprise.getShortName(),
                        enterprise.getIndustry(),
                        enterprise.getScale(),
                        enterprise.getLogoUrl()
                ))
                .toList();
    }

    @Override
    public EnterprisePublicProfileDTO getPublicEnterprise(Long enterpriseId) {
        // 岗位详情只查询目标企业，避免加载全部可见企业。
        Enterprise enterprise = enterprisesService.lambdaQuery()
                .select(
                        Enterprise::getId,
                        Enterprise::getName,
                        Enterprise::getShortName,
                        Enterprise::getIndustry,
                        Enterprise::getScale,
                        Enterprise::getLogoUrl
                )
                .eq(Enterprise::getId, enterpriseId)
                .eq(Enterprise::getStatus, EnterpriseStatus.NORMAL)
                .one();
        if (enterprise == null) {
            return null;
        }
        return new EnterprisePublicProfileDTO(
                enterprise.getId(),
                enterprise.getName(),
                enterprise.getShortName(),
                enterprise.getIndustry(),
                enterprise.getScale(),
                enterprise.getLogoUrl()
        );
    }

    @Override
    public void validateEnterpriseMembers(Long enterpriseId, List<Long> userIds) {
        Long count = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .in(EnterpriseTeamMember::getUserId, userIds)
                .count();

        if (count != userIds.size()) {
            throw new BusinessException(ErrorCode.INTERVIEWER_NOT_ENTERPRISE_MEMBER);
        }
    }
}
