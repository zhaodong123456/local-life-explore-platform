package com.explore.platform.common.exception;

import com.explore.platform.common.result.ErrorCode;
import com.explore.platform.common.result.Result;
import jakarta.validation.ConstraintViolationException;//校验异常
import lombok.extern.slf4j.Slf4j;//日志注解，用于在类中使用 log 记录日志
import org.springframework.http.converter.HttpMessageNotReadableException;//HTTP 消息转换异常，用于处理请求体转换失败的情况
import org.springframework.security.access.AccessDeniedException;//权限不足异常
import org.springframework.security.core.AuthenticationException;//认证异常
import org.springframework.validation.BindException;//绑定异常，用于处理表单数据绑定失败的情况
import org.springframework.validation.FieldError;//字段错误，用于表示表单字段校验失败的情况
import org.springframework.web.HttpRequestMethodNotSupportedException;//不支持的 HTTP 方法异常
import org.springframework.web.bind.MethodArgumentNotValidException;//方法参数校验失败异常
import org.springframework.web.bind.MissingServletRequestParameterException;//缺少请求参数异常
import org.springframework.web.bind.annotation.ExceptionHandler;//异常处理注解，用于指定处理的异常类型
import org.springframework.web.bind.annotation.RestControllerAdvice;//控制器建议注解，用于在类中定义全局异常处理方法

import java.util.stream.Collectors;//流操作，用于处理集合等数据结构

/**
 * 全局异常处理器
 *
 * @author explore-platform
 */
@Slf4j
@RestControllerAdvice//控制器建议注解，用于在类中定义全局异常处理方法
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 认证异常（登录失败、Token 无效等）
     */
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败：{}", e.getMessage());
        return Result.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * @RequestBody 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return Result.fail(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 表单参数绑定失败
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败：{}", message);
        return Result.fail(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 单个参数校验失败（@RequestParam / @PathVariable 上的 @Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("约束校验失败：{}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数：{}", e.getParameterName());
        return Result.fail(ErrorCode.PARAM_ERROR, "缺少请求参数：" + e.getParameterName());
    }

    /**
     * 请求体不可读 / JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误：{}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR, "请求体格式错误");
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMethod());
        return Result.fail(ErrorCode.PARAM_ERROR, "请求方法不支持：" + e.getMethod());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
