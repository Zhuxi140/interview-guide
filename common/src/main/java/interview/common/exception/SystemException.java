package interview.common.exception;

import interview.common.Enum.ErrorCode;
import lombok.Getter;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 500 系统内部异常
 */
@Getter
public class SystemException extends RuntimeException {

    private final Integer code;
    private final String message;

    public SystemException() {
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.message = ErrorCode.SYSTEM_ERROR.getMessage();
    }

    public SystemException(ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public SystemException(ErrorCode code, String message) {
        this.code = code.getCode();
        this.message = message;
    }
}
