package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "用户注册请求")
public class RegisterRequestDTO {
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名（唯一）", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "password123")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "企业ID", example = "1")
    private Long enterpriseId;

    @Schema(description = "用户角色", example = "OPERATOR")
    private String userRole;

    @Schema(description = "邀请码（可选，用于关联邀请企业）", example = "INVITE123")
    private String inviteCode;
}