package interview.api.system;

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


}
