package interview.system.tenant;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import interview.system.tenant.mapper.EnterpriseTeamMembersMapper;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import interview.system.tenant.model.vo.TeamMemberUpdateVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 企业内部团队成员表 服务实现类
 * </p>
 *
 * @author zhuxi
 */
@Service
public class EnterpriseTeamMembersServiceImpl extends ServiceImpl<EnterpriseTeamMembersMapper, EnterpriseTeamMember> implements EnterpriseTeamMembersService {

    @Override
    public List<TeamMemberItemVO> listTeamMembers(Long enterpriseId) {
        // TODO: 校验 enterpriseId 合法性
        //       租户隔离校验：当前用户必须属于该企业
        //       联表查询 enterprise_team_members + sys_users（username, nickname, email）+ sys_roles（role_code）
        //       按 created_at 升序排列
        //       返回 List<TeamMemberItemVO>
        return List.of();
    }

    @Override
    public TeamMemberCreateVO inviteMember(Long enterpriseId, TeamMemberCreateReq req) {
        // TODO: 校验 enterpriseId 合法性
        //       校验操作人权限（HR_MANAGER 及以上可邀请成员）
        //       校验 req.userId 是否存在且未被禁用
        //       校验 req.roleId 是否为 ENTERPRISE 域角色
        //       防止重复邀请：校验 (enterprise_id, user_id) 唯一性
        //       INSERT enterprise_team_members
        //       返回 TeamMemberCreateVO
        return null;
    }

    @Override
    public TeamMemberUpdateVO updateMemberRole(Long enterpriseId, Long memberId, TeamMemberUpdateReq req) {
        // TODO: 校验 enterpriseId 合法性
        //       校验操作人权限（HR_MANAGER 及以上可修改角色）
        //       校验 memberId 属于该企业
        //       不允许修改 OWNER 角色
        //       校验 req.roleId 是否为 ENTERPRISE 域角色
        //       UPDATE enterprise_team_members SET role_id = req.roleId
        //       返回 TeamMemberUpdateVO
        return null;
    }

    @Override
    public void removeMember(Long enterpriseId, Long memberId) {
        // TODO: 校验 enterpriseId 合法性
        //       校验操作人权限（HR_MANAGER 及以上可移除成员）
        //       校验 memberId 属于该企业
        //       不允许移除 OWNER
        //       不允许移除自己
        //       逻辑删除 enterprise_team_members（is_deleted = true）
    }
}
