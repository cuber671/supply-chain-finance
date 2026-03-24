package com.fisco.app.feign;

import com.fisco.app.enums.ResultCodeEnum;
import com.fisco.app.util.Result;

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
    public String getBlockchainAddress(Long entId) {
        logger.warn("企业服务降级: getBlockchainAddress, entId={}", entId);
        return null;
    }

    @Override
    public Result<Object> getEnterpriseById(Long entId) {
        logger.warn("企业服务降级: getEnterpriseById, entId={}", entId);
        return Result.error(ResultCodeEnum.SERVICE_UNAVAILABLE);
    }
}
