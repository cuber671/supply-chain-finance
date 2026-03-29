package com.fisco.app.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * 增强型 JWT 工具类 - 双令牌策略
 * 功能：多级权限隔离、财务职能校验、数据归属校验
 *
 * 双令牌策略说明：
 * - Access Token: 短期令牌，有效期2小时，用于业务请求
 * - Refresh Token: 长期令牌，有效期7天，用于自动续期
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class JwtUtil {

    // ==================== 静态配置持有类 ====================

    /**
     * JWT配置持有类
     * 用于在静态方法中访问Spring配置的密钥
     */
    private static class JwtConfigHolder {
        private static String SECRET;
        private static javax.crypto.SecretKey KEY;
        private static long ACCESS_TOKEN_EXPIRATION;
        private static long REFRESH_TOKEN_EXPIRATION;

        public static void init(String secret, long accessExpiration, long refreshExpiration) {
            SECRET = secret;
            if (SECRET == null || SECRET.length() < 32) {
                throw new IllegalStateException(
                    "JWT密钥配置无效：密钥长度必须至少32字节。请配置环境变量 JWT_SECRET 为安全的随机字符串。");
            }
            // 检测已知的弱密钥，禁止启动
            if ("FiscoBcos_Platform_Secret_Key_2026".equals(SECRET)
                    || "default-secret-key-for-dev-only".equals(SECRET)) {
                throw new IllegalStateException(
                    "JWT密钥使用了已知的不安全默认值！请立即配置环境变量 JWT_SECRET 为安全的随机字符串。");
            }
            KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            ACCESS_TOKEN_EXPIRATION = accessExpiration;
            REFRESH_TOKEN_EXPIRATION = refreshExpiration;
            log.info("JWT工具类初始化完成，AccessToken过期时间: {}ms, RefreshToken过期时间: {}ms",
                    ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        }
    }

    // ==================== 过期时间常量（供外部获取）====================

    /**
     * Access Token 过期时间（毫秒）
     */
    public static long ACCESS_TOKEN_EXPIRATION = 2 * 60 * 60 * 1000L;

    /**
     * Refresh Token 过期时间（毫秒）
     */
    public static long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    // ==================== Claims 常量定义 ====================
    public static final String CLAIM_ENT_ID = "entId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SCOPE = "scope";
    public static final String CLAIM_ENT_ROLE = "entRole";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_JTI = "jti";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final List<String> FINANCE_ROLES = Arrays.asList("ADMIN", "FINANCE");

    private static final Map<String, Integer> ROLE_LEVEL_MAP = new HashMap<>();
    static {
        ROLE_LEVEL_MAP.put("ADMIN", 10);
        ROLE_LEVEL_MAP.put("FINANCE", 5);
        ROLE_LEVEL_MAP.put("USER", 1);
    }

    // ==================== 静态初始化方法 ====================

    /**
     * 静态初始化方法
     */
    public static void init(String secret, long accessExpiration, long refreshExpiration) {
        JwtConfigHolder.init(secret, accessExpiration, refreshExpiration);
        ACCESS_TOKEN_EXPIRATION = accessExpiration;
        REFRESH_TOKEN_EXPIRATION = refreshExpiration;
    }

    // ==================== 双令牌生成方法 ====================

    public static Map<String, String> createTokenPair(Long sub, Long entId, String role, Integer scope) {
        return createTokenPair(sub, entId, role, scope, null);
    }

    public static Map<String, String> createTokenPair(Long sub, Long entId, String role, Integer scope, Integer entRole) {
        Map<String, String> tokenPair = new HashMap<>();
        String jti = UUID.randomUUID().toString();

        // Access Token
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put(CLAIM_ENT_ID, entId);
        accessClaims.put(CLAIM_ROLE, role);
        accessClaims.put(CLAIM_SCOPE, scope);
        accessClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        accessClaims.put(CLAIM_JTI, jti);
        if (entRole != null) {
            accessClaims.put(CLAIM_ENT_ROLE, entRole);
        }

        String accessToken = Jwts.builder()
                .claims(accessClaims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.ACCESS_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();

        // Refresh Token
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put(CLAIM_ENT_ID, entId);
        refreshClaims.put(CLAIM_ROLE, role);
        refreshClaims.put(CLAIM_SCOPE, scope);
        refreshClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        refreshClaims.put(CLAIM_JTI, jti);
        if (entRole != null) {
            refreshClaims.put(CLAIM_ENT_ROLE, entRole);
        }

        String refreshToken = Jwts.builder()
                .claims(refreshClaims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.REFRESH_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();

        tokenPair.put("accessToken", accessToken);
        tokenPair.put("refreshToken", refreshToken);
        tokenPair.put("expiresIn", String.valueOf(JwtConfigHolder.ACCESS_TOKEN_EXPIRATION / 1000));

        log.info("生成双令牌成功，用户ID: {}, JTI: {}", sub, jti);
        return tokenPair;
    }

    public static String createAccessToken(Long sub, Long entId, String role, Integer scope) {
        return createAccessToken(sub, entId, role, scope, null);
    }

    public static String createAccessToken(Long sub, Long entId, String role, Integer scope, Integer entRole) {
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ENT_ID, entId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SCOPE, scope);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        claims.put(CLAIM_JTI, jti);
        if (entRole != null) {
            claims.put(CLAIM_ENT_ROLE, entRole);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.ACCESS_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();
    }

    public static String createRefreshToken(Long sub, Long entId, String role, Integer scope) {
        return createRefreshToken(sub, entId, role, scope, null);
    }

    public static String createRefreshToken(Long sub, Long entId, String role, Integer scope, Integer entRole) {
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ENT_ID, entId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SCOPE, scope);
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_JTI, jti);
        if (entRole != null) {
            claims.put(CLAIM_ENT_ROLE, entRole);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(sub.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JwtConfigHolder.REFRESH_TOKEN_EXPIRATION))
                .signWith(JwtConfigHolder.KEY)
                .compact();
    }

    // ==================== Token解析和验证 ====================

    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(JwtConfigHolder.KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("JWT 验证失败: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public static String getTokenType(Claims claims) {
        return claims.get(CLAIM_TOKEN_TYPE, String.class);
    }

    public static boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(claims));
    }

    public static boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(claims));
    }

    // ==================== 属性提取方法 ====================

    public static Long getSubId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public static Long getEntId(Claims claims) {
        Object entId = claims.get(CLAIM_ENT_ID);
        if (entId == null) return null;
        if (entId instanceof Integer) return ((Integer) entId).longValue();
        return (Long) entId;
    }

    public static String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public static Integer getScope(Claims claims) {
        return claims.get(CLAIM_SCOPE, Integer.class);
    }

    public static Integer getEntRole(Claims claims) {
        return claims.get(CLAIM_ENT_ROLE, Integer.class);
    }

    public static int getPermissionLevel(Claims claims) {
        String role = getRole(claims);
        if (role == null) return 0;
        Integer level = ROLE_LEVEL_MAP.get(role);
        return level != null ? level : 0;
    }

    public static boolean hasPermissionLevel(Claims claims, int requiredLevel) {
        if (Integer.valueOf(1).equals(getScope(claims))) return true;
        int userLevel = getPermissionLevel(claims);
        return userLevel >= requiredLevel;
    }

    public static String getJti(Claims claims) {
        return claims.get(CLAIM_JTI, String.class);
    }

    // ==================== 权限校验方法 ====================

    public static boolean canAccess(Claims claims, Long targetEntId) {
        if (claims == null) return false;
        if (Integer.valueOf(1).equals(getScope(claims))) return true;
        Long entId = getEntId(claims);
        return entId != null && entId.equals(targetEntId);
    }

    public static boolean isFinanceRole(Claims claims) {
        if (claims == null) return false;
        if (Integer.valueOf(1).equals(getScope(claims))) return true;
        String role = getRole(claims);
        return role != null && FINANCE_ROLES.contains(role);
    }

    public static boolean canExecuteFinance(Claims claims, Long targetEntId) {
        return canAccess(claims, targetEntId) && isFinanceRole(claims);
    }

    public static long getExpireIn(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) return -1;
            Date expiration = claims.getExpiration();
            if (expiration == null) return -1;
            return (expiration.getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            log.error("获取令牌过期时间失败: {}", e.getMessage());
            return -1;
        }
    }
}
