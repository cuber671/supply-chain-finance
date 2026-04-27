package com.fisco.app.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录请求DTO - 替代Map接收参数，提供完整的参数校验能力
 */
@Schema(description = "用户登录请求")
public class LoginRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 50, message = "用户名长度必须在1-50之间")
    @Schema(description = "用户名", example = "admin001")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "********")
    private String password;

    @Pattern(regexp = "USER|ENTERPRISE", message = "登录类型无效，仅支持USER或ENTERPRISE")
    @Schema(description = "登录类型", example = "ENTERPRISE", allowableValues = {"USER", "ENTERPRISE"})
    private String loginType = "ENTERPRISE";

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String username, String password, String loginType) {
        this.username = username;
        this.password = password;
        this.loginType = loginType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public boolean isValid() {
        return username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty();
    }
}
