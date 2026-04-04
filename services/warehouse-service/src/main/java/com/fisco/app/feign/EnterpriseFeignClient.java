package com.fisco.app.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 企业服务 Feign 客户端
 *
 * 仓库服务通过此接口调用企业服务进行金融机构身份验证
 */
@FeignClient(name = "enterprise-service", contextId = "warehouseEnterpriseClient", fallback = EnterpriseFeignClientFallback.class)
public interface EnterpriseFeignClient {

    @GetMapping("/api/v1/enterprise/check-financial-institution/{entId}")
    Map<String, Object> isFinancialInstitution(@PathVariable("entId") Long entId);
}