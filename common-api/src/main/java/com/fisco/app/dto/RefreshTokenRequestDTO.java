package com.fisco.app.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 刷新令牌请求DTO
 */
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Refresh Token不能为空")
    @JsonProperty("refreshToken")
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "授权类型", example = "refresh_token", allowableValues = {"refresh_token"})
    private String grantType;

    public RefreshTokenRequestDTO() {
    }

    public RefreshTokenRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public boolean isValid() {
        return refreshToken != null && !refreshToken.trim().isEmpty();
    }
}