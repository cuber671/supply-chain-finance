package com.fisco.app.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fisco.app.util.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Token服务实现类 - 双令牌策略
 * 使用Redis作为令牌黑名单集中式存储，支持分布式多实例部署
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public Map<String, String> generateTokenPair(Long userId, Long entId, String role, Integer scope, Integer entRole) {
        try {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }

            Map<String, String> tokenPair = JwtUtil.createTokenPair(userId, entId, role, scope, entRole);
            log.info("生成令牌对成功，用户ID: {}, 企业ID: {}, 角色: {}, 企业角色: {}", userId, entId, role, entRole);
            return tokenPair;
        } catch (Exception e) {
            log.error("生成令牌对失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate token pair: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        try {
            // 1. 解析Refresh Token
            Claims claims = JwtUtil.parseToken(refreshToken);
            if (claims == null) {
                log.warn("Refresh Token解析失败");
                return null;
            }

            // 2. 验证Token类型
            if (!JwtUtil.isRefreshToken(claims)) {
                log.warn("Token类型错误，不是Refresh Token");
                return null;
            }

            // 3. 检查是否在黑名单中
            String jti = JwtUtil.getJti(claims);
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Refresh Token已被吊销，JTI: {}", jti);
                return null;
            }

            // 4. 验证Token是否过期
            if (!JwtUtil.validateToken(refreshToken)) {
                log.warn("Refresh Token已过期");
                return null;
            }

            // 5. 提取用户信息生成新令牌
            Long userId = JwtUtil.getSubId(claims);
            Long entId = JwtUtil.getEntId(claims);
            String role = JwtUtil.getRole(claims);
            Integer scope = JwtUtil.getScope(claims);
            Integer entRole = JwtUtil.getEntRole(claims);

            // 生成新的令牌对
            Map<String, String> newTokenPair = JwtUtil.createTokenPair(userId, entId, role, scope, entRole);
            log.info("刷新令牌成功，用户ID: {}", userId);
            return newTokenPair;
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            // 1. 解析Token
            Claims claims = JwtUtil.parseToken(accessToken);
            if (claims == null) {
                log.warn("Access Token解析失败");
                return false;
            }

            // 2. 验证Token类型
            if (!JwtUtil.isAccessToken(claims)) {
                log.warn("Token类型错误，不是Access Token");
                return false;
            }

            // 3. 检查是否在黑名单中
            String jti = JwtUtil.getJti(claims);
            if (jti != null && isTokenBlacklisted(jti)) {
                log.warn("Access Token已被吊销，JTI: {}", jti);
                return false;
            }

            // 4. 验证Token是否过期
            return JwtUtil.validateToken(accessToken);
        } catch (Exception e) {
            log.error("验证Access Token失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> parseAccessToken(String accessToken) {
        Map<String, Object> userInfo = new HashMap<>();
        try {
            Claims claims = JwtUtil.parseToken(accessToken);
            if (claims == null) {
                log.warn("Access Token解析失败");
                return null;
            }

            if (!JwtUtil.isAccessToken(claims)) {
                log.warn("Token类型错误");
                return null;
            }

            userInfo.put("userId", JwtUtil.getSubId(claims));
            userInfo.put("entId", JwtUtil.getEntId(claims));
            userInfo.put("role", JwtUtil.getRole(claims));
            userInfo.put("scope", JwtUtil.getScope(claims));
            userInfo.put("jti", JwtUtil.getJti(claims));

            long expireIn = JwtUtil.getExpireIn(accessToken);
            userInfo.put("expireIn", expireIn);

            return userInfo;
        } catch (Exception e) {
            log.error("解析Access Token失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("null")
    @Override
    public boolean revokeToken(String token) {
        try {
            Claims claims = JwtUtil.parseToken(token);
            if (claims == null) {
                log.warn("Token解析失败，无法吊销");
                return false;
            }

            String jti = JwtUtil.getJti(claims);
            if (jti == null) {
                log.warn("Token无JTI标识，无法吊销");
                return false;
            }

            long expirationTime = claims.getExpiration().getTime();
            long remainingTtl = Math.max(0, expirationTime - System.currentTimeMillis());
            long ttlSeconds = remainingTtl / 1000;

            log.debug("添加JTI到Redis黑名单: {}, 过期时间: {}, TTL: {}秒", jti, expirationTime, ttlSeconds);
            long ttl = ttlSeconds > 0 ? ttlSeconds : 1;
            String ttlKey = BLACKLIST_PREFIX + jti;
            String expStr = String.valueOf(expirationTime);
            // String.valueOf(long) never returns null, but @NonNull on Redis template method requires proof
            //noinspection ConstantConditions
            redisTemplate.opsForValue().set(ttlKey, expStr, ttl, TimeUnit.SECONDS);
            log.info("令牌吊销成功，JTI: {}", jti);
            return true;
        } catch (Exception e) {
            log.error("吊销令牌失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            String key = BLACKLIST_PREFIX + jti;
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("JTI不在黑名单中: {}", jti);
                return false;
            }
            long expirationTime = Long.parseLong(value);
            if (System.currentTimeMillis() > expirationTime) {
                redisTemplate.delete(key);
                log.debug("JTI已过期，从黑名单移除: {}", jti);
                return false;
            }
            log.debug("JTI在黑名单中: {}", jti);
            return true;
        } catch (Exception e) {
            log.error("查询黑名单异常: {}", e.getMessage());
            return false;
        }
    }
}
