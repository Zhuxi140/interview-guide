package interview.system.tenant.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.api.bportal.JobValidationApi;
import interview.api.system.SecureChallengeApi;
import interview.api.system.dto.SecureChallengeStartDTO;
import interview.common.constant.SecureActionContext;
import interview.common.enums.ErrorCode;
import interview.common.enums.SecureActionType;
import interview.common.enums.UserType;
import interview.common.exception.BusinessException;
import interview.common.util.TraceUtil;
import interview.framework.config.CustomIdGenerator;
import interview.framework.context.AuthContext;
import interview.system.auth.model.entity.User;
import interview.system.auth.model.vo.SecureChallengeStartVO;
import interview.system.auth.service.UsersService;
import interview.system.rbac.model.entity.Role;
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
import interview.system.tenant.model.req.EnterpriseContactEmailUpdateReq;
import interview.system.tenant.model.req.EnterpriseCreateReq;
import interview.system.tenant.model.vo.EnterpriseContactEmailUpdateVO;
import interview.system.tenant.model.vo.EnterpriseContactPhoneUpdateVO;
import interview.system.tenant.model.vo.EnterpriseDetailVO;
import interview.system.tenant.model.vo.EnterpriseUpdateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
@Slf4j
public class EnterprisesServiceImpl extends ServiceImpl<EnterprisesMapper, Enterprise> implements EnterprisesService {

    private static final long ENTERPRISE_CONTACT_LOCK_NAMESPACE = 0x454E54434F4E5443L;

    private final CustomIdGenerator customIdGenerator;
    private final UserRolesService userRolesService;
    private final EnterpriseTeamMembersService enterpriseTeamMembersService;
    private final EnterprisesMapper enterprisesMapper;
    private final RolesService rolesService;
    private final SecureChallengeApi secureChallengeApi;
    private final JobValidationApi jobValidationApi;
    private final UsersService usersService;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public EnterpriseCreateBO createEnterprise(EnterpriseCreateReq req) {
        //校验企业名称唯一性
        boolean exists = lambdaQuery()
                .select(Enterprise::getName)
                .eq(Enterprise::getName, req.getName())
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
        EnterpriseTeamMember enterpriseTeamMember = EnterpriseTeamMember.builder()
                .userId(AuthContext.getRequiredUserId())
                .roleId(interview.common.enums.Role.ENTERPRISE_OWNER.getCode())
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
                .select(Role::getId, Role::getRoleCode)
                .in(Role::getId, roleIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Role::getId, Role::getRoleCode));

        //统计每个企业的 memberCount
        Set<Long> enterpriseIds = bos.stream()
                .map(UserEnterprisesBO::enterpriseId)
                .collect(Collectors.toSet());


        List<Map<String, Object>> countList = enterpriseTeamMembersService.listMaps(
                new QueryWrapper<EnterpriseTeamMember>()
                        .select("enterprise_id", "COUNT(DISTINCT user_id) AS count")
                        .in("enterprise_id", enterpriseIds)
                        .groupBy("enterprise_id")
        );

        Map<Long, Long> memberCountMap = countList.stream()
                .collect(
                    Collectors.toMap(
                            map -> ((Number) map.get("enterprise_id")).longValue(),
                            map -> ((Number) map.get("count")).longValue()
                ));

        //返回 List<ListUserEnterprisesBO>
        return bos.stream()
                .map(bo ->{
                    String roleCode = roleCodeMap.getOrDefault(bo.roleId(), interview.common.enums.Role.UNKNOWN.name());
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
                            .roleCode(roleCode)
                            .memberCount(memberCountMap.get(bo.enterpriseId()))
                            .build();
                    }
                ).toList();
    }

    @Override
    public EnterpriseDetailVO getEnterpriseDetail(Long enterpriseId) {
        //校验 enterpriseId 合法性（是否存在、is_deleted = false）
        verifyEnterpriseId(enterpriseId);
        //租户隔离校验：当前用户必须属于该企业
        Long userId = AuthContext.getRequiredUserId();
        boolean exists1 = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
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

        if (baseMapper.updateById(update) == 0) {
            log.error("更新企业基本信息未影响任何记录: enterpriseId [{}]", enterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }
        Enterprise updated = baseMapper.selectById(enterpriseId);
        return new EnterpriseUpdateVO(updated.getId(), updated.getName(), updated.getShortName(), updated.getIndustry());
    }

    @Override
    public SecureChallengeStartVO startContactEmailChallenge(Long enterpriseId) {
        // 确认当前用户属于目标企业后，创建邮箱更新专用 Challenge
        validateEnterpriseBelong(enterpriseId);
        SecureChallengeStartDTO challenge = secureChallengeApi.create(
                AuthContext.getRequiredUserId(),
                SecureActionType.UPDATE_ENTERPRISE_EMAIL,
                enterpriseId);
        return toSecureChallengeStartVO(challenge);
    }

    @Override
    public SecureChallengeStartVO startDeletionChallenge(Long enterpriseId) {
        // 确认当前用户属于目标企业后，创建企业注销专用 Challenge
        validateEnterpriseBelong(enterpriseId);
        SecureChallengeStartDTO challenge = secureChallengeApi.create(
                AuthContext.getRequiredUserId(),
                SecureActionType.DELETE_ENTERPRISE,
                enterpriseId);
        return toSecureChallengeStartVO(challenge);
    }

    @Override
    @Transactional
    public EnterpriseContactEmailUpdateVO updateEnterpriseContactEmail(
            Long enterpriseId, SecureActionContext secureActionContext,
            EnterpriseContactEmailUpdateReq req) {
        // 校验企业归属以及令牌动作和企业绑定关系
        validateEnterpriseBelong(enterpriseId);
        validateSecureActionContext(
                secureActionContext,
                SecureActionType.UPDATE_ENTERPRISE_EMAIL,
                enterpriseId);
        String contactEmail = req.getContactEmail();

        // 邮箱为单字段更新，使用 LambdaUpdate 并手动维护审计字段
        boolean updated = lambdaUpdate()
                .eq(Enterprise::getId, enterpriseId)
                .set(Enterprise::getContactEmail, contactEmail)
                .set(Enterprise::getUpdatedAt, OffsetDateTime.now())
                .set(Enterprise::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!updated) {
            log.error("更新企业联系方式未影响任何记录: enterpriseId [{}]", enterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }

        return new EnterpriseContactEmailUpdateVO(enterpriseId, contactEmail);
    }

    @Override
    @Transactional
    public EnterpriseContactPhoneUpdateVO updateEnterpriseContactPhone(
            Long enterpriseId, SecureActionContext secureActionContext) {
        // 校验企业归属以及令牌动作和企业绑定关系
        validateEnterpriseBelong(enterpriseId);
        validateSecureActionContext(
                secureActionContext,
                SecureActionType.UPDATE_ENTERPRISE_PHONE,
                enterpriseId);

        // 新联系电话以安全令牌上下文为准，不接受客户端重复提交
        String contactPhone = secureActionContext.getTargetPhone();
        if (StrUtil.isBlank(contactPhone)) {
            throw new BusinessException(ErrorCode.CONTACT_VERIFY_FLOW_INVALID);
        }

        // 串行化同一企业的联系电话变更，并检查原联系电话快照未变化
        enterprisesMapper.lockEnterpriseContact(
                enterpriseId ^ ENTERPRISE_CONTACT_LOCK_NAMESPACE);
        Enterprise current = lambdaQuery()
                .select(Enterprise::getId, Enterprise::getContactPhone)
                .eq(Enterprise::getId, enterpriseId)
                .one();
        if (current == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        if (!StrUtil.nullToEmpty(current.getContactPhone()).equals(
                StrUtil.nullToEmpty(secureActionContext.getSourcePhone()))) {
            throw new BusinessException(ErrorCode.ENTERPRISE_CONTACT_CHANGED);
        }

        // 联系电话为单字段更新，使用 LambdaUpdate 并手动维护审计字段
        boolean updated = lambdaUpdate()
                .eq(Enterprise::getId, enterpriseId)
                .set(Enterprise::getContactPhone, contactPhone)
                .set(Enterprise::getUpdatedAt, OffsetDateTime.now())
                .set(Enterprise::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!updated) {
            log.error("更新企业联系电话未影响任何记录: enterpriseId [{}]", enterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }

        return new EnterpriseContactPhoneUpdateVO(enterpriseId, contactPhone);
    }


    @Override
    @Transactional
    public void deleteEnterprise(
            Long enterpriseId, SecureActionContext secureActionContext) {
        // 校验企业归属以及令牌动作和企业绑定关系
        validateEnterpriseBelong(enterpriseId);
        validateSecureActionContext(
                secureActionContext,
                SecureActionType.DELETE_ENTERPRISE,
                enterpriseId);

        // 校验企业下无活跃岗位
        if (jobValidationApi.hasActiveJobs(enterpriseId)) {
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }

        // 企业状态是认证结果的业务事实源，NORMAL 表示注销前已通过认证。
        Enterprise current = lambdaQuery()
                .select(Enterprise::getId, Enterprise::getStatus)
                .eq(Enterprise::getId, enterpriseId)
                .one();
        if (current == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
        boolean certified = current.getStatus() == EnterpriseStatus.NORMAL;

        // 使用原状态作为并发条件，将企业状态收敛为已注销并逻辑删除。
        Long deleteUserId = AuthContext.getRequiredUserId();
        boolean updated = lambdaUpdate()
                .eq(Enterprise::getId, enterpriseId)
                .eq(Enterprise::getStatus, current.getStatus())
                .set(Enterprise::getStatus, EnterpriseStatus.CANCELLED)
                .set(Enterprise::getIsDeleted, true)
                .set(Enterprise::getUpdatedAt, OffsetDateTime.now())
                .set(Enterprise::getTraceId, TraceUtil.getTraceId())
                .update();

        if (!updated) {
            log.error("删除企业未影响任何记录: enterpriseId [{}]", enterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }
        //级联逻辑删除 enterprise_team_members 相关记录
        boolean membersUpdated = enterpriseTeamMembersService.lambdaUpdate()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .set(EnterpriseTeamMember::getIsDeleted, true)
                .set(EnterpriseTeamMember::getUpdatedBy, deleteUserId)
                .set(EnterpriseTeamMember::getUpdatedAt, OffsetDateTime.now())
                .set(EnterpriseTeamMember::getTraceId, TraceUtil.getTraceId())
                .update();
        if (!membersUpdated) {
            log.error("级联删除企业成员未影响任何记录: enterpriseId [{}]", enterpriseId);
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }

        // 已认证企业注销后，仅在用户没有其他正常企业时回退账号类型。
        if (certified && !hasOtherCertifiedEnterprise(deleteUserId)) {
            User user = usersService.lambdaQuery()
                    .select(User::getId, User::getUserType)
                    .eq(User::getId, deleteUserId)
                    .one();
            if (user == null) {
                throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
            }
            if (user.getUserType() == UserType.ENTERPRISE_USER) {
                boolean userUpdated = usersService.lambdaUpdate()
                        .eq(User::getId, deleteUserId)
                        .eq(User::getUserType, UserType.ENTERPRISE_USER)
                        .set(User::getUserType, UserType.CANDIDATE)
                        .set(User::getUpdatedAt, OffsetDateTime.now())
                        .update();
                if (!userUpdated) {
                    throw new BusinessException(ErrorCode.ACCOUNT_DATA_ANOMALY);
                }
            }
        }
    }

    private boolean hasOtherCertifiedEnterprise(Long userId) {
        List<Long> enterpriseIds = enterpriseTeamMembersService.lambdaQuery()
                .select(EnterpriseTeamMember::getEnterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .list()
                .stream()
                .map(EnterpriseTeamMember::getEnterpriseId)
                .distinct()
                .toList();
        return !enterpriseIds.isEmpty() && lambdaQuery()
                .in(Enterprise::getId, enterpriseIds)
                .eq(Enterprise::getStatus, EnterpriseStatus.NORMAL)
                .exists();
    }

    private SecureChallengeStartVO toSecureChallengeStartVO(
            SecureChallengeStartDTO challenge) {
        // 将内部 API DTO 转换为对外 Swagger VO
        return new SecureChallengeStartVO(
                challenge.challengeId(),
                challenge.maskedPhone(),
                challenge.expiresInSeconds());
    }

    private void validateSecureActionContext(
            SecureActionContext context,
            SecureActionType expectedAction,
            Long enterpriseId) {
        // 令牌必须精确匹配业务动作，并绑定当前路径中的企业
        if (context == null || context.getActionType() != expectedAction) {
            throw new BusinessException(ErrorCode.SECURE_ACTION_NOT_MATCH);
        }
        if (!enterpriseId.equals(context.getEnterpriseId())
                || (context.getResourceId() != null
                && !enterpriseId.equals(context.getResourceId()))) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        if (!AuthContext.getRequiredUserId().equals(context.getUserId())) {
            throw new BusinessException(ErrorCode.SECURE_TOKEN_USER_MISMATCH);
        }
    }

    /**
     * 校验企业存在且未被逻辑删除
     * @param enterpriseId 企业 ID
     */
    public void verifyEnterpriseId(Long enterpriseId) {
        boolean exists = lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .in(Enterprise::getStatus,
                        EnterpriseStatus.PENDING,
                        EnterpriseStatus.NORMAL)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
    }


    /**
     * 校验 enterpriseId 合法且当前用户为企业成员
     * @param enterpriseId 企业 ID
     */
    private void validateEnterpriseBelong(Long enterpriseId) {
        verifyEnterpriseId(enterpriseId);

        Long userId = AuthContext.getRequiredUserId();
        boolean member = enterpriseTeamMembersService.lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();
        if (!member) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
    }
}
