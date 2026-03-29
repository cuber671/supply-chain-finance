package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 企业服务 Feign 客户端
 */
@FeignClient(name = "enterprise-service", contextId = "enterpriseFeignClient")
public interface EnterpriseFeignClient {

    /**
     * 企业登录
     */
    @PostMapping("/api/v1/enterprise/login")
    Result<?> login(@RequestBody Map<String, String> loginRequest);

    @GetMapping("/api/v1/enterprise/{id}")
    Result<Object> getEnterpriseById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/enterprise/blockchain-address/{address}")
    Result<Object> getEnterpriseByBlockchainAddress(@PathVariable("address") String address);

    @GetMapping("/api/v1/enterprise/check-financial-institution/{entId}")
    Result<Boolean> isFinancialInstitution(@PathVariable("entId") Long entId);

    @GetMapping("/api/v1/enterprise/validate-invite-code")
    Result<Boolean> validateInviteCode(@RequestParam("inviteCode") String inviteCode);
}
