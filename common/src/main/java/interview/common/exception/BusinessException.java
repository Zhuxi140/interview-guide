package interview.common.exception;

import interview.common.Enum.ErrorCode;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 业务异常
 */
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

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
