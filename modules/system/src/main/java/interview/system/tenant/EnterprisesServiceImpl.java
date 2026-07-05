package interview.system.tenant;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.Role;
import interview.common.exception.BusinessException;
import interview.framework.config.CustomIdGenerator;
import interview.framework.context.AuthContext;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.service.RolesService;
import interview.system.rbac.service.UserRolesService;
import interview.system.tenant.mapper.EnterprisesMapper;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.req.EnterpriseUpdateReq;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseListItemVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 服务实现类
 * </p>
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class EnterprisesServiceImpl extends ServiceImpl<EnterprisesMapper, Enterprise> implements EnterprisesService {

    private final CustomIdGenerator customIdGenerator;
    private final UserRolesService userRolesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;
    private final EnterprisesMapper enterprisesMapper;
    private final RolesService rolesService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public EnterpriseCreateBO createEnterprise(EnterpriseCreateReq req) {
        //校验企业名称唯一性
        boolean exists = lambdaQuery()
                .select(Enterprise::getName)
                .eq(Enterprise::getName, req.getName())
                .eq(Enterprise::getIsDeleted, false)
                .exists();

        if (exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NAME_ALREADY_EXISTS);
        }
        //构建 Enterprise 实体，填充雪花算法 ID
        Long number = (Long)customIdGenerator.nextId(Enterprise.class);
        Enterprise enterprise = Enterprise.builder()
                .id(number)
                .name(req.getName())
                .shortName(req.getShortName())
                .industry(req.getIndustry())
                .scale(req.getScale())
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .status(EnterpriseStatus.PENDING)
                .build();

        //INSERT enterprises
        try{
            save(enterprise);
        }catch (DataIntegrityViolationException e){
            // 企业名称已存在
            throw new BusinessException(ErrorCode.ENTERPRISE_NAME_ALREADY_EXISTS);
        }
        //在 enterprise_team_members 中创建一条 OWNER 记录（当前用户 + role_id=ENTERPRISE_OWNER）
        SysUserRole role = SysUserRole.builder()
                .userId(AuthContext.getUserId())
                .roleId(Role.ENTERPRISE_OWNER.getCode())
                .build();

        userRolesService.save(role);
        EnterpriseTeamMember enterpriseTeamMember = EnterpriseTeamMember.builder()
                .userId(AuthContext.getUserId())
                .roleId(Role.ENTERPRISE_OWNER.getCode())
                .enterpriseId(number)
                .build();

        enterpriseTeamMembersService.save(enterpriseTeamMember);

        //TODO:  Phase 8 扩展：status 默认置为 2（待认证），需配合 sys_enterprise_cert

        //返回 EnterpriseCreateBO
        return EnterpriseCreateBO.builder()
                .id(number)
                .name(enterprise.getName())
                .shortName(enterprise.getShortName())
                .industry(enterprise.getIndustry())
                .scale(enterprise.getScale())
                .status(enterprise.getStatus())
                .logoUrl(enterprise.getLogoUrl())
                .build();
    }

    @Override
    public List<ListUserEnterprisesBO> listUserEnterprises() {
        //从 AuthContext 获取当前 user_id
        Long userId = AuthContext.getUserId();
        //联表查询 enterprise_team_members 获取用户所属企业列表
        List<ListUserEnterprisesBO> bos = enterprisesMapper.getlistUserEnterprises(userId);
        if (bos.isEmpty()){
            return List.of();
        }
        //关联 sys_roles 获取 roleCode
        Set<Integer> roleIds = bos.stream()
                .map(ListUserEnterprisesBO::roleId)
                .collect(Collectors.toSet());

        Map<Integer, String> roleCodeMap = rolesService.lambdaQuery()
                .select(SysRole::getRoleCode)
                .in(SysRole::getId, roleIds)
                .list()
                .stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));

        //统计每个企业的 memberCount
        Set<Long> enterpriseIds = bos.stream()
                .map(ListUserEnterprisesBO::enterpriseId)
                .collect(Collectors.toSet());

        Map<Long, Long> memberCountMap = enterpriseTeamMembersService.lambdaQuery()
                .in(EnterpriseTeamMember::getEnterpriseId, enterpriseIds)
                .eq(EnterpriseTeamMember::getIsDeleted, false)
                .list()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                EnterpriseTeamMember::getEnterpriseId,
                                Collectors.counting()
                        )
                );

        //返回 List<ListUserEnterprisesBO>
        return bos.stream()
                .map(bo ->
                ListUserEnterprisesBO.builder()
                        .enterpriseId(bo.enterpriseId())
                        .name(bo.name())
                        .shortName(bo.shortName())
                        .status(bo.status())
                        .roleCode(roleCodeMap.get(bo.roleId()))
                        .memberCount(memberCountMap.get(bo.enterpriseId()))
                        .build()
                ).toList();
    }

    @Override
    public EnterpriseDetailVO getEnterpriseDetail(Long enterpriseId) {
        // TODO: 校验 enterpriseId 合法性（是否存在、is_deleted = false）
        //       租户隔离校验：当前用户必须属于该企业
        //       查询 Enterprise 实体
        //       转换为 EnterpriseDetailVO
        return null;
    }

    @Override
    public EnterpriseUpdateVO updateEnterprise(Long enterpriseId, EnterpriseUpdateReq req) {
        // TODO: 校验 enterpriseId 合法性
        //       校验操作人权限（仅 ADMIN 及以上可修改企业信息）
        //       仅更新非 null 字段（部分更新，不覆盖未传字段）
        //       UPDATE enterprises
        //       返回 EnterpriseUpdateVO
        return null;
    }

    @Override
    public void deleteEnterprise(Long enterpriseId) {
        // TODO: 校验 enterpriseId 合法性
        //       校验操作人权限（仅 OWNER 可注销企业）
        //       校验企业下无活跃岗位（jobs.status = 1）
        //       逻辑删除 enterprises（is_deleted = true）
        //       级联逻辑删除 enterprise_team_members 相关记录
        //       Phase 8 扩展：需校验 sys_enterprise_cert 已通过认证
    }
}
