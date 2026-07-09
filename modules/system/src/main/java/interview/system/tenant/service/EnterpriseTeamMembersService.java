package interview.system.tenant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.tenant.model.entity.EnterpriseTeamMember;
import interview.system.tenant.model.req.TeamMemberCreateReq;
import interview.system.tenant.model.req.TeamMemberUpdateReq;
import interview.system.tenant.model.vo.TeamMemberCreateVO;
import interview.system.tenant.model.vo.TeamMemberItemVO;

/**
 * @author zhuxi
 */
public interface EnterpriseTeamMembersService extends IService<EnterpriseTeamMember> {

    IPage<TeamMemberItemVO> listTeamMembers(Long enterpriseId, Integer page, Integer size, String order);

    TeamMemberCreateVO inviteMember(Long enterpriseId, TeamMemberCreateReq req);

    void updateMemberRole(Long enterpriseId, Long memberId, TeamMemberUpdateReq req);

    void removeMember(Long enterpriseId, Long memberId);
}
