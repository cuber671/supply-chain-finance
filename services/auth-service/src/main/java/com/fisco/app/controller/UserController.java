package com.fisco.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fisco.app.config.JwtAuthenticationFilter;
import com.fisco.app.dto.RegisterRequestDTO;
import com.fisco.app.dto.UserQueryResponseDTO;
import com.fisco.app.entity.User;
import com.fisco.app.enums.UserStatusEnum;
import com.fisco.app.mapper.UserMapper;
import com.fisco.app.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户管理控制器
 * 提供用户注册、信息查询、状态变更、角色变更接口
 */
@Tag(name = "用户管理", description = "用户注册、信息查询、状态变更、角色变更接口")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserService userService;
    /**
     * 用户注册接口
     */
    @Operation(summary = "用户注册", description = "新用户注册账号。注册后状态为\"待审核\"，需管理员审核通过后才能正常使用。\n\n" +
            "**业务规则**：\n- 用户名不能重复\n- 手机号不能重复（可选）\n- 支持邀请码（可选）用于关联邀请企业")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "参数错误：用户名已存在或手机号已被注册", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequestDTO request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameCheck = new LambdaQueryWrapper<>();
        usernameCheck.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(usernameCheck) > 0) {
            return buildErrorResponse(400, "用户名已存在");
        }

        // 检查手机号是否已存在
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneCheck = new LambdaQueryWrapper<>();
            phoneCheck.eq(User::getPhone, request.getPhone());
            if (userMapper.selectCount(phoneCheck) > 0) {
                return buildErrorResponse(400, "手机号已被注册");
            }
        }

        // 调用 Service 层注册（含邀请码校验等完整逻辑）
        Long userId;
        try {
            userId = userService.registerUser(request);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "注册成功，请等待审核");
        response.put("data", Map.of("userId", userId));

        return ResponseEntity.ok(response);
    }

    /**
     * 用户信息查询接口
     * 支持用户查看自己和管理员查看所有用户
     */
    @Operation(summary = "查询用户详情", description = "根据用户ID查询用户详细信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = UserQueryResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return buildErrorResponse(404, "用户不存在");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", UserQueryResponseDTO.fromUser(user));

        return ResponseEntity.ok(response);
    }

    /**
     * 用户状态变更接口
     * 管理员可冻结/解冻用户，普通用户可申请注销
     */
    @Operation(summary = "变更用户状态", description = "管理员变更用户状态。\n\n" +
            "**状态机规则**：\n" +
            "- PENDING → NORMAL：审核通过\n" +
            "- NORMAL → FROZEN：冻结用户\n" +
            "- NORMAL → CANCELLING：申请注销\n" +
            "- CANCELLING/PENDING_CANCEL → CANCELLED：完成注销\n\n" +
            "**前置条件**：仅管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "状态更新成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "状态值不合法或状态转换不允许", content = @Content),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PutMapping("/{userId}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {

        User user = userMapper.selectById(userId);
        if (user == null) {
            return buildErrorResponse(404, "用户不存在");
        }

        Integer newStatus = (Integer) request.get("status");
        if (newStatus == null) {
            return buildErrorResponse(400, "状态值不能为空");
        }

        // 状态机校验
        Integer currentStatus = user.getStatus();
        boolean validTransition = false;
        String errorMsg = null;

        switch (newStatus) {
            case User.STATUS_NORMAL:
                // PENDING -> NORMAL（审核通过）
                if (currentStatus == UserStatusEnum.PENDING.getValue()) {
                    validTransition = true;
                } else {
                    errorMsg = "只有待审核状态可以设置为正常";
                }
                break;
            case User.STATUS_FROZEN:
                // NORMAL -> FROZEN（冻结）
                if (currentStatus == UserStatusEnum.NORMAL.getValue()) {
                    validTransition = true;
                } else {
                    errorMsg = "只有正常状态可以冻结";
                }
                break;
            case User.STATUS_CANCELLING:
                // NORMAL -> CANCELLING（申请注销）
                if (currentStatus == UserStatusEnum.NORMAL.getValue()) {
                    validTransition = true;
                } else {
                    errorMsg = "只有正常状态可以申请注销";
                }
                break;
            case User.STATUS_CANCELLED:
                // CANCELLING/PENDING_CANCEL -> CANCELLED（注销）
                if (currentStatus == UserStatusEnum.CANCELLING.getValue()
                        || currentStatus == UserStatusEnum.PENDING_CANCEL.getValue()) {
                    validTransition = true;
                } else {
                    errorMsg = "只有注销中或注销待审核状态可以完成注销";
                }
                break;
            default:
                errorMsg = "不支持的状态值";
        }

        if (!validTransition) {
            return buildErrorResponse(400, errorMsg);
        }

        user.setStatus(newStatus);
        userMapper.updateById(user);
        log.info("用户状态变更成功, userId={}, oldStatus={}, newStatus={}", userId, currentStatus, newStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "状态更新成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 用户角色变更接口
     * 管理员可为用户分配角色
     */
    @Operation(summary = "变更用户角色", description = "管理员为用户分配角色。\n\n" +
            "**角色值**：ADMIN（管理员）、FINANCE（财务）、OPERATOR（操作员）\n\n" +
            "**前置条件**：仅管理员可操作，且只能操作本企业用户。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "角色更新成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "角色值不合法", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权操作其他企业的用户", content = @Content),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PutMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            javax.servlet.http.HttpServletRequest httpRequest) {

        User user = userMapper.selectById(userId);
        if (user == null) {
            return buildErrorResponse(404, "用户不存在");
        }

        // FIX: 添加企业归属校验，防止A企业管理员修改B企业用户角色
        Long requesterEntId = JwtAuthenticationFilter.getEntId(httpRequest);
        if (requesterEntId == null || !user.getEnterpriseId().equals(requesterEntId)) {
            return buildErrorResponse(403, "无权操作其他企业的用户");
        }

        String newRole = (String) request.get("role");
        if (newRole == null || newRole.isEmpty()) {
            return buildErrorResponse(400, "角色不能为空");
        }

        // 校验角色值
        if (!User.ROLE_ADMIN.equals(newRole)
                && !User.ROLE_FINANCE.equals(newRole)
                && !User.ROLE_OPERATOR.equals(newRole)) {
            return buildErrorResponse(400, "无效的角色值，仅支持ADMIN/FINANCE/OPERATOR");
        }

        String oldRole = user.getUserRole();
        user.setUserRole(newRole);
        userMapper.updateById(user);
        log.info("用户角色变更成功, userId={}, oldRole={}, newRole={}", userId, oldRole, newRole);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "角色更新成功");

        return ResponseEntity.ok(response);
    }

    // ==================== Phase 1: 个人资料管理 ====================

    /**
     * 获取当前用户个人资料
     */
    @Operation(summary = "获取当前用户资料", description = "获取当前登录用户的个人资料信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = UserQueryResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(javax.servlet.http.HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getUserId(request);
        if (userId == null) {
            return buildErrorResponse(401, "未登录或Token无效");
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            return buildErrorResponse(404, "用户不存在");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", UserQueryResponseDTO.fromUser(user));

        return ResponseEntity.ok(response);
    }

    /**
     * 修改个人资料
     */
    @Operation(summary = "修改个人资料", description = "修改当前用户的个人资料，包括真实姓名、手机号、邮箱。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserInfo(
            javax.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long userId = JwtAuthenticationFilter.getUserId(request);
        if (userId == null) {
            return buildErrorResponse(401, "未登录或Token无效");
        }

        String realName = body.get("realName");
        String phone = body.get("phone");
        String email = body.get("email");

        try {
            userService.updateUserInfo(userId, realName, phone, email);
            return buildSuccessResponse("更新成功");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码，需要验证原密码。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "密码修改成功", content = @Content),
        @ApiResponse(responseCode = "400", description = "原密码或新密码为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            javax.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long userId = JwtAuthenticationFilter.getUserId(request);
        if (userId == null) {
            return buildErrorResponse(401, "未登录或Token无效");
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return buildErrorResponse(400, "原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return buildErrorResponse(400, "新密码不能为空");
        }

        try {
            userService.updatePassword(userId, oldPassword, newPassword);
            return buildSuccessResponse("密码修改成功");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    // ==================== Phase 2: 员工列表与审核 ====================

    /**
     * 分页查询企业员工列表
     */
    @Operation(summary = "分页查询企业员工列表", description = "分页查询当前企业下的所有员工列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUserPage(
            javax.servlet.http.HttpServletRequest request,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        Long enterpriseId = JwtAuthenticationFilter.getEntId(request);
        if (enterpriseId == null) {
            return buildErrorResponse(401, "无法获取企业信息");
        }

        IPage<User> page = userService.getUserPageByEnterpriseId(enterpriseId, pageNum, pageSize);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", page);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询待审核用户列表
     */
    @Operation(summary = "查询待审核用户列表", description = "查询当前企业下所有待审核（状态为PENDING）的用户注册申请列表。\n\n" +
            "**前置条件**：仅管理员可访问。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可访问", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingUsers(javax.servlet.http.HttpServletRequest request) {
        String role = JwtAuthenticationFilter.getRole(request);
        if (!User.ROLE_ADMIN.equals(role)) {
            return buildErrorResponse(403, "只有管理员可以访问此接口");
        }

        Long enterpriseId = JwtAuthenticationFilter.getEntId(request);
        List<User> users = userService.getPendingUsers(enterpriseId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", users);
        return ResponseEntity.ok(response);
    }

    /**
     * 审核用户注册申请
     */
    @Operation(summary = "审核用户注册申请", description = "管理员审核用户的注册申请。审核通过后用户状态变为\"正常\"，审核拒绝后用户状态不变。\n\n" +
            "**前置条件**：仅管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核完成", content = @Content),
        @ApiResponse(responseCode = "400", description = "审核结果不能为空", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可操作", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{userId}/audit")
    public ResponseEntity<Map<String, Object>> auditUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body,
            javax.servlet.http.HttpServletRequest request) {
        String role = JwtAuthenticationFilter.getRole(request);
        if (!User.ROLE_ADMIN.equals(role)) {
            return buildErrorResponse(403, "只有管理员可以访问此接口");
        }

        Boolean approved = body.get("approved");
        if (approved == null) {
            return buildErrorResponse(400, "审核结果不能为空");
        }

        Long auditorId = JwtAuthenticationFilter.getUserId(request);

        try {
            userService.auditUser(userId, approved, auditorId);
            return buildSuccessResponse(approved ? "审核通过" : "审核已拒绝");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    // ==================== Phase 3: 注销与强制操作 ====================

    /**
     * 申请注销
     */
    @Operation(summary = "申请注销账号", description = "当前用户申请注销自己的账号，需要验证密码。注销申请提交后状态变为\"注销中\"，需管理员审核完成注销。\n\n" +
            "**前置条件**：仅正常状态用户可申请注销。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注销申请已提交", content = @Content),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/cancel/apply")
    public ResponseEntity<Map<String, Object>> applyCancellation(
            javax.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long userId = JwtAuthenticationFilter.getUserId(request);
        if (userId == null) {
            return buildErrorResponse(401, "未登录或Token无效");
        }

        String reason = body.get("reason");
        String password = body.get("password");

        if (password == null || password.isEmpty()) {
            return buildErrorResponse(400, "密码不能为空");
        }

        try {
            userService.applyCancellation(userId, reason, password);
            return buildSuccessResponse("注销申请已提交，等待管理员审核");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    /**
     * 撤回注销申请
     */
    @Operation(summary = "撤回注销申请", description = "用户撤回自己提交的注销申请，状态恢复到\"正常\"。\n\n" +
            "**前置条件**：仅注销中状态的用户可撤回。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注销申请已撤回", content = @Content),
        @ApiResponse(responseCode = "400", description = "状态不允许撤回", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/cancel/revoke")
    public ResponseEntity<Map<String, Object>> revokeCancellation(javax.servlet.http.HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getUserId(request);
        if (userId == null) {
            return buildErrorResponse(401, "未登录或Token无效");
        }

        try {
            userService.revokeCancellation(userId);
            return buildSuccessResponse("注销申请已撤回");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    /**
     * 获取待审核注销用户列表
     */
    @Operation(summary = "查询待审核注销用户列表", description = "查询当前企业下所有申请注销待审核的用户列表。\n\n" +
            "**前置条件**：仅管理员可访问。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可访问", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/cancel/pending")
    public ResponseEntity<Map<String, Object>> getPendingCancellationUsers(javax.servlet.http.HttpServletRequest request) {
        String role = JwtAuthenticationFilter.getRole(request);
        if (!User.ROLE_ADMIN.equals(role)) {
            return buildErrorResponse(403, "只有管理员可以访问此接口");
        }

        Long enterpriseId = JwtAuthenticationFilter.getEntId(request);
        List<User> users = userService.getPendingCancellationUsers(enterpriseId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "查询成功");
        response.put("data", users);
        return ResponseEntity.ok(response);
    }

    /**
     * 审核注销申请
     */
    @Operation(summary = "审核注销申请", description = "管理员审核用户的注销申请。审核通过后用户状态变为\"已注销\"。\n\n" +
            "**前置条件**：仅管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核完成", content = @Content),
        @ApiResponse(responseCode = "400", description = "审核结果不能为空", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可操作", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{userId}/cancel/audit")
    public ResponseEntity<Map<String, Object>> auditCancellation(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @RequestBody Map<String, Boolean> body,
            javax.servlet.http.HttpServletRequest request) {
        String role = JwtAuthenticationFilter.getRole(request);
        if (!User.ROLE_ADMIN.equals(role)) {
            return buildErrorResponse(403, "只有管理员可以访问此接口");
        }

        Boolean approved = body.get("approved");
        if (approved == null) {
            return buildErrorResponse(400, "审核结果不能为空");
        }

        Long auditorId = JwtAuthenticationFilter.getUserId(request);

        try {
            userService.auditCancellation(userId, approved, auditorId);
            return buildSuccessResponse(approved ? "注销审核通过" : "注销审核已拒绝");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    /**
     * 强制禁用用户
     */
    @Operation(summary = "强制禁用用户", description = "管理员强制禁用指定用户，将其状态设置为\"冻结\"。\n\n" +
            "**前置条件**：仅管理员可操作，且只能操作本企业用户。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "用户已禁用", content = @Content),
        @ApiResponse(responseCode = "400", description = "状态不允许", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权操作其他企业的用户", content = @Content),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PutMapping("/disable/{userId}")
    public ResponseEntity<Map<String, Object>> disableUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            javax.servlet.http.HttpServletRequest request) {
        String role = JwtAuthenticationFilter.getRole(request);
        if (!User.ROLE_ADMIN.equals(role)) {
            return buildErrorResponse(403, "只有管理员可以访问此接口");
        }

        User targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return buildErrorResponse(404, "用户不存在");
        }

        Long enterpriseId = JwtAuthenticationFilter.getEntId(request);
        if (!targetUser.getEnterpriseId().equals(enterpriseId)) {
            return buildErrorResponse(403, "无权操作其他企业的用户");
        }

        try {
            userService.disableUser(userId);
            return buildSuccessResponse("用户已禁用");
        } catch (IllegalStateException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "管理员删除指定用户（物理删除）。\n\n" +
            "**前置条件**：仅管理员可操作，且只能删除本企业用户。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "用户已删除", content = @Content),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权操作其他企业的用户", content = @Content),
        @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            javax.servlet.http.HttpServletRequest request) {
        String role = JwtAuthenticationFilter.getRole(request);
        if (!User.ROLE_ADMIN.equals(role)) {
            return buildErrorResponse(403, "只有管理员可以访问此接口");
        }

        User targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return buildErrorResponse(404, "用户不存在");
        }

        Long enterpriseId = JwtAuthenticationFilter.getEntId(request);
        if (!targetUser.getEnterpriseId().equals(enterpriseId)) {
            return buildErrorResponse(403, "无权操作其他企业的用户");
        }

        try {
            userService.deleteUser(userId);
            return buildSuccessResponse("用户已删除");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(400, e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建错误响应
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        return ResponseEntity.status(code >= 400 && code < 500 ? 400 : code).body(error);
    }

    /**
     * 构建成功响应
     */
    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
}
