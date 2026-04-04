package com.fisco.app.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 信用服务 Feign 客户端
 *
 * 企业服务通过此接口调用信用服务进行企业状态变更同步
 */
@FeignClient(name = "credit-service", contextId = "enterpriseCreditClient", fallback = CreditFeignClientFallback.class)
public interface CreditFeignClient {

    /**
     * 锁定企业信用额度
     * 用于企业注销审核通过后锁定该企业的信用额度
     *
     * @return 操作结果
     */
    @PostMapping("/api/v1/credit/limit/lock")
    Map<String, Object> lockCreditLimit();
}