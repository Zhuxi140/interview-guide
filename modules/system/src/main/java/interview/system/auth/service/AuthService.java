package interview.system.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.entity.UserToken;
import interview.system.auth.model.req.*;
import interview.system.auth.model.vo.RefreshTokenVO;
import interview.system.auth.model.vo.TokenInfoVO;

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
     * @return 登录结果
     */
    LoginBO login(LoginReq loginReq);

    /**
     * 刷新token
     * @param refreshToken 刷新token
     * @return 刷新结果
     */
    RefreshTokenVO refreshToken(RefreshTokenReq refreshToken);

    /**
     * 登出
     * @param logout 登出信息
     */
    void logout(LogoutReq logout);

    /**
     * 获取用户在线设备(Token)列表
     * @return 用户token列表
     */
    List<TokenInfoVO> getUserToken();

    /**
     * 踢下指定设备(Token)
     * @param tokenId tokenId
     */
    void revoke(Long tokenId, RevokeDeviceReq code);


    /**
     * 获取用户信息
     * @return 用户信息
     */
    UserInfoBO getUserInfo();


}
