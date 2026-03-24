package com.fisco.app.feign;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 仓库服务 Feign 客户端降级处理
 */
@Component
public class WarehouseFeignClientFallback implements WarehouseFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseFeignClientFallback.class);

    @Override
    public Object getReceiptById(Long id) {
        logger.warn("调用warehouse-service失败，使用降级返回: getReceiptById({})", id);
        return null;
    }

    @Override
    public Object getReceiptByNo(String receiptNo) {
        logger.warn("调用warehouse-service失败，使用降级返回: getReceiptByNo({})", receiptNo);
        return null;
    }

    @Override
    public Object getReceiptStatus(Long id) {
        logger.warn("调用warehouse-service失败，使用降级返回: getReceiptStatus({})", id);
        return null;
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
}
