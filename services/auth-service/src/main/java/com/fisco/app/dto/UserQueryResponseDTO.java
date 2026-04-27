package com.fisco.app.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fisco.app.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息响应")
public class UserQueryResponseDTO {
    @Schema(description = "用户ID", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "企业ID", example = "1")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long enterpriseId;

    @Schema(description = "用户角色", example = "OPERATOR", allowableValues = {"ADMIN", "FINANCE", "OPERATOR"})
    private String userRole;

    @Schema(description = "状态", example = "2", allowableValues = {"1", "2", "3", "4", "5", "6"})
    private Integer status;

    @Schema(description = "创建时间", example = "2026-01-01T10:00:00")
    private LocalDateTime createTime;

    public static UserQueryResponseDTO fromUser(User user) {
        UserQueryResponseDTO dto = new UserQueryResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setEnterpriseId(user.getEnterpriseId());
        dto.setUserRole(user.getUserRole());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        return dto;
    }
}
