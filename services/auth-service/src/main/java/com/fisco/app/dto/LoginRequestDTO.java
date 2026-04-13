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

    @SuppressWarnings("deprecation")
    @Schema(description = "密码", example = "********", writeOnly = true)
    private String password;

    @Schema(description = "登录类型", example = "USER", allowableValues = {"USER", "ENTERPRISE"})
    private String loginType;
}
