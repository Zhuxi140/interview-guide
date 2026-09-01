package interview.system.auth.api;

import cn.hutool.core.util.StrUtil;
import interview.api.system.UserApi;
import interview.api.system.dto.UserProfileDTO;
import interview.api.system.dto.UserProfileUpdateDTO;
import interview.common.enums.ErrorCode;
import interview.common.exception.BusinessException;
import interview.system.auth.model.entity.User;
import interview.system.auth.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
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

    @Override
    public UserProfileDTO getUserProfile(Long userId) {
        User user = usersService.lambdaQuery()
                .select(User::getId, User::getNickname, User::getEmail, User::getUpdatedAt)
                .eq(User::getId, userId)
                .one();
        return user == null ? null : toProfileDTO(user);
    }

    @Override
    public UserProfileDTO updateUserProfile(Long userId, UserProfileUpdateDTO update) {
        User current = usersService.lambdaQuery()
                .select(User::getId)
                .eq(User::getId, userId)
                .one();
        if (current == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String displayName = update.displayName() == null
                ? null : update.displayName().trim();
        String email = update.email() == null
                ? null : StrUtil.emptyToNull(update.email().trim().toLowerCase(Locale.ROOT));
        if (update.email() != null && email != null && usersService.lambdaQuery()
                .eq(User::getEmail, email)
                .ne(User::getId, userId)
                .exists()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 只更新客户端明确提交的昵称和邮箱。
        try {
            usersService.lambdaUpdate()
                    .eq(User::getId, userId)
                    .set(update.displayName() != null, User::getNickname, displayName)
                    .set(update.email() != null, User::getEmail, email)
                    .set(User::getUpdatedAt, OffsetDateTime.now())
                    .update();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return getUserProfile(userId);
    }

    private UserProfileDTO toProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId(), user.getNickname(), user.getEmail(), user.getUpdatedAt());
    }
}
