package com.fisco.app.feign;

import com.fisco.app.enums.ResultCodeEnum;
import com.fisco.app.util.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 企业服务 Feign 客户端降级处理
 *
 * 当企业服务不可用时，提供降级实现
 */
@Component
public class EnterpriseFeignClientFallback implements EnterpriseFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseFeignClientFallback.class);

    @Override
    public Result<?> login(Map<String, String> loginRequest) {
        logger.warn("企业服务降级: login, username={}", loginRequest.get("username"));
        return Result.error(ResultCodeEnum.SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Object> getEnterpriseById(Long id) {
        logger.warn("企业服务降级: getEnterpriseById, id={}", id);
        return Result.error(ResultCodeEnum.SERVICE_UNAVAILABLE);
    }

    @Override
    public Result<Boolean> validateInviteCode(String inviteCode) {
        logger.warn("企业服务降级: validateInviteCode, inviteCode={}", inviteCode);
        return Result.error(ResultCodeEnum.SERVICE_UNAVAILABLE);
    }
}
