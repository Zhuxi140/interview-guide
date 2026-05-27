package interview.framework.context;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 认证上下文
 * <p>
 *     存入用户认证信息，用于全局调用
 * </p>
 */

public class AuthContext {

    private static final ThreadLocal<AuthUser> AUTH_CONTEXT = new ThreadLocal<>();


    /**
     * 设置认证上下文
     * @param authUser 认证用户信息
     */
    public static void setAuthContext(AuthUser authUser){
        AUTH_CONTEXT.set(authUser);
    }

    /**
     * 设置认证上下文
     * @param userId 用户ID
     * @param username 用户名
     * @param nickname 昵称
     */
    public static void setAuthContext(Long userId, String username, String nickname){
        AUTH_CONTEXT.set(new AuthUser(userId, username));
    }

    /**
     * 获取认证上下文
     * @return 认证用户信息
     */
    public static AuthUser getAuthContext(){
        return AUTH_CONTEXT.get();
    }

    /**
     * 获取认证上下文 (已处理null情况)
     * @return 认证用户信息
     */
    public static AuthUser getRequiredAuthContext(){
        AuthUser authUser = AUTH_CONTEXT.get();
        if (authUser == null){
            // TODO: 抛出异常，因异常模块 没有完全构建完成 简单抛出
            throw new RuntimeException("未登录");
        }
        return authUser;
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public static Long getUserId(){
        return getRequiredAuthContext().userId;
    }

    /**
     * 移除认证上下文
     */
    public static void remove(){
        AUTH_CONTEXT.remove();
    }

    /**
     * 内部类存储用户认证信息
     */
    public record AuthUser(
            Long userId,
            String username
    ){}
}
