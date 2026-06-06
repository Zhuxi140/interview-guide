package interview.system.auth;


import interview.system.auth.model.bo.LoginBO;
import interview.system.auth.model.bo.RegisterBo;
import interview.system.auth.model.bo.UserInfoBO;
import interview.system.auth.model.vo.LoginVO;
import interview.system.auth.model.vo.RegisterVO;
import interview.system.auth.model.vo.UserInfoVO;
import org.mapstruct.Mapper;

/**
 * @author zhuxi
 * @apiNote 认证转换器
 * @since 2026/5/27 14:05
 */


@Mapper(componentModel = "spring")
public interface AuthConvertor {
    RegisterVO toRegisterVO(RegisterBo registerBo);

    LoginVO toLoginVO(LoginBO loginBO);

    UserInfoVO toUserInfoVO(UserInfoBO userInfoBO);
}
