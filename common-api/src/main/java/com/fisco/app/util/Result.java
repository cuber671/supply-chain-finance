package com.fisco.app.util;

import java.io.Serializable;

import com.fisco.app.enums.ResultCodeEnum;

import lombok.Data;

/**
 * 通用API响应封装类
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;
    private String txHash;
    private String errorStack;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMsg(ResultCodeEnum.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data, String txHash) {
        Result<T> result = success(data);
        result.setTxHash(txHash);
        return result;
    }

    public static <T> Result<T> accepted(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.ACCEPTED.getCode());
        result.setMsg(ResultCodeEnum.ACCEPTED.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> accepted(T data, String taskId) {
        Result<T> result = accepted(data);
        result.setMsg("任务已提交，taskId: " + taskId);
        return result;
    }

    public static <T> Result<T> paramError(String customMessage) {
        return error(ResultCodeEnum.PARAM_ERROR, customMessage);
    }

    public static <T> Result<T> unauthorized() {
        return error(ResultCodeEnum.UNAUTHORIZED);
    }

    public static <T> Result<T> forbidden() {
        return error(ResultCodeEnum.FORBIDDEN);
    }

    public static <T> Result<T> notFound() {
        return error(ResultCodeEnum.NOT_FOUND);
    }

    public static <T> Result<T> systemError() {
        return error(ResultCodeEnum.SYSTEM_ERROR);
    }

    public static <T> Result<T> error(ResultCodeEnum codeEnum) {
        return error(codeEnum.getCode(), codeEnum.getMessage());
    }

    public static <T> Result<T> error(ResultCodeEnum codeEnum, String customMessage) {
        return error(codeEnum.getCode(), customMessage);
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message, String errorStack) {
        Result<T> result = error(code, message);
        result.setErrorStack(errorStack);
        return result;
    }
}
