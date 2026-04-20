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
    public Map<String, Object> markWaitLogistics(Long receiptId, String voucherNo) {
        logger.warn("仓单服务降级: markWaitLogistics, receiptId={}", receiptId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> clearWaitLogistics(Long receiptId) {
        logger.warn("仓单服务降级: clearWaitLogistics, receiptId={}", receiptId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> setInTransit(Long receiptId) {
        logger.warn("仓单服务降级: setInTransit, receiptId={}", receiptId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> updateReceiptRemark(Long receiptId, String remark) {
        logger.warn("仓单服务降级: updateReceiptRemark, receiptId={}, remark={}", receiptId, remark);
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
    public Map<String, Object> mintDirectReceipt(Map<String, Object> params) {
        logger.warn("仓单服务降级: mintDirectReceipt");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> applySplit(Map<String, Object> params) {
        logger.warn("仓单服务降级: applySplit");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> executeSplitMerge(Long opLogId, Boolean execute, Long authorizedCarrierEntId) {
        logger.warn("仓单服务降级: executeSplitMerge, opLogId={}, execute={}, authorizedCarrierEntId={}",
                opLogId, execute, authorizedCarrierEntId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> voidReceipt(Long receiptId, String reason) {
        logger.warn("仓单服务降级: voidReceipt, receiptId={}, reason={}", receiptId, reason);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> applyStockIn(Map<String, Object> params) {
        logger.warn("仓单服务降级: applyStockIn");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> applyStockInAndConfirm(Map<String, Object> params, Long actualWarehouseId) {
        logger.warn("仓单服务降级: applyStockInAndConfirm");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> createStockInConfirmed(Map<String, Object> params, Long actualWarehouseId) {
        logger.warn("仓单服务降级: createStockInConfirmed");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }

    @Override
    public Map<String, Object> updateStockOrderStatus(Long stockOrderId, Integer status, String remark) {
        logger.warn("仓单服务降级: updateStockOrderStatus, stockOrderId={}, status={}, remark={}",
                stockOrderId, status, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "仓单服务暂不可用");
        return result;
    }
}
