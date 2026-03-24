package com.fisco.app.dto;

import java.time.LocalDateTime;

/**
 * 用户查询响应DTO - 脱敏用户信息
 */
public class UserQueryResponseDTO {

    private Long userId;
    private Long enterpriseId;
    private String realName;
    private String phone;
    private String email;
    private String username;
    private String userRole;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;

    public UserQueryResponseDTO() {
    }

    public static UserQueryResponseDTO fromUser(Object userObj) {
        UserQueryResponseDTO dto = new UserQueryResponseDTO();
        if (userObj == null) {
            return dto;
        }
        // 反射获取字段值，密码字段不映射
        try {
            Class<?> clazz = userObj.getClass();
            dto.userId = getLongField(userObj, clazz, "userId");
            dto.enterpriseId = getLongField(userObj, clazz, "enterpriseId");
            dto.realName = getStringField(userObj, clazz, "realName");
            dto.phone = getStringField(userObj, clazz, "phone");
            dto.email = getStringField(userObj, clazz, "email");
            dto.username = getStringField(userObj, clazz, "username");
            dto.userRole = getStringField(userObj, clazz, "userRole");
            dto.status = getIntField(userObj, clazz, "status");
            dto.lastLoginTime = getLocalDateTimeField(userObj, clazz, "lastLoginTime");
            dto.createTime = getLocalDateTimeField(userObj, clazz, "createTime");
        } catch (Exception e) {
            // 忽略反射异常
        }
        return dto;
    }

    private static Long getLongField(Object obj, Class<?> clazz, String fieldName) {
        try {
            Object value = clazz.getDeclaredField(fieldName).get(obj);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer getIntField(Object obj, Class<?> clazz, String fieldName) {
        try {
            Object value = clazz.getDeclaredField(fieldName).get(obj);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getStringField(Object obj, Class<?> clazz, String fieldName) {
        try {
            Object value = clazz.getDeclaredField(fieldName).get(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime getLocalDateTimeField(Object obj, Class<?> clazz, String fieldName) {
        try {
            return (LocalDateTime) clazz.getDeclaredField(fieldName).get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Long enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
