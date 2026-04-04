package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 金融服务 Feign 客户端
 *
 * 信用服务通过此接口调用金融服务进行贷款状态查询
 */
@FeignClient(name = "finance-service", contextId = "creditFinanceClient", fallback = FinanceFeignClientFallback.class)
public interface FinanceFeignClient {

    /**
     * 根据ID获取贷款详情
     *
     * @param id 贷款ID
     * @return 贷款详情
     */
    @GetMapping("/api/v1/finance/loan/{id}")
    Result<Object> getLoanById(@PathVariable("id") Long id);
}