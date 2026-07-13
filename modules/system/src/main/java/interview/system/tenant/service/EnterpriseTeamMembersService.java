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

    /**
     * 查询团队成员列表（分页）
     *
     * @param enterpriseId 企业 ID
     * @param page         页码
     * @param size         每页条数
     * @param order        排序方向（asc/desc）
     * @return 分页结果
     */
    IPage<TeamMemberItemVO> listTeamMembers(Long enterpriseId, Integer page, Integer size, String order);

    /**
     * 邀请成员加入企业
     *
     * @param enterpriseId 企业 ID
     * @param req          邀请请求
     * @return 邀请结果
     */
    TeamMemberCreateVO inviteMember(Long enterpriseId, TeamMemberCreateReq req);

    /**
     * 修改成员角色
     *
     * @param enterpriseId 企业 ID
     * @param memberId     成员 ID
     * @param req          角色修改请求
     */
    void updateMemberRole(Long enterpriseId, Long memberId, TeamMemberUpdateReq req);

    /**
     * 移除团队成员
     *
     * @param enterpriseId 企业 ID
     * @param memberId     成员 ID
     */
    void removeMember(Long enterpriseId, Long memberId);
}
