package com.fisco.app.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 刷新令牌请求DTO
 */
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Refresh Token不能为空")
    @JsonProperty("refreshToken")
    private String refreshToken;

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
