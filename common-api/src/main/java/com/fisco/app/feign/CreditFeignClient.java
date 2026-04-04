package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 信用服务 Feign 客户端
 */
@FeignClient(name = "credit-service", contextId = "creditFeignClient")
public interface CreditFeignClient {

    @GetMapping("/api/v1/credit/score")
    Result<Object> getCreditScore(@RequestParam(value = "entId", required = false) Long entId);

    @GetMapping("/api/v1/credit/limit/available")
    Result<Object> getAvailableCreditLimit(@RequestParam(value = "entId", required = false) Long entId);
}
