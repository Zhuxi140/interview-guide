package interview.system.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.UsersService;
import interview.system.rbac.mapper.UsersMapper;
import interview.system.rbac.model.req.AdminUserSearchReq;
import interview.system.rbac.model.vo.AdminUserListItemVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统用户主表（核心用户表，多张表依赖它） 服务实现类
 * </p>
 *
 * @author zhuxi
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, User> implements UsersService {

    @Override
    public IPage<AdminUserListItemVO> pageUsers(AdminUserSearchReq req) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname,
                        User::getPhone, User::getUserType, User::getStatus, User::getCreatedAt)
                .eq(req.getUserType() != null, User::getUserType, req.getUserType())
                .eq(req.getStatus() != null, User::getStatus, req.getStatus())
                .and(cn.hutool.core.util.StrUtil.isNotBlank(req.getKeyword()),
                        w -> w.like(User::getUsername, req.getKeyword())
                                .or()
                                .like(User::getNickname, req.getKeyword()))
                .orderByDesc(User::getCreatedAt);
        Page<User> userPage = baseMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        Page<AdminUserListItemVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(user -> AdminUserListItemVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .phone(user.getPhone())
                        .userType(user.getUserType() == null ? null : user.getUserType().name())
                        .status(user.getStatus() == null ? null : user.getStatus().name())
                        .createdAt(user.getCreatedAt())
                        .build())
                .toList());
        return voPage;
    }
}
