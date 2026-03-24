package com.fisco.app.feign;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 仓单服务 Feign 客户端降级处理
 *
 * 当仓单服务不可用时，提供降级实现
 */
@Component
public class WarehouseFeignClientFallback implements WarehouseFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseFeignClientFallback.class);

    @Override
    public Map<String, Object> getReceiptById(Long receiptId) {
        logger.warn("仓单服务降级: getReceiptById, receiptId={}", receiptId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> getReceiptByOnChainId(String onChainId) {
        logger.warn("仓单服务降级: getReceiptByOnChainId, onChainId={}", onChainId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> lockReceipt(Long receiptId, Map<String, String> params) {
        logger.warn("仓单服务降级: lockReceipt, receiptId={}", receiptId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> unlockReceipt(Long receiptId) {
        logger.warn("仓单服务降级: unlockReceipt, receiptId={}", receiptId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> getWarehouseById(Long warehouseId) {
        logger.warn("仓单服务降级: getWarehouseById, warehouseId={}", warehouseId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> mintReceipt(Map<String, Object> params) {
        logger.warn("仓单服务降级: mintReceipt");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> mergeReceipt(Map<String, Object> params) {
        logger.warn("仓单服务降级: mergeReceipt");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }
}
