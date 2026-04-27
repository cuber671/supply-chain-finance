package com.fisco.app.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户查询响应DTO - 脱敏用户信息
 */
@Schema(description = "用户查询响应")
public class UserQueryResponseDTO {

    @Schema(description = "用户ID", example = "1001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "企业ID", example = "100")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long enterpriseId;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "用户角色", example = "USER", allowableValues = {"USER", "ADMIN", "FINANCE", "WAREHOUSE", "LOGISTICS"})
    private String userRole;

    @Schema(description = "用户状态", example = "2", allowableValues = {"1", "2", "3", "4", "5", "6"})
    private Integer status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
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