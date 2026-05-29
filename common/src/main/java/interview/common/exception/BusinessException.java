package interview.common.exception;

import interview.common.enums.ErrorCode;
import lombok.Getter;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 业务异常
 */

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BusinessException(ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public BusinessException(ErrorCode code, String message) {
        this.code = code.getCode();
        this.message = message;
    }
}
