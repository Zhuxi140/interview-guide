package interview.common.constant;

import interview.common.enums.ErrorCode;
import interview.common.util.TraceUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote 封装结果类
 */

@AllArgsConstructor
@Getter
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private String traceId;


    public static <T> Result<T> success(T data) {
        long now = System.currentTimeMillis();
        return new Result<>(ErrorCode.SUCCESS.getCode(), "成功", data, now, TraceUtil.getTraceId());
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        long now = System.currentTimeMillis();
        return new Result<>(code, message, null, now, TraceUtil.getTraceId());
    }
}
