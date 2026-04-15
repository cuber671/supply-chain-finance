package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "更新用户状态请求")
public class UpdateUserStatusRequestDTO {

    @NotNull(message = "状态值不能为空")
    @Schema(description = "新状态", example = "2")
    private Integer status;
}
