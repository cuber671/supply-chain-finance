package com.fisco.app.dto;

import com.fisco.app.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息响应")
public class UserQueryResponseDTO {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "企业ID")
    private Long enterpriseId;

    @Schema(description = "用户角色：ADMIN-管理员, FINANCE-财务, OPERATOR-操作员")
    private String userRole;

    @Schema(description = "状态：0-待审核, 1-正常, 2-冻结, 3-注销中, 4-已注销")
    private Integer status;

    @Schema(description = "创建时间")
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