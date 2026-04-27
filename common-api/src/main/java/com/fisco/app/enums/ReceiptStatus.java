package com.fisco.app.enums;

/**
 * 仓单状态枚举
 * 对应链上 WarehouseReceiptCore.ReceiptStatus
 * 注意：实体类使用此枚举时需要与 t_warehouse_receipt.status 列对应
 */
public enum ReceiptStatus {
    NONE(0, "不存在"),
    IN_STOCK(1, "在库"),              // 对应实体 STATUS_IN_STOCK
    PENDING_TRANSFER(2, "待转让"),    // 对应实体 STATUS_PENDING_TRANSFER
    SPLIT_MERGED(3, "已拆分/合并"),    // 对应实体 STATUS_SPLIT_MERGED
    BURNED(4, "已核销"),              // 对应实体 STATUS_BURNED
    IN_TRANSIT(5, "物流转运中"),      // 对应实体 STATUS_IN_TRANSIT
    VOID(6, "已作废"),                // 对应实体 STATUS_VOID
    WAIT_LOGISTICS(7, "待物流");      // 对应实体 STATUS_WAIT_LOGISTICS (禁止拆分/转让/再次创建物流)

    private final int value;
    private final String description;

    ReceiptStatus(int value, String description) {
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
    public static ReceiptStatus fromChainStatus(int chainStatus) {
        for (ReceiptStatus status : values()) {
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