package interview.system.tenant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.common.enums.ErrorCode;
import interview.common.enums.Role;
import interview.common.enums.RoleScope;
import interview.common.enums.SelectOrder;
import interview.common.exception.BusinessException;
import interview.framework.config.CustomIdGenerator;
import interview.framework.context.AuthContext;
import interview.system.rbac.model.entity.SysRole;
import interview.system.rbac.service.RolesService;
import interview.system.tenant.mapper.EnterpriseTeamMembersMapper;
import interview.system.tenant.model.bo.TeamMemberItemBO;
import interview.system.tenant.model.entity.Enterprise;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 企业内部团队成员表 服务实现类
 * </p>
 *
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class EnterpriseTeamMembersServiceImpl extends ServiceImpl<EnterpriseTeamMembersMapper, EnterpriseTeamMember> implements EnterpriseTeamMembersService {

    private final EnterprisesService enterprisesService;
    private final EnterpriseTeamMembersMapper enterpriseTeamMembersMapper;
    private final RolesService rolesService;
    private final CustomIdGenerator idGenerator;

    @Override
    public IPage<TeamMemberItemVO> listTeamMembers(Long enterpriseId, Integer page, Integer size,String order) {
        // TODO: 校验 enterpriseId 合法性
        verifyEnterpriseId(enterpriseId);
        // 租户隔离校验：当前用户必须属于该企业
        Long userId = AuthContext.getRequiredUserId();
        boolean exists = lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }

        Page<TeamMemberItemBO> boPage = new Page<>(page,size);
        // 根据order决定按 created_at排列
        if (SelectOrder.ASC.name().equalsIgnoreCase(order)) {
            boPage.addOrder(OrderItem.asc("created_at"));
        }else{
            boPage.addOrder(OrderItem.desc("created_at"));
        }

        // 联表查询 enterprise_team_members + sys_users（username, nickname, email）
        Page<TeamMemberItemBO> teamMembersPage = enterpriseTeamMembersMapper.getTeamMembersPage(boPage, enterpriseId);
        List<TeamMemberItemBO> records = teamMembersPage.getRecords();
        if (records.isEmpty()){
            return new Page<>(teamMembersPage.getCurrent(),teamMembersPage.getSize(),teamMembersPage.getTotal());
        }


        // 取出RoleId 并去重去空
        List<Integer> list = records.stream()
                .map(TeamMemberItemBO::roleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();


        // 查询sys_roles（role_code）
        Map<Integer, String> roleCodeMap = rolesService.lambdaQuery()
                .select(SysRole::getId, SysRole::getRoleCode)
                .in(SysRole::getId, list)
                .list()
                .stream()
                .collect(
                        Collectors.toMap(SysRole::getId, SysRole::getRoleCode)
                );

        List<TeamMemberItemVO> teamMemberItemVO = records.stream()
                .map(bo ->
                        TeamMemberItemVO.builder()
                                .id(bo.id())
                                .userId(bo.userId())
                                .username(bo.username())
                                .nickname(bo.nickname())
                                .email(bo.email())
                                .roleCode(roleCodeMap.getOrDefault(bo.roleId(), Role.UNKNOWN.getMsg()))
                                .createdAt(bo.createdAt())
                                .build()
                ).toList();
        // 分页返回 IPage<TeamMemberItemVO>
        Page<TeamMemberItemVO> voPage = new Page<>(teamMembersPage.getCurrent(),teamMembersPage.getSize(),teamMembersPage.getTotal());
        voPage.setRecords(teamMemberItemVO);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public TeamMemberCreateVO inviteMember(Long enterpriseId, TeamMemberCreateReq req) {

        // 校验 enterpriseId 合法性
        verifyEnterpriseId(enterpriseId);

        Integer roleId = req.getRoleId();
        //校验 req.roleId 是否为 ENTERPRISE域的角色
        verifyRoleId(roleId);

        // TODO: 等待权限模块构造完成，补全该处逻辑
        //  校验操作人权限（HR_MANAGER 及以上可邀请成员）
        //  邀请并分配ENTERPRISE_ADMIN 需要ENTERPRISE_OWNER权限

        //       防止重复邀请：校验 (enterprise_id, user_id, role_id) 唯一性
        Long userId = req.getUserId();
        boolean exists2 = lambdaQuery()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getUserId, userId)
                .eq(EnterpriseTeamMember::getRoleId, roleId)
                .exists();
        if (exists2) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }
        //       INSERT enterprise_team_members
        Long number = (Long)idGenerator.nextId(EnterpriseTeamMember.class);
        EnterpriseTeamMember build = EnterpriseTeamMember.builder()
                .id(number)
                .enterpriseId(enterpriseId)
                .userId(userId)
                .roleId(roleId)
                .build();

        try {
            save(build);
        }catch (DataIntegrityViolationException e){
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        //       返回 TeamMemberCreateVO
        return new TeamMemberCreateVO(number);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void updateMemberRole(Long enterpriseId, Long memberId, TeamMemberUpdateReq req) {
        // TODO: 校验 enterpriseId 合法性
        verifyEnterpriseId(enterpriseId);
        //TODO: 校验操作人权限（HR_MANAGER 及以上可修改角色）

        //       校验 memberId 属于该企业
        verifyMemberIdBelong(enterpriseId,memberId);

        //不允许修改 OWNER 角色
        if (Role.ENTERPRISE_OWNER.getCode().equals(req.getRoleId())){
            throw new BusinessException(ErrorCode.NO_OPERATE_ENTERPRISE_OWNER);
        }
        //       校验 req.roleId 是否为 ENTERPRISE 域角色
        verifyRoleId(req.getRoleId());
        //       UPDATE enterprise_team_members SET role_id = req.roleId
        boolean updated = lambdaUpdate()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getId, memberId)
                .set(EnterpriseTeamMember::getRoleId, req.getRoleId())
                .update();

        if (!updated) {
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void removeMember(Long enterpriseId, Long memberId) {
        // 校验 enterpriseId 合法性
        verifyEnterpriseId(enterpriseId);

        //TODO : 校验操作人权限（HR_MANAGER 及以上可移除成员）

        //       校验 memberId 属于该企业
        EnterpriseTeamMember one = lambdaQuery()
                .select(EnterpriseTeamMember::getEnterpriseId,
                        EnterpriseTeamMember::getRoleId,
                        EnterpriseTeamMember::getUserId
                )
                .eq(EnterpriseTeamMember::getId, memberId)
                .one();
        if (one == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_MEMBER_NOT_FOUND);
        }
        if (!one.getEnterpriseId().equals(enterpriseId)){
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }

        //       不允许移除 OWNER
        if (Role.ENTERPRISE_OWNER.getCode().equals(one.getRoleId())){
            throw new BusinessException(ErrorCode.NO_OPERATE_ENTERPRISE_OWNER);
        }

        //       不允许移除自己
        if (one.getUserId().equals(AuthContext.getRequiredUserId())){
            throw new BusinessException(ErrorCode.NO_DELETE_OWNER);
        }
        //       逻辑删除 enterprise_team_members（is_deleted = true）
        boolean deleted = lambdaUpdate()
                .eq(EnterpriseTeamMember::getEnterpriseId, enterpriseId)
                .eq(EnterpriseTeamMember::getId, memberId)
                .set(EnterpriseTeamMember::getIsDeleted,true)
                .update();

        if (!deleted) {
            throw new BusinessException(ErrorCode.ENTERPRISE_DATA_ANOMALY);
        }
    }


    /**
     * 校验企业存在且未被逻辑删除
     * @param enterpriseId 企业 ID
     */
    public void verifyEnterpriseId(Long enterpriseId) {
        boolean exists = enterprisesService.lambdaQuery()
                .eq(Enterprise::getId, enterpriseId)
                .exists();
        if (!exists) {
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_FOUND);
        }
    }

    /**
     * 校验角色 ID 属于 ENTERPRISE 域
     * @param roleId 角色 ID
     */
    public void verifyRoleId(Integer roleId) {
        if (!Role.fromCode(roleId).getRoleScope().equals(RoleScope.ENTERPRISE))
        {
            throw new BusinessException(ErrorCode.NO_ALLOW_ROLE);
        }
    }

    /**
     * 校验团队成员记录存在且属于指定企业
     * @param enterpriseId 企业 ID
     * @param memberId 团队成员 ID
     */
    public void verifyMemberIdBelong(Long enterpriseId, Long memberId) {
        EnterpriseTeamMember one = lambdaQuery()
                .select(EnterpriseTeamMember::getEnterpriseId)
                .eq(EnterpriseTeamMember::getId, memberId)
                .one();
        if (one == null) {
            throw new BusinessException(ErrorCode.ENTERPRISE_MEMBER_NOT_FOUND);
        }
        if (!one.getEnterpriseId().equals(enterpriseId)){
            throw new BusinessException(ErrorCode.ENTERPRISE_NOT_BELONG);
        }
    }
}
