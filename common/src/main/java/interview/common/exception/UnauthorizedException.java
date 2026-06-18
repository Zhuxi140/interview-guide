package interview.common.exception;

import interview.common.enums.ErrorCode;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 401 用户未登录/Token 无效异常
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final Integer code;
    private final String message;

    public UnauthorizedException() {
        this.code = ErrorCode.USER_NOT_LOGIN.getCode();
        this.message = ErrorCode.USER_NOT_LOGIN.getMessage();
    }

    public UnauthorizedException(String message) {
        this.code = ErrorCode.USER_NOT_LOGIN.getCode();
        this.message = message;
    }
}
