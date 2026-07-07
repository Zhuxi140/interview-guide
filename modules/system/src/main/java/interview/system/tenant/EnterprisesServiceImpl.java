package interview.system.tenant;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.Role;
import interview.common.enums.SmsType;
import interview.common.exception.BusinessException;
import interview.framework.annonate.RequireSecure;
import interview.framework.config.CustomIdGenerator;
import interview.framework.context.AuthContext;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.model.entity.SysUserRole;
import interview.system.rbac.service.RolesService;
import interview.system.rbac.service.UserRolesService;
import interview.system.tenant.mapper.EnterprisesMapper;
import interview.system.tenant.model.bo.EnterpriseCreateBO;
import interview.system.tenant.model.bo.ListUserEnterprisesBO;
import interview.system.tenant.model.bo.UserEnterprisesBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.enums.EnterpriseStatus;
import interview.system.tenant.model.req.EnterpriseBasicUpdateReq;
import interview.system.tenant.model.req.EnterpriseContactUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseContactUpdateVO;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import jakarta.servlet.ServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * <p>
 * 企业租户主表 (SaaS 隔离核心，被约 20 张表依赖) 服务实现类
 * </p>
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnterprisesServiceImpl extends ServiceImpl<EnterprisesMapper, Enterprise> implements EnterprisesService {

    private final CustomIdGenerator customIdGenerator;
    private final UserRolesService userRolesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;
    private final EnterprisesMapper enterprisesMapper;
    private final RolesService rolesService;
    private final ServletRequest request;

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
                .userId(AuthContext.getRequiredUserId())
                .roleId(Role.ENTERPRISE_OWNER.getCode())
                .build();

        userRolesService.save(role);
        EnterpriseTeamMember enterpriseTeamMember = EnterpriseTeamMember.builder()
                .userId(AuthContext.getRequiredUserId())
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
        Long userId = AuthContext.getRequiredUserId();
        //联表查询 enterprise_team_members 获取用户所属企业列表
        List<UserEnterprisesBO> bos = enterprisesMapper.getListUserEnterprises(userId);
        if (bos.isEmpty()){
            return List.of();
        }
        //关联 sys_roles 获取 roleCode
        Set<Integer> roleIds = bos.stream()
                .map(UserEnterprisesBO::roleId)
                .collect(Collectors.toSet());

        Map<Integer, String> roleCodeMap = rolesService.lambdaQuery()
                .select(SysRole::getId,SysRole::getRoleCode)
                .in(SysRole::getId, roleIds)
                .list()
                .stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));

        //统计每个企业的 memberCount
        Set<Long> enterpriseIds = bos.stream()
                .map(UserEnterprisesBO::enterpriseId)
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
                .map(bo ->{
                    String roleCode = roleCodeMap.get(bo.roleId());
                    if (roleCode == null){
                        log.warn("[数据一致性警告] 未找到对应的 RoleCode! 脏数据企业关联 ID: {}, 缺失的 RoleID:{}",
                                bo.enterpriseId(), bo.roleId());
                    }
                    return ListUserEnterprisesBO.builder()
                            .enterpriseId(bo.enterpriseId())
                            .name(bo.name())
                            .shortName(bo.shortName())
                            .industry(bo.industry())
                            .status(bo.status())
                            // FIXME: 若 roleCode 为 null，前端应拦截并提示用户其数据有异常 建议联系客服
                            .roleCode(roleCode)
                            .memberCount(memberCountMap.get(bo.enterpriseId()))
                            .build();
                    }
                ).toList();
    }

    @Override
    public EnterpriseDetailVO getEnterpriseDetail(Long enterpriseId) {
        // TODO: 校验 enterpriseId 合法性（是否存在、is_deleted = false）
        boolean exists = lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .eq(Enterprise::getIsDeleted,false)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        //租户隔离校验：当前用户必须属于该企业
        Long userId = AuthContext.getRequiredUserId();
        boolean exists1 = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getIsDeleted, false)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();
        if (!exists1) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
        //       查询 Enterprise 实体
        Enterprise enterprise = enterprisesMapper.getEnterpriseDetail(enterpriseId);

        return EnterpriseDetailVO.builder()
                .id(enterpriseId)
                .name(enterprise.getName())
                .shortName(enterprise.getShortName())
                .industry(enterprise.getIndustry())
                .scale(enterprise.getScale())
                .contactEmail(enterprise.getContactEmail())
                .contactPhone(enterprise.getContactPhone())
                .status(enterprise.getStatus())
                .logoUrl(enterprise.getLogoUrl())
                .createdAt(enterprise.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public EnterpriseUpdateVO updateEnterpriseBasic(Long enterpriseId, EnterpriseBasicUpdateReq req) {
        validateEnterpriseBelong(enterpriseId);

        Enterprise update = Enterprise.builder().id(enterpriseId).build();
        if (req.getName() != null){
            update.setName(req.getName());
        }
        if (req.getShortName() != null){
            update.setShortName(req.getShortName());
        }
        if (req.getIndustry() != null){
            update.setIndustry(req.getIndustry());
        }
        if (req.getScale() != null){
            update.setScale(req.getScale());
        }
        if (req.getLogoUrl() != null){
            update.setLogoUrl(req.getLogoUrl());
        }

        baseMapper.updateById(update);
        Enterprise updated = baseMapper.selectById(enterpriseId);
        return new EnterpriseUpdateVO(updated.getId(), updated.getName(), updated.getShortName(), updated.getIndustry());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public EnterpriseContactUpdateVO updateEnterpriseContact(Long enterpriseId, EnterpriseContactUpdateReq req) {
        validateEnterpriseBelong(enterpriseId);

        SecureActionContext secureActionContext = (SecureActionContext)request.getAttribute("SECURE_ACTION_CONTEXT");

        String contactEmail = req.getContactEmail();
        String contactPhone = req.getContactPhone();
        if (contactEmail == null && contactPhone == null) {
            throw new BusinessException(ErrorCode.LACK_PHONE_OR_EMAIL);
        }

        String targetPhone = secureActionContext.getTargetPhone();
        if (contactPhone != null && !contactPhone.equals(targetPhone)){
            throw new BusinessException(ErrorCode.PHONE_MISMATCH);
        }

        lambdaUpdate()
                .eq(Enterprise::getId,enterpriseId)
                .set(contactPhone != null,Enterprise::getContactPhone,contactPhone)
                .set(contactEmail != null,Enterprise::getContactEmail, req.getContactEmail())
                .update();

        return new EnterpriseContactUpdateVO(enterpriseId, contactEmail, contactPhone);
    }

    private void validateEnterpriseBelong(Long enterpriseId) {
        boolean exists = lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .eq(Enterprise::getIsDeleted, false)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        Long userId = AuthContext.getRequiredUserId();
        boolean member = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .eq(EnterpriseTeamMember::getIsDeleted, false)
                .exists();
        if (!member) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
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
