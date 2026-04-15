package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "修改密码请求")
public class UpdatePasswordRequestDTO {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码", example = "********")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", example = "********")
    private String newPassword;
}
