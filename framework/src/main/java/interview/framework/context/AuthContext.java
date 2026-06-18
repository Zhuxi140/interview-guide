package interview.framework.context;

import interview.common.enums.RiskLevel;
import interview.common.enums.RoleScope;
import interview.common.enums.UserType;
import lombok.Builder;

import java.util.List;

/**
 * @author zhuxi
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
            throw new RuntimeException("未登录或ThreadLocal数据不完整");
        }
        return authUser;
    }

    /**
     * 获取用户ID (可能为null)
     * 专供基础拦截器（如 MyBatis-Plus 字段填充、日志拦截器）使用
     * @return 用户ID
     */
    public static Long getUserIdSafe() {
        AuthUser authUser = AUTH_CONTEXT.get();
        return authUser != null ? authUser.userId() : null;
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public static Long getUserId(){
        return getRequiredAuthContext().userId;
    }

    /**
     * 获取用户类型
     * @return 用户类型
     */
    public static UserType getUserType(){
        return getRequiredAuthContext().userType;
    }

    /**
     * 获取企业租户 ID
     * @return 企业租户 ID
     */
    public static Long getEnterpriseId(){
        return getRequiredAuthContext().enterpriseId;
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
    @Builder
    public record AuthUser(
            Long userId,
            UserType userType,
            RiskLevel riskLevel,
            RoleScope roleScope,
            Long enterpriseId,
            String username,
            List<String> roleCodes
    ){}
}
