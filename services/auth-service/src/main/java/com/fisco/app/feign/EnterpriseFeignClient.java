package com.fisco.app.feign;

import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/api/v1/enterprise/{id}")
    Result<Object> getEnterpriseById(@PathVariable("id") Long id);

    /**
     * 校验邀请码并返回邀请企业信息
     * 调用 POST /api/v1/enterprise/invitation/validate
     * 返回 Map 包含 inviterEntId, inviterEnterpriseName, maxUses, usedCount
     */
    @PostMapping("/api/v1/enterprise/invitation/validate")
    Result<Map<String, Object>> validateInvitation(@RequestBody Map<String, String> request);

    /**
     * 使用邀请码（注册成功后调用，原子递增usedCount）
     * 调用 POST /api/v1/enterprise/invite-codes/use
     */
    @PostMapping("/api/v1/enterprise/invite-codes/use")
    Result<Long> useInviteCode(@RequestBody Map<String, String> request);
}
