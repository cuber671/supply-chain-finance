package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "企业注销申请请求")
public class CancellationApplyRequest {

    @Schema(description = "企业登录密码（用于身份验证）", example = "Enterprise2024@123")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "注销原因", example = "不再使用该账号")
    @Size(max = 500, message = "注销原因不能超过500字符")
    private String reason;
}