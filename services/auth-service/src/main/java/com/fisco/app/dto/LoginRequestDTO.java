package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
@Schema(description = "登录请求")
public class LoginRequestDTO {
    @Schema(description = "用户名/企业账号", example = "admin")
    private String username;

    @Schema(description = "密码", example = "password123")
    private String password;

    @Schema(description = "登录类型：USER-用户登录, ENTERPRISE-企业登录", example = "USER")
    private String loginType;
}