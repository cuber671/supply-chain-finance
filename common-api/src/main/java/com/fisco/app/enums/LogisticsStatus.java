package com.fisco.app.enums;

/**
 * 物流状态枚举
 * 对应链上 ILogisticsCore.LogisticsStatus
 */
public enum LogisticsStatus {
    NONE(0, "不存在"),
    PENDING(1, "待指派"),
    ASSIGNED(2, "已调度"),
    IN_TRANSIT(3, "运输中"),
    DELIVERED(4, "已交付"),
    INVALID(5, "已失效");

    private final int value;
    private final String description;

    LogisticsStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从链上状态值转换为枚举
     * @param chainStatus 链上状态值
     * @return 对应的枚举，如果未找到则返回 null
     */
    public static LogisticsStatus fromChainStatus(int chainStatus) {
        for (LogisticsStatus status : values()) {
            if (status.value == chainStatus) {
                return status;
            }
        }
        return null;
    }

    /**
     * 转换为链上状态值
     * @return 链上状态值
     */
    public int toChainStatus() {
        return value;
    }
}