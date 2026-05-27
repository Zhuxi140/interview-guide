package interview.framework.handler;

import interview.common.Enum.ErrorCode;
import interview.common.constant.Result;
import interview.common.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author zhuxi
 * @since 2026-05-26
 * @apiNote  全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e){
        Integer code = e.getCode();
        String msg = e.getMessage();
        log.warn("业务异常拦截: code={},message:{}",code,msg);
        return Result.error(code,msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e){
        Integer code = e.getCode();
        String msg = e.getMessage();
        log.warn("权限异常拦截: code={},message:{}",code,msg);
        return Result.error(code,msg);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public Result<String> handleTooManyRequestsException(TooManyRequestsException e){
        Integer code = e.getCode();
        String msg = e.getMessage();
        log.warn("请求过于频繁异常拦截: code={},message:{}",code,msg);
        return Result.error(code,msg);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<String> handleUnauthorizedException(UnauthorizedException e){
        Integer code = e.getCode();
        String msg = e.getMessage();
        log.warn("未授权异常拦截: code={},message:{}",code,msg);
        return Result.error(code,msg);
    }

    @ExceptionHandler(SystemException.class)
    public Result<String> handleSystemException(SystemException e){
        Integer code = e.getCode();
        String msg = e.getMessage();
        log.warn("系统内部异常拦截: code={},message:{}",code,msg);
        return Result.error(code,msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e){
        log.error("系统发生未知异常:",e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(),"系统异常");
    }

}
