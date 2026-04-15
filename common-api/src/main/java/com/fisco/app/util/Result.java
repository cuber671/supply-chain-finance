package com.fisco.app.util;

import java.io.Serializable;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fisco.app.enums.ResultCodeEnum;

import lombok.Data;

/**
 * 通用API响应封装类
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    @JsonSerialize(using = LongToStringSerializer.class)
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

    /**
     * 自定义序列化器：仅将超过安全整数范围的数字类型（Long、Integer、BigInteger）
     * 序列化为字符串，其他类型（Map、List、自定义对象）正常序列化为JSON。
     */
    public static class LongToStringSerializer extends JsonSerializer<Object> {
        private static final BigInteger JS_MAX_SAFE_INTEGER = new BigInteger("9007199254740991");

        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws java.io.IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value instanceof Long) {
                long l = (Long) value;
                if (l > JS_MAX_SAFE_INTEGER.longValue() || l < -JS_MAX_SAFE_INTEGER.longValue()) {
                    gen.writeString(String.valueOf(l));
                    return;
                }
            }
            if (value instanceof Integer) {
                gen.writeNumber((Integer) value);
                return;
            }
            if (value instanceof BigInteger) {
                gen.writeString(value.toString());
                return;
            }
            if (value instanceof Short) {
                gen.writeNumber((Short) value);
                return;
            }
            if (value instanceof Byte) {
                gen.writeNumber((Byte) value);
                return;
            }
            // 其他类型（Map、List、String等）正常JSON序列化
            serializers.defaultSerializeValue(value, gen);
        }
    }
}
