package com.fisco.app.feign;

import com.fisco.app.dto.TokenResponseDTO;
import com.fisco.app.util.Result;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 认证服务 Feign 客户端
 */
@FeignClient(name = "auth-service", contextId = "authFeignClient")
public interface AuthFeignClient {

    @GetMapping("/api/v1/auth/validate")
    Result<Boolean> validateToken(@RequestParam("accessToken") String accessToken);

    @GetMapping("/api/v1/auth/userinfo/{userId}")
    Result<TokenResponseDTO> getUserInfo(@PathVariable("userId") Long userId);

    @PostMapping("/api/v1/auth/refresh")
    Result<TokenResponseDTO> refreshToken(@RequestBody String refreshToken);
}
