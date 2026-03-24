package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 刷新Token请求 DTO
 */
@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequestDTO {
    @Schema(description = "刷新令牌（refreshToken）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    public boolean isValid() {
        return refreshToken != null && !refreshToken.isEmpty();
    }
}