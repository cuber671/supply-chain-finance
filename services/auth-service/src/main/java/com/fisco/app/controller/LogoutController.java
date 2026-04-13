package com.fisco.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 登出控制器
 * 提供Token吊销功能，将JWT加入黑名单
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "登出", description = "Token吊销接口")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class LogoutController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public LogoutController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 登出接口
     * 将当前Token加入黑名单，强制其失效
     */
    @Operation(summary = "用户登出", description = "将当前访问令牌加入黑名单，强制其失效。\n\n" +
            "**注意**：即使 Token 已无效或已过期，也返回成功。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登出成功", content = @Content),
        @ApiResponse(responseCode = "401", description = "无效的Authorization头", content = @Content),
        @ApiResponse(responseCode = "500", description = "登出失败，服务暂时不可用", content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@Parameter(description = "Bearer Token", required = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("登出失败：未提供有效的Authorization头");
            Map<String, Object> error = new HashMap<>();
            error.put("code", 401);
            error.put("message", "Invalid authorization header");
            return ResponseEntity.status(401).body(error);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            boolean revoked = tokenService.revokeToken(token);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Logout successful");
            if (revoked) {
                log.info("Token吊销成功");
            } else {
                // Token无效（格式错误或已过期），业务上视为登出成功
                log.info("Token无效或已过期，视为登出成功");
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // 业务预期内异常（Token格式错误等），视为登出成功
            log.warn("Token格式错误: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 系统异常（Redis连接失败等），不应返回成功
            log.error("Token吊销系统异常: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "登出失败，服务暂时不可用");
            return ResponseEntity.status(500).body(error);
        }
    }
}
