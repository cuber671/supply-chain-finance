package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 企业服务 Feign 客户端
 *
 * 用于信用服务调用企业服务获取企业信息
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@FeignClient(name = "enterprise-service", path = "/api/v1/enterprise", fallback = EnterpriseFeignClientFallback.class)
public interface EnterpriseFeignClient {

    /**
     * 根据企业ID获取区块链地址
     *
     * @param entId 企业ID
     * @return 区块链地址
     */
    @GetMapping("/address/{entId}")
    String getBlockchainAddress(@PathVariable("entId") Long entId);

    /**
     * 根据ID获取企业信息
     *
     * @param entId 企业ID
     * @return 企业信息
     */
    @GetMapping("/enterprises/{entId}")
    Result<Object> getEnterpriseById(@PathVariable("entId") Long entId);
}
