package interview.common.exception;

import interview.common.enums.ErrorCode;
import lombok.Getter;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 403 权限不足异常
 */
@Getter
public class AccessDeniedException extends RuntimeException {

    private final Integer code;
    private final String message;

    public AccessDeniedException() {
        this.code = ErrorCode.PERMISSION_DENIED.getCode();
        this.message = ErrorCode.PERMISSION_DENIED.getMessage();
    }

    public AccessDeniedException(String message) {
        this.code = ErrorCode.PERMISSION_DENIED.getCode();
        this.message = message;
    }
}
