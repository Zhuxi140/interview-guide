package interview.common.exception;

import interview.common.enums.ErrorCode;
import lombok.Getter;

/**
 * @author zhuxi
 * @apiNote 429 请求过于频繁异常
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final Integer code;
    private final String message;

    public TooManyRequestsException() {
        this.code = ErrorCode.TOO_MANY_REQUESTS.getCode();
        this.message = ErrorCode.TOO_MANY_REQUESTS.getMessage();
    }

    public TooManyRequestsException(String message) {
        this.code = ErrorCode.TOO_MANY_REQUESTS.getCode();
        this.message = message;
    }
}
