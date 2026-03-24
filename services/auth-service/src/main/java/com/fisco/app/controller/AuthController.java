package com.fisco.app.controller;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.LoginRequestDTO;
import com.fisco.app.dto.RefreshTokenRequestDTO;
import com.fisco.app.dto.TokenResponseDTO;
import com.fisco.app.entity.LoginTransaction;
import com.fisco.app.feign.EnterpriseFeignClient;
import com.fisco.app.service.EnterpriseLoginService;
import com.fisco.app.service.TokenService;
import com.fisco.app.service.UserService;
import com.fisco.app.util.JwtUtil;
import com.fisco.app.util.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证控制器 - 双令牌策略
 * 提供登录、刷新Token等接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "用户认证", description = "登录、登出、Token刷新、Token验证接口")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TokenService tokenService;
    private final UserService userService;
    private final EnterpriseFeignClient enterpriseFeignClient;
    private final EnterpriseLoginService enterpriseLoginService;

    public AuthController(TokenService tokenService,
                          UserService userService,
                          EnterpriseFeignClient enterpriseFeignClient,
                          EnterpriseLoginService enterpriseLoginService) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.enterpriseFeignClient = enterpriseFeignClient;
        this.enterpriseLoginService = enterpriseLoginService;
    }

    /**
     * 登录接口 - 验证凭证并生成双令牌
     */
    @Operation(summary = "用户/企业登录", description = "支持用户登录(USER)和企业登录(ENTERPRISE)两种模式。\n" +
            "- **用户登录**：验证用户名密码，返回用户信息和企业ID\n" +
            "- **企业登录**：调用enterprise-service验证企业凭证，支持TCC事务追踪\n\n" +
            "登录成功后返回 accessToken 和 refreshToken 双令牌。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或业务异常", content = @Content),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            String loginType = loginRequest.getLoginType();

            Long userId;
            Long entId;
            String role;
            Integer scope;
            String txUuid = null;  // 企业登录事务ID，用于错误追踪

            if ("USER".equalsIgnoreCase(loginType)) {
                // 用户登录
                var user = userService.login(username, password);
                if (user == null) {
                    return buildErrorResponse(401, "用户名或密码错误");
                }
                userId = user.getUserId();
                entId = user.getEnterpriseId();
                role = user.getUserRole() != null ? user.getUserRole() : "USER";
                scope = 1;
            } else {
                // 企业登录（默认）- 调用enterprise-service，带TCC事务追踪
                // Try阶段：创建登录事务
                txUuid = enterpriseLoginService.tryLogin(username, password);

                try {
                    // 调用enterprise-service（Confirm/Cancel由EnterpriseLoginService内部处理）
                    Map<String, String> entLoginRequest = new HashMap<>();
                    entLoginRequest.put("username", username);
                    entLoginRequest.put("password", password);
                    Result<?> enterpriseResult = enterpriseFeignClient.login(entLoginRequest);

                    if (enterpriseResult == null || enterpriseResult.getCode() != 0) {
                        // 登录失败，Cancel事务
                        String msg = enterpriseResult != null ? enterpriseResult.getMsg() : "企业登录失败";
                        enterpriseLoginService.cancelLogin(txUuid, msg);
                        return buildErrorResponse(401, msg);
                    }
                    Object data = enterpriseResult.getData();
                    if (!(data instanceof Map)) {
                        enterpriseLoginService.cancelLogin(txUuid, "返回数据格式错误");
                        return buildErrorResponse(401, "企业登录失败：返回数据格式错误");
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> enterpriseData = (Map<String, Object>) data;
                    userId = ((Number) enterpriseData.get("entId")).longValue();
                    entId = userId;
                    role = "ENTERPRISE";
                    scope = 5;

                    // Confirm事务
                    String sessionId = String.valueOf(enterpriseData.get("sessionId"));
                    enterpriseLoginService.confirmLogin(txUuid, entId, sessionId);
                } catch (Exception e) {
                    // 调用超时或异常，Cancel事务
                    log.error("企业登录调用异常: txUuid={}, error={}", txUuid, e.getMessage());
                    enterpriseLoginService.cancelLogin(txUuid, "服务调用异常: " + e.getMessage());
                    return buildErrorResponse(500, "企业登录服务暂时不可用，请稍后重试");
                }
            }

            // 生成令牌对
            // FIX: 若此处异常但企业登录已Confirm，需记录异常供后台补偿任务处理
            Map<String, String> tokenPair;
            try {
                tokenPair = tokenService.generateTokenPair(userId, entId, role, scope, null);
            } catch (Exception e) {
                // 企业登录已确认但Token生成失败，记录异常供人工/定时任务处理
                log.error("Token生成失败但企业登录已确认, txUuid={}, userId={}, entId={}, error={}",
                          txUuid, userId, entId, e.getMessage());
                // 后续补偿任务应扫描异常记录并重试Token生成或回滚状态
                return buildErrorResponse(500, "登录过程中出现异常，请稍后重试");
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");

            TokenResponseDTO tokenResponse = TokenResponseDTO.of(
                    tokenPair.get("accessToken"),
                    tokenPair.get("refreshToken"),
                    JwtUtil.ACCESS_TOKEN_EXPIRATION / 1000,
                    userId,
                    entId
            );
            response.put("data", tokenResponse);

            log.info("用户登录成功，用户ID: {}, 企业ID: {}", userId, entId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("登录业务异常: {}", e.getMessage());
            return buildErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 构建错误响应
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (code >= 500) {
            return ResponseEntity.status(code).body(error);
        } else if (code >= 400) {
            return ResponseEntity.badRequest().body(error);
        } else {
            return ResponseEntity.status(code).body(error);
        }
    }

    /**
     * 管理员登录接口
     * 平台管理员（scope=1）通过此接口登录
     */
    @Operation(summary = "管理员登录", description = "平台超级管理员登录接口。登录成功后返回 accessToken 和 refreshToken 双令牌，entId 为 null。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            // 使用 UserService 验证管理员凭证
            var user = userService.login(username, password);
            if (user == null) {
                return buildErrorResponse(401, "用户名或密码错误");
            }

            // 管理员登录：userId, entId=null, role=ADMIN, scope=1
            Long userId = user.getUserId();
            String role = "ADMIN";
            Integer scope = 1;

            // 生成令牌对
            Map<String, String> tokenPair = tokenService.generateTokenPair(userId, null, role, scope, null);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "管理员登录成功");

            TokenResponseDTO tokenResponse = TokenResponseDTO.of(
                    tokenPair.get("accessToken"),
                    tokenPair.get("refreshToken"),
                    JwtUtil.ACCESS_TOKEN_EXPIRATION / 1000,
                    userId,
                    null  // 管理员无企业归属
            );
            response.put("data", tokenResponse);

            log.info("管理员登录成功，用户ID: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("管理员登录失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 刷新Token接口
     */
    @Operation(summary = "刷新访问令牌", description = "使用 refreshToken 获取新的 accessToken 和 refreshToken 对。\n" +
            "**注意**：此接口不需要 Authorization 头，使用 refreshToken 进行认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "刷新成功", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Refresh Token为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "Refresh Token无效或已过期", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        try {
            if (!request.isValid()) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 400);
                error.put("message", "Refresh Token不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, String> newTokenPair = tokenService.refreshToken(request.getRefreshToken());

            if (newTokenPair == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 401);
                error.put("message", "Refresh Token无效或已过期");
                return ResponseEntity.status(401).body(error);
            }

            String accessToken = newTokenPair.get("accessToken");
            Map<String, Object> userInfo = tokenService.parseAccessToken(accessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Token刷新成功");

            TokenResponseDTO tokenResponse = TokenResponseDTO.of(
                    accessToken,
                    newTokenPair.get("refreshToken"),
                    JwtUtil.ACCESS_TOKEN_EXPIRATION / 1000,
                    (Long) userInfo.get("userId"),
                    (Long) userInfo.get("entId")
            );
            response.put("data", tokenResponse);

            log.info("Token刷新成功，用户ID: {}", userInfo.get("userId"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Token刷新失败，请稍后重试");
        }
    }

    /**
     * 健康检查接口 - 验证Token有效性
     */
    @Operation(summary = "验证访问令牌", description = "验证 accessToken 的有效性，返回 Token 中包含的用户信息。\n" +
            "**注意**：此接口不需要 Authorization 头，直接传入 accessToken。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token有效", content = @Content),
        @ApiResponse(responseCode = "400", description = "Access Token为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "Token无效或已过期", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        try {
            String accessToken = request.get("accessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 400);
                error.put("message", "Access Token不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            boolean valid = tokenService.validateAccessToken(accessToken);

            Map<String, Object> response = new HashMap<>();
            if (valid) {
                Map<String, Object> userInfo = tokenService.parseAccessToken(accessToken);
                response.put("code", 200);
                response.put("message", "Token有效");
                response.put("data", userInfo);
            } else {
                response.put("code", 401);
                response.put("message", "Token无效或已过期");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage(), e);
            return buildErrorResponse(500, "Token验证失败，请稍后重试");
        }
    }

    /**
     * 查询登录事务状态 - TCC补偿机制状态查询
     * 客户端可轮询此接口获取企业登录结果
     */
    @Operation(summary = "查询企业登录事务状态", description = "查询TCC分布式事务中的企业登录状态。\n" +
            "用于企业登录时 Try 阶段成功但 Confirm/Cancel 阶段结果未知时，客户端轮询查询最终状态。\n\n" +
            "**txUuid**：登录接口返回的事务唯一标识")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "txUuid为空", content = @Content),
        @ApiResponse(responseCode = "404", description = "事务不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/login/status")
    public ResponseEntity<Map<String, Object>> queryLoginStatus(@RequestBody Map<String, String> request) {
        String txUuid = request.get("txUuid");
        if (txUuid == null || txUuid.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", "txUuid不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        LoginTransaction tx = enterpriseLoginService.getTransactionStatus(txUuid);
        Map<String, Object> response = new HashMap<>();

        if (tx == null) {
            response.put("code", 404);
            response.put("message", "事务不存在");
            return ResponseEntity.ok(response);
        }

        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", Map.of(
                "txUuid", tx.getTxUuid(),
                "status", tx.getStatus(),
                "username", tx.getUsername(),
                "enterpriseEntId", tx.getEnterpriseEntId() != null ? tx.getEnterpriseEntId() : "",
                "errorMsg", tx.getErrorMsg() != null ? tx.getErrorMsg() : "",
                "tryTime", tx.getTryTime() != null ? tx.getTryTime().toString() : ""
        ));

        return ResponseEntity.ok(response);
    }
}
