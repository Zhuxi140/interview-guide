package interview.api.system;

import interview.api.system.dto.UserProfileDTO;
import interview.api.system.dto.UserProfileUpdateDTO;

import java.util.List;
import java.util.Map;

/**
 * @author zhuxi
 */


public interface UserApi {

    /**
     * 根据ids获取用户names
     * @param userIds ids
     * @return map<id,name>
     */
    Map<Long,String> getUserNamesByIds(List<Long> userIds);

    /**
     * 根据id获取用户name
     * @param userId id
     * @return name
     */
    String getUserNameById(Long userId);

    /**
     * 查询用户基础资料
     * @param userId 用户 ID
     * @return 用户基础资料；用户不存在时返回 null
     */
    UserProfileDTO getUserProfile(Long userId);

    /**
     * 更新用户昵称和邮箱
     * @param userId 用户 ID
     * @param update 更新内容，null 字段保持不变
     * @return 更新后的用户基础资料
     */
    UserProfileDTO updateUserProfile(Long userId, UserProfileUpdateDTO update);

}
