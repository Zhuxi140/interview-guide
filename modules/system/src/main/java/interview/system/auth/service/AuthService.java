package interview.system.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.common.constant.SecureActionContext;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.RefreshTokenVO;
import interview.system.auth.model.vo.TokenInfoVO;
import interview.system.auth.model.vo.WorkspaceSwitchVO;
import interview.system.auth.model.vo.SecureChallengeStartVO;

import java.util.List;

/**
 * @author zhuxi
 * @apiNote 认证服务
 */
public interface AuthService extends IService<UserToken> {

    /**
     *  注册
     * @param register 注册信息
     * @return 注册结果
     */
    RegisterBo register(RegisterReq register);

    /**
     * 登录
     * @param loginReq 登录信息
     * @param clientIp 服务端识别的客户端 IP
     * @return 登录结果
     */
    LoginBO login(LoginReq loginReq, String clientIp);

    /**
     * 手机验证码登录
     * @param loginReq 手机验证码登录信息
     * @param clientIp 服务端识别的客户端 IP
     * @return 登录结果
     */
    LoginBO loginBySms(SmsLoginReq loginReq, String clientIp);

    /**
     * 使用短信验证码重置密码
     * @param resetReq 重置密码信息
     */
    void resetPassword(PasswordResetReq resetReq);

    /**
     * 刷新token
     * @param refreshToken 刷新token
     * @return 刷新结果
     */
    RefreshTokenVO refreshToken(RefreshTokenReq refreshToken);

    /**
     * 登出
     * @param logout 登出信息
     * @param accessToken 当前请求头中的 Access Token
     */
    void logout(LogoutReq logout, String accessToken);

    /**
     * 获取用户在线设备(Token)列表
     * @return 用户token列表
     */
    List<TokenInfoVO> getUserToken();

    /**
     * 创建下线指定设备的安全验证 Challenge
     * @param tokenId 设备 Token ID
     * @return Challenge 启动信息
     */
    SecureChallengeStartVO startRevokeChallenge(Long tokenId);

    /**
     * 踢下指定设备 Token
     * @param tokenId 设备 Token ID
     * @param secureActionContext 安全操作上下文
     */
    void revoke(Long tokenId, SecureActionContext secureActionContext);


    /**
     * 获取用户信息
     * @return 用户信息
     */
    UserInfoBO getUserInfo();

    /**
     * 切换当前工作区
     * @param switchReq 工作区选择
     * @param accessToken 当前 JWT（需加入黑名单）
     * @return 新 JWT
     */
    WorkspaceSwitchVO switchWorkspace(WorkspaceSwitchReq switchReq, String accessToken);

}
