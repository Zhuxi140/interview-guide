package interview.system.tenant;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;
import interview.system.tenant.model.vo.TeamMemberUpdateVO;

import java.util.List;

/**
 * <p>
 * 企业内部团队成员表 服务类
 * </p>
 *
 * @author zhuxi
 */
public interface EnterpriseTeamMembersService extends IService<EnterpriseTeamMember> {

    /**
     * 查询团队成员列表
     *
     * @param enterpriseId 企业 ID
     * @return 成员列表
     */
    List<TeamMemberItemVO> listTeamMembers(Long enterpriseId);

    /**
     * 邀请成员加入企业
     *
     * @param enterpriseId 企业 ID
     * @param req          邀请请求
     * @return 创建结果
     */
    TeamMemberCreateVO inviteMember(Long enterpriseId, TeamMemberCreateReq req);

    /**
     * 修改成员角色
     *
     * @param enterpriseId 企业 ID
     * @param memberId     成员记录 ID
     * @param req          修改角色请求
     * @return 修改结果
     */
    TeamMemberUpdateVO updateMemberRole(Long enterpriseId, Long memberId, TeamMemberUpdateReq req);

    /**
     * 移除团队成员
     *
     * @param enterpriseId 企业 ID
     * @param memberId     成员记录 ID
     */
    void removeMember(Long enterpriseId, Long memberId);
}
