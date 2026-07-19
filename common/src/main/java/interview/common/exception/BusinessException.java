package interview.common.exception;

import interview.common.enums.ErrorCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author zhuxi
 * @apiNote 业务异常
 */

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;
    @Setter
    private String rawExceptionMsg;

    public BusinessException(ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public BusinessException(ErrorCode code, String message) {
        this.code = code.getCode();
        this.message = message;
    }

}
