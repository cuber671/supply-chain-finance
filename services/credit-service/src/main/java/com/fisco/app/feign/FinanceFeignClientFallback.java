package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 金融服务 Feign 客户端降级处理
 *
 * 当金融服务不可用时，提供降级实现
 */
@Component
public class FinanceFeignClientFallback implements FinanceFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(FinanceFeignClientFallback.class);

    @Override
    public Result<Object> getLoanById(Long id) {
        logger.warn("金融服务降级: getLoanById, loanId={}", id);
        return Result.error(503, "金融服务暂不可用");
    }
}