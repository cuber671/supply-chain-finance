package com.fisco.app.service;

import java.util.Map;

/**
 * Token服务接口 - 双令牌策略
 * 提供令牌的生成、验证、刷新功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface TokenService {

    /**
     * 生成双令牌（Access Token + Refresh Token）
     *
     * @param userId  用户ID（必填）
     * @param entId   企业ID（可选，用于数据隔离）
     * @param role    角色（可选）
     * @param scope   权限范围（可选，1=系统管理员）
     * @param entRole 企业角色（可选，用于仓单权限校验，6=金融机构，9=仓储方等）
     * @return 包含accessToken和refreshToken的Map
     *         - accessToken: 短期令牌（2小时）
     *         - refreshToken: 长期令牌（7天）
     */
    Map<String, String> generateTokenPair(Long userId, Long entId, String role, Integer scope, Integer entRole);

    /**
     * 刷新令牌
     * 使用Refresh Token获取新的Access Token
     *
     * @param refreshToken Refresh Token字符串
     * @return 新的令牌对Map，刷新失败返回null
     */
    Map<String, String> refreshToken(String refreshToken);

    /**
     * 验证Access Token是否有效
     *
     * @param accessToken Access Token字符串
     * @return true=有效, false=无效
     */
    boolean validateAccessToken(String accessToken);

    /**
     * 解析Access Token获取用户信息
     *
     * @param accessToken Access Token字符串
     * @return 包含用户信息的Map，包含userId、entId、role、scope
     */
    Map<String, Object> parseAccessToken(String accessToken);

    /**
     * 吊销令牌（将JTI加入黑名单）
     *
     * @param token 要吊销的令牌
     * @return true=吊销成功
     */
    boolean revokeToken(String token);

    /**
     * 检查令牌是否在黑名单中
     *
     * @param jti 令牌JTI
     * @return true=在黑名单中
     */
    boolean isTokenBlacklisted(String jti);
}
