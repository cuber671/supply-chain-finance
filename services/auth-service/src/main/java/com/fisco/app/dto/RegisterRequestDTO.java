package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequestDTO {
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名（唯一）", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    @Schema(description = "密码", example = "********")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "企业ID", example = "1")
    private Long enterpriseId;

    @Schema(description = "用户角色", example = "OPERATOR", allowableValues = {"ADMIN", "FINANCE", "OPERATOR"})
    private String userRole;

    @Schema(description = "邀请码（可选，用于关联邀请企业）", example = "INVITE123")
    private String inviteCode;
}
