package com.fisco.app.feign;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 企业服务 Feign 客户端降级处理
 *
 * 当企业服务不可用时，提供降级实现
 */
@Component
public class EnterpriseFeignClientFallback implements EnterpriseFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseFeignClientFallback.class);

    @Override
    public Map<String, Object> isFinancialInstitution(Long entId) {
        logger.warn("企业服务降级: isFinancialInstitution, entId={}, assuming not FI", entId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "企业服务暂不可用");
        result.put("data", false);  // Fail safe - deny if service unavailable
        return result;
    }
}