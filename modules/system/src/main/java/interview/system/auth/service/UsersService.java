package interview.system.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.auth.model.entity.User;
import interview.system.rbac.model.req.AdminUserSearchReq;
import interview.system.rbac.model.vo.AdminUserListItemVO;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） 服务类
 * </p>
 *
 * @author zhuxi
 */
public interface UsersService extends IService<User> {

    /**
     * 平台用户分页查询（关键词匹配用户名/昵称，手机号加密存储不支持模糊匹配）
     * @param req 查询参数
     * @return 用户分页
     */
    IPage<AdminUserListItemVO> pageUsers(AdminUserSearchReq req);
}
