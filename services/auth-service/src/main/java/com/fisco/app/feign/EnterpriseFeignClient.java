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
 * 用于企业登录验证
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@FeignClient(name = "enterprise-service", contextId = "enterpriseFeignClient", fallback = EnterpriseFeignClientFallback.class)
public interface EnterpriseFeignClient {

    /**
     * 企业登录
     *
     * @param loginRequest 包含username和password的Map
     * @return 登录结果
     */
    @PostMapping("/api/v1/enterprise/login")
    Result<?> login(@RequestBody Map<String, String> loginRequest);

    @GetMapping("/api/v1/enterprises/{id}")
    Result<Object> getEnterpriseById(@PathVariable("id") Long id);

    @GetMapping("/api/v1/enterprises/validate-invite-code")
    Result<Boolean> validateInviteCode(@RequestParam("inviteCode") String inviteCode);
}
