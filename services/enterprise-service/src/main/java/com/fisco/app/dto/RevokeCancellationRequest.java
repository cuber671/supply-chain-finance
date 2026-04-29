package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "撤回注销申请请求")
public class RevokeCancellationRequest {

    @Schema(description = "企业登录密码（用于身份验证）", example = "Enterprise2024@123")
    @NotBlank(message = "密码不能为空")
    private String password;
}