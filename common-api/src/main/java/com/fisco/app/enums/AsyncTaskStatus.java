package com.fisco.app.enums;

/**
 * 异步任务状态枚举
 */
public enum AsyncTaskStatus {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    TIMEOUT("TIMEOUT", "超时");

    private final String code;
    private final String description;

    AsyncTaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
