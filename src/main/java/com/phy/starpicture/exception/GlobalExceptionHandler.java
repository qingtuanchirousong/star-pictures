package com.phy.starpicture.exception;

import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
    }
}
