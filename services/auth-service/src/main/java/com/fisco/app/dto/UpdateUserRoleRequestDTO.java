package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "更新用户角色请求")
public class UpdateUserRoleRequestDTO {

    @NotBlank(message = "角色不能为空")
    @Schema(description = "新角色", example = "FINANCE", allowableValues = {"ADMIN", "FINANCE", "OPERATOR"})
    private String role;
}
