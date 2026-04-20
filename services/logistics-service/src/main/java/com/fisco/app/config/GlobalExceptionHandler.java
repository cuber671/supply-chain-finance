package com.fisco.app.config;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.fisco.app.util.Result;

/**
 * 全局异常处理器
 *
 * 统一处理各层抛出的业务异常，返回标准化错误响应
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Bean Validation 校验失败（@Valid 注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        logger.warn("参数校验失败: {}", errors);
        return Result.error(400, "参数校验失败: " + errors);
    }

    /**
     * 处理 @Validated 校验失败（表单绑定异常）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleBindException(BindException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        logger.warn("参数绑定失败: {}", errors);
        return Result.error(400, "参数绑定失败: " + errors);
    }

    /**
     * 处理缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleMissingParam(MissingServletRequestParameterException e) {
        logger.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(400, "缺少请求参数: " + e.getParameterName());
    }

    /**
     * 处理请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> requiredType = e.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "未知";
        String msg = String.format("参数 %s 类型错误，期望 %s", e.getName(), typeName);
        logger.warn("参数类型不匹配: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 处理业务 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("业务参数异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理业务 IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Object> handleIllegalState(IllegalStateException e) {
        logger.warn("业务状态异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        logger.warn("不支持的请求方法: {}", e.getMethod());
        return Result.error(405, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 处理 404
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Object> handleNoHandler(NoHandlerFoundException e) {
        logger.warn("接口不存在: {}", e.getRequestURL());
        return Result.error(404, "接口不存在: " + e.getRequestURL());
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Object> handleGeneralException(Exception e) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return Result.error(500, "系统异常: " + e.getMessage());
    }
}
