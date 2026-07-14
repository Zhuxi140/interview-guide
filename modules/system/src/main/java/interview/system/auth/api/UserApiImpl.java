package interview.system.auth.api;

import interview.api.system.UserApi;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author zhuxi
 */
@Service
@RequiredArgsConstructor
public class UserApiImpl implements UserApi {
    private final UsersService usersService;
    @Override
    public Map<Long,String> getUserNamesByIds(List<Long> userIds) {
        return usersService.lambdaQuery()
                .select(User::getId, User::getNickname)
                .in(User::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }

    @Override
    public String getUserNameById(Long userId) {
        return usersService.lambdaQuery()
                .select(User::getNickname)
                .eq(User::getId, userId)
                .one()
                .getNickname();
    }
}
