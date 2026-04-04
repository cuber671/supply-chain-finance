package com.fisco.app.feign;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 信用服务 Feign 客户端降级处理
 *
 * 当信用服务不可用时，提供降级实现
 */
@Component
public class CreditFeignClientFallback implements CreditFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(CreditFeignClientFallback.class);

    @Override
    public Map<String, Object> lockCreditLimit() {
        logger.warn("信用服务降级: lockCreditLimit, credit-service unavailable");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("msg", "信用服务暂不可用，额度锁定已记录但未同步");
        return result;
    }
}