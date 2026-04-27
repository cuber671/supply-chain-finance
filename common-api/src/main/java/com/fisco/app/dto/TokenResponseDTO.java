package com.fisco.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Token令牌响应DTO
 */
@Schema(description = "认证令牌响应")
@JsonDeserialize(using = TokenResponseDTO.SnakeToCamelDeserializer.class)
public class TokenResponseDTO {

    public static class SnakeToCamelDeserializer extends JsonDeserializer<TokenResponseDTO> {
        @Override
        public TokenResponseDTO deserialize(JsonParser p, DeserializationContext ctxt) {
            try {
                TokenResponseDTO dto = new TokenResponseDTO();
                JsonNode tree = p.readValueAsTree();

                dto.setAccessToken(getTextValue(tree, "access_token", "accessToken"));
                dto.setRefreshToken(getTextValue(tree, "refresh_token", "refreshToken"));
                dto.setExpiresIn(getLongValue(tree, "expires_in", "expiresIn"));
                dto.setTokenType(getTextValue(tree, "token_type", "tokenType"));
                dto.setScope(getTextValue(tree, "scope"));
                dto.setUserId(getLongValue(tree, "user_id", "userId"));
                dto.setEntId(getLongValue(tree, "ent_id", "entId"));

                return dto;
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize TokenResponseDTO", e);
            }
        }

        private String getTextValue(JsonNode tree, String... fieldNames) {
            for (String name : fieldNames) {
                JsonNode node = tree.get(name);
                if (node != null && !node.isNull()) {
                    return node.asText();
                }
            }
            return null;
        }

        private Long getLongValue(JsonNode tree, String... fieldNames) {
            for (String name : fieldNames) {
                JsonNode node = tree.get(name);
                if (node != null && !node.isNull()) {
                    return node.asLong();
                }
            }
            return null;
        }
    }

    @JsonProperty("accessToken")
    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @JsonProperty("refreshToken")
    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @JsonProperty("expiresIn")
    @Schema(description = "令牌有效期（秒）", example = "7200")
    private Long expiresIn;

    @JsonProperty("tokenType")
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "权限范围", example = "ALL")
    private String scope;

    @JsonProperty("userId")
    @Schema(description = "用户ID", example = "1001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonProperty("entId")
    @Schema(description = "企业ID", example = "100")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long entId;

    public TokenResponseDTO() {
    }

    public TokenResponseDTO(String accessToken, String refreshToken, Long expiresIn,
                           String tokenType, String scope, Long userId, Long entId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
        this.scope = scope;
        this.userId = userId;
        this.entId = entId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEntId() {
        return entId;
    }

    public void setEntId(Long entId) {
        this.entId = entId;
    }
}