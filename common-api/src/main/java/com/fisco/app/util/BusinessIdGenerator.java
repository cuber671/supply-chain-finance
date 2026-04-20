package com.fisco.app.util;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务ID生成器
 * 格式: {前缀}-{日期}-{6位序号}
 * 例如: SIO-20260420-000001
 */
@Component
public class BusinessIdGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成入库单ID
     * 格式: SIO-{日期}-{6位序号}
     */
    public String generateStockInOrderId(long sequence) {
        return buildId("SIO", sequence);
    }

    /**
     * 生成仓单ID
     * 格式: WR-{日期}-{6位序号}
     */
    public String generateWarehouseReceiptId(long sequence) {
        return buildId("WR", sequence);
    }

    /**
     * 生成物流委托ID
     * 格式: LD-{日期}-{6位序号}
     */
    public String generateLogisticsDelegateId(long sequence) {
        return buildId("LD", sequence);
    }

    /**
     * 通用构建方法
     * @param prefix 业务前缀
     * @param sequence 序号（6位，不足补0）
     * @return 格式: PREFIX-YYYYMMDD-XXXXXX
     */
    public String buildId(String prefix, long sequence) {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String sequencePart = String.format("%06d", sequence % 1_000_000);
        return prefix + "-" + datePart + "-" + sequencePart;
    }

    /**
     * 从ID中解析日期
     * 例如: SIO-20260420-000001 -> 20260420
     */
    public String extractDate(String businessId) {
        if (businessId == null || businessId.length() < 17) {
            return null;
        }
        String[] parts = businessId.split("-");
        return parts.length >= 2 ? parts[1] : null;
    }

    /**
     * 从ID中解析序号
     * 例如: SIO-20260420-000001 -> 1
     */
    public long extractSequence(String businessId) {
        if (businessId == null || businessId.length() < 17) {
            return -1;
        }
        String[] parts = businessId.split("-");
        return parts.length >= 3 ? Long.parseLong(parts[2]) : -1;
    }
}