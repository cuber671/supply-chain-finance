package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token响应 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "认证令牌响应")
public class TokenResponseDTO {
    @Schema(description = "访问令牌（JWT）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌（用于刷新访问令牌）", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "访问令牌过期时间（秒）", example = "7200")
    private Long expirationSeconds;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "企业ID（管理员为null）", example = "1")
    private Long entId;

    public static TokenResponseDTO of(String accessToken, String refreshToken,
            Long expirationSeconds, Long userId, Long entId) {
        return new TokenResponseDTO(accessToken, refreshToken, expirationSeconds, userId, entId);
    }
}
