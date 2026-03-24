package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 信用服务 Feign 客户端
 */
@FeignClient(name = "credit-service", contextId = "creditFeignClient")
public interface CreditFeignClient {

    @GetMapping("/api/v1/credit/score/{entId}")
    Result<Object> getCreditScore(@PathVariable("entId") Long entId);

    @GetMapping("/api/v1/credit/available-limit/{entId}")
    Result<Object> getAvailableCreditLimit(@PathVariable("entId") Long entId);
}
