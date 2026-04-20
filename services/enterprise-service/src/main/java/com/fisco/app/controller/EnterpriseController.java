package com.fisco.app.controller;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.fisco.app.constant.EntRoleConstant;
import com.fisco.app.dto.FinancialInstitutionCheckDTO;
import com.fisco.app.entity.Enterprise;
import com.fisco.app.entity.InvitationCode;
import com.fisco.app.service.EnterpriseService;
import com.fisco.app.service.EnterpriseService.AssetBalance;
import com.fisco.app.service.EnterpriseService.CancellationResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fisco.app.dto.TokenResponseDTO;
import com.fisco.app.feign.AuthFeignClient;
import com.fisco.app.util.CurrentUser;
import com.fisco.app.util.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 企业管理 Controller
 *
 * 提供企业注册、信息查询、状态管理等 API
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "企业管理", description = "企业注册、登录、信息管理、状态变更、邀请码管理、注销流程、区块链上链接口")
@RestController
@RequestMapping("/api/v1/enterprise")
public class EnterpriseController {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseController.class);

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired(required = false)
    private AuthFeignClient authFeignClient;

    // ==================== 企业注册 ====================

    /**
     * 注册企业
     */
    @Operation(summary = "注册企业", description = "新企业注册账号，包含登录密码和交易密码。注册后状态为\"待审核\"，需平台管理员审核通过后才能正常使用。审核通过后自动将企业信息写入区块链。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：用户名、密码、企业名称、统一社会信用代码等必填字段为空"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/register")
    public ResponseEntity<Result<Map<String, Object>>> registerEnterprise(
            @Parameter(description = "企业注册信息", required = true) @Valid @RequestBody EnterpriseRegisterRequest request) {
        try {
            // 参数校验
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "用户名不能为空"));
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "登录密码不能为空"));
            }
            if (request.getPayPassword() == null || request.getPayPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "交易密码不能为空"));
            }
            if (request.getEnterpriseName() == null || request.getEnterpriseName().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "企业名称不能为空"));
            }
            if (request.getOrgCode() == null || request.getOrgCode().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "统一社会信用代码不能为空"));
            }
            if (request.getEntRole() == null) {
                return ResponseEntity.ok(Result.error(400, "企业角色不能为空"));
            }

            // 调用Service完成注册
            Long entId = enterpriseService.registerEnterprise(
                    request.getUsername(),
                    request.getPassword(),
                    request.getPayPassword(),
                    request.getEnterpriseName(),
                    request.getOrgCode(),
                    request.getEntRole(),
                    request.getLocalAddress(),
                    request.getContactPhone()
            );

            // 查询注册后的企业信息
            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("username", enterprise.getUsername());
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("orgCode", enterprise.getOrgCode());
            result.put("entRole", enterprise.getEntRole());
            result.put("status", enterprise.getStatus());
            result.put("blockchainAddress", enterprise.getBlockchainAddress());

            logger.info("企业注册成功: entId={}, username={}, blockchainAddress={}",
                    entId, request.getUsername(), enterprise.getBlockchainAddress());
            return ResponseEntity.ok(Result.success(result));

        } catch (IllegalArgumentException e) {
            logger.warn("企业注册参数错误: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("注册企业异常", e);
            return ResponseEntity.ok(Result.error(500, "注册失败，请稍后重试"));
        }
    }

    // ==================== 企业查询 ====================

    /**
     * 根据ID获取企业信息
     */
    @Operation(summary = "根据ID获取企业信息", description = "根据企业ID查询企业详细信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "企业ID为空"),
        @ApiResponse(responseCode = "404", description = "企业不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/{entId}")
    public ResponseEntity<Result<Enterprise>> getEnterpriseById(
            @Parameter(description = "企业ID", required = true)
            @PathVariable Long entId) {
        try {
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }

            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);
            if (enterprise == null) {
                return ResponseEntity.ok(Result.error(404, "企业不存在"));
            }

            return ResponseEntity.ok(Result.success(enterprise));

        } catch (Exception e) {
            logger.error("获取企业信息异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "查询失败，请稍后重试"));
        }
    }

    /**
     * 获取企业列表（分页）
     */
    @Operation(summary = "获取企业列表（分页）", description = "分页查询企业列表，支持按状态和角色筛选。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/list")
    public ResponseEntity<Result<Map<String, Object>>> getEnterpriseList(
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "状态过滤")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "角色过滤")
            @RequestParam(required = false) Integer entRole) {
        try {
            if (pageNum < 1) pageNum = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 10;

            IPage<Enterprise> page = enterpriseService.listEnterprisesPaginated(pageNum, pageSize, status, entRole);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("pageNum", page.getCurrent());
            data.put("pageSize", page.getSize());
            data.put("pages", page.getPages());
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            logger.error("获取企业列表异常", e);
            return ResponseEntity.ok(Result.error(500, "查询失败，请稍后重试"));
        }
    }

    /**
     * 获取待审核企业列表
     */
    @Operation(summary = "获取待审核企业列表", description = "查询所有状态为\"待审核(0)\"的企业列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/pending")
    public ResponseEntity<Result<List<Enterprise>>> getPendingEnterprises() {
        try {
            List<Enterprise> list = enterpriseService.listEnterprises(0, null);
            return ResponseEntity.ok(Result.success(list));
        } catch (Exception e) {
            logger.error("获取待审核企业列表异常", e);
            return ResponseEntity.ok(Result.error(500, "查询失败，请稍后重试"));
        }
    }

    // ==================== 企业状态管理 ====================

    /**
     * 更新企业状态
     */
    @Operation(summary = "更新企业状态", description = "系统管理员修改企业状态。\n\n" +
            "**前置条件**：仅系统管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限：仅系统管理员可操作"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PutMapping("/{entId}/status")
    public ResponseEntity<Result<Map<String, Object>>> updateEnterpriseStatus(
            @Parameter(description = "企业ID", required = true)
            @PathVariable Long entId,
            @Parameter(description = "状态更新信息", required = true)
            @RequestBody StatusRequest request) {
        try {
            // FIX: 添加权限校验，只有系统管理员才能修改企业状态
            if (!CurrentUser.isAdmin()) {
                return ResponseEntity.ok(Result.error(403, "只有系统管理员才能修改企业状态"));
            }

            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }
            if (request.getStatus() == null) {
                return ResponseEntity.ok(Result.error(400, "状态不能为空"));
            }

            boolean success = enterpriseService.updateEnterpriseStatus(entId, request.getStatus().intValue());

            Map<String, Object> result = new HashMap<>();
            result.put("dbStatus", success ? "success" : "failed");
            result.put("status", request.getStatus());

            if (success) {
                return ResponseEntity.ok(Result.success(result));
            } else {
                return ResponseEntity.ok(Result.error(500, "更新企业状态失败"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("更新企业状态异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 审核企业申请
     */
    @Operation(summary = "审核企业申请", description = "平台管理员审核企业注册申请。\n\n" +
            "- 审核通过：企业状态变为\"正常(1)\"，并将企业信息注册上链\n" +
            "- 审核拒绝：企业状态变为\"已冻结(2)\"\n\n" +
            "**前置条件**：仅系统管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核完成"),
        @ApiResponse(responseCode = "400", description = "参数错误或企业非待审核状态"),
        @ApiResponse(responseCode = "404", description = "企业不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/{entId}/audit")
    public ResponseEntity<Result<Map<String, Object>>> auditEnterprise(
            @Parameter(description = "企业ID", required = true)
            @PathVariable Long entId,
            @Parameter(description = "审核信息", required = true)
            @RequestBody AuditRequest request) {
        try {
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }
            if (request.getApproved() == null) {
                return ResponseEntity.ok(Result.error(400, "审核结果不能为空"));
            }

            // 获取企业当前状态
            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);
            if (enterprise == null) {
                return ResponseEntity.ok(Result.error(404, "企业不存在"));
            }
            if (enterprise.getStatus() != 0) {
                return ResponseEntity.ok(Result.error(400, "该企业不是待审核状态，无法重复审核"));
            }

            // 审核结果：通过设为正常(1)，拒绝设为冻结(2)
            int newStatus = request.getApproved() ? 1 : 2;
            String action = request.getApproved() ? "通过" : "拒绝";

            Map<String, Object> result = new HashMap<>();
            result.put("enterpriseId", entId);
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("action", action);
            result.put("newStatus", newStatus);

            // FIX: 审核通过时执行区块链注册和状态更新
            if (request.getApproved()) {
                // ① 注册企业上链
                String registerTxHash = null;
                try {
                    registerTxHash = enterpriseService.registerEnterpriseOnChain(entId);
                    result.put("registerTxHash", registerTxHash);
                    logger.info("企业审核通过已上链, entId={}, txHash={}", entId, registerTxHash);
                } catch (Exception e) {
                    logger.error("企业注册上链失败, entId={}, error={}", entId, e.getMessage());
                    // 链上注册失败不应阻塞审核，但需记录
                    result.put("registerTxHash", null);
                    result.put("registerError", e.getMessage());
                }

                // ② 更新链上企业状态
                String statusTxHash = null;
                try {
                    statusTxHash = enterpriseService.updateEnterpriseStatusOnChain(entId, newStatus);
                    result.put("statusTxHash", statusTxHash);
                    logger.info("企业链上状态已更新, entId={}, status={}, txHash={}", entId, newStatus, statusTxHash);
                } catch (Exception e) {
                    logger.error("更新链上企业状态失败, entId={}, error={}", entId, e.getMessage());
                    result.put("statusTxHash", null);
                    result.put("statusError", e.getMessage());
                }
            }

            // ③ 更新数据库状态
            boolean success = enterpriseService.updateEnterpriseStatus(entId, newStatus);
            result.put("dbStatus", success ? "success" : "failed");

            if (success) {
                return ResponseEntity.ok(Result.success(result));
            } else {
                return ResponseEntity.ok(Result.error(500, "审核企业失败"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("审核企业异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    // ==================== 企业登录 ====================

    /**
     * 企业登录
     */
    @Operation(summary = "企业登录", description = "企业用户登录，验证用户名和密码，返回企业信息。\n\n" +
            "**注意**：此接口不需要 JWT Token 认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "用户名或密码为空"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "403", description = "企业状态不允许登录"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, Object>>> login(
            @Parameter(description = "登录信息", required = true) @Valid @RequestBody LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "用户名不能为空"));
            }
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "密码不能为空"));
            }

            Enterprise enterprise = enterpriseService.login(request.getUsername(), request.getPassword());
            if (enterprise == null) {
                return ResponseEntity.ok(Result.error(401, "用户名或密码错误"));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("entId", enterprise.getEntId());
            result.put("username", enterprise.getUsername());
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("entRole", enterprise.getEntRole());
            result.put("status", enterprise.getStatus());
            result.put("blockchainAddress", enterprise.getBlockchainAddress());

            // 调用 auth-service 生成 JWT Token
            if (authFeignClient != null) {
                try {
                    Map<String, Object> tokenRequest = new HashMap<>();
                    tokenRequest.put("userId", enterprise.getEntId());
                    tokenRequest.put("entId", enterprise.getEntId());
                    tokenRequest.put("role", "ENTERPRISE");
                    tokenRequest.put("scope", 5);
                    tokenRequest.put("entRole", enterprise.getEntRole());

                    var tokenResult = authFeignClient.generateEnterpriseToken(tokenRequest);
                    if (tokenResult != null && tokenResult.getCode() == 200 && tokenResult.getData() != null) {
                        TokenResponseDTO tokenData = tokenResult.getData();
                        result.put("accessToken", tokenData.getAccessToken());
                        result.put("refreshToken", tokenData.getRefreshToken());
                        result.put("expiresIn", tokenData.getExpiresIn());
                    }
                } catch (Exception e) {
                    logger.warn("调用 auth-service 生成 Token 失败: {}", e.getMessage());
                    // 不影响登录成功返回，仅不包含 token
                }
            }

            logger.info("企业登录成功: entId={}, username={}", enterprise.getEntId(), enterprise.getUsername());
            return ResponseEntity.ok(Result.success(result));

        } catch (IllegalStateException e) {
            logger.warn("企业登录失败: {}", e.getMessage());
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("企业登录异常", e);
            return ResponseEntity.ok(Result.error(500, "登录失败，请稍后重试"));
        }
    }

    // ==================== 密码管理 ====================

    /**
     * 修改登录密码
     */
    @Operation(summary = "修改登录密码", description = "企业用户修改登录密码，需要验证原密码。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PutMapping("/password/login")
    public ResponseEntity<Result<Void>> updateLoginPassword(
            @Parameter(description = "密码更新信息", required = true) @Valid @RequestBody PasswordUpdateRequest request) {
        try {
            if (request.getEntId() == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }
            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "原密码不能为空"));
            }
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "新密码不能为空"));
            }

            boolean success = enterpriseService.updateLoginPassword(
                    request.getEntId(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                logger.info("企业登录密码已更新: entId={}", request.getEntId());
                return ResponseEntity.ok(Result.success(null));
            } else {
                return ResponseEntity.ok(Result.error(500, "修改密码失败"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("修改登录密码异常: entId={}", request.getEntId(), e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 重置交易密码
     */
    @Operation(summary = "重置交易密码", description = "企业用户重置交易密码，需要验证原交易密码。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PutMapping("/password/pay")
    public ResponseEntity<Result<Void>> updatePayPassword(
            @Parameter(description = "交易密码重置信息", required = true) @Valid @RequestBody PasswordUpdateRequest request) {
        try {
            if (request.getEntId() == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }
            if (request.getOldPassword() == null || request.getOldPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "原交易密码不能为空"));
            }
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "新交易密码不能为空"));
            }

            boolean success = enterpriseService.updatePayPassword(
                    request.getEntId(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );

            if (success) {
                logger.info("企业交易密码已重置: entId={}", request.getEntId());
                return ResponseEntity.ok(Result.success(null));
            } else {
                return ResponseEntity.ok(Result.error(500, "重置交易密码失败"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("重置交易密码异常: entId={}", request.getEntId(), e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    // ==================== 企业详情 ====================

    /**
     * 获取企业详情
     */
    @Operation(summary = "获取企业详情", description = "获取企业详细信息，包含企业名称、信用代码、联系方式、区块链地址等。\n\n" +
            "如未指定entId，则从当前登录用户Token中获取企业ID。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "企业ID为空且未登录"),
        @ApiResponse(responseCode = "404", description = "企业不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/detail")
    public ResponseEntity<Result<EnterpriseDetailResponse>> getEnterpriseDetail(
            @Parameter(description = "企业ID（不填则从Token自动获取）")
            @RequestParam(required = false) Long entId,
            HttpServletRequest request) {
        try {
            // 如果未指定entId，从当前登录用户获取企业ID
            if (entId == null) {
                entId = CurrentUser.getEntId();
            }
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空，请先登录企业账号或指定entId"));
            }

            Enterprise enterprise = enterpriseService.getEnterpriseById(entId);
            if (enterprise == null) {
                return ResponseEntity.ok(Result.error(404, "企业不存在"));
            }

            return ResponseEntity.ok(Result.success(convertToDetailResponse(enterprise)));

        } catch (Exception e) {
            logger.error("获取企业详情异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "查询失败，请稍后重试"));
        }
    }

    // ==================== 邀请码管理 ====================

    /**
     * 生成邀请码
     */
    @Operation(summary = "生成邀请码", description = "企业生成邀请码，用于邀请新用户注册。新用户注册时使用邀请码可关联邀请企业。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "生成成功"),
        @ApiResponse(responseCode = "400", description = "企业ID为空"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/invite-codes")
    public ResponseEntity<Result<InvitationCodeResponse>> generateInvitationCode(
            @Parameter(description = "企业ID，不传则从JWT自动获取")
            @RequestParam(required = false) Long entId,
            @Parameter(description = "最大使用次数", example = "10")
            @RequestParam(required = false) Integer maxUses,
            @Parameter(description = "过期天数", example = "30")
            @RequestParam(required = false) Integer expireDays,
            @Parameter(description = "备注")
            @RequestParam(required = false) String remark,
            HttpServletRequest request) {
        try {
            // JWT 自动获取 entId
            if (entId == null) {
                entId = CurrentUser.getEntId();
            }
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空，请先登录企业账号或指定entId"));
            }

            String code = enterpriseService.generateInvitationCode(entId, maxUses, expireDays, remark);

            InvitationCodeResponse response = new InvitationCodeResponse();
            response.setCode(code);
            response.setMaxUses(maxUses != null ? maxUses : 1);
            response.setExpireDays(expireDays);
            response.setRemark(remark);

            logger.info("生成邀请码成功: entId={}, code={}", entId, code);
            return ResponseEntity.ok(Result.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("生成邀请码异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 查询邀请码列表
     */
    @Operation(summary = "查询邀请码列表", description = "查询当前企业生成的所有邀请码列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "企业ID为空且未登录"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/invite-codes/list")
    public ResponseEntity<Result<List<InvitationCode>>> listInvitationCodes(
            @Parameter(description = "企业ID，不传则从JWT自动获取")
            @RequestParam(required = false) Long entId,
            HttpServletRequest request) {
        try {
            // JWT 自动获取 entId
            if (entId == null) {
                entId = CurrentUser.getEntId();
            }
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空，请先登录企业账号或指定entId"));
            }

            List<InvitationCode> list = enterpriseService.listInvitationCodes(entId);
            return ResponseEntity.ok(Result.success(list));

        } catch (Exception e) {
            logger.error("查询邀请码列表异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "查询失败，请稍后重试"));
        }
    }

    /**
     * 删除邀请码
     */
    @Operation(summary = "删除邀请码", description = "删除指定的邀请码。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "邀请码ID为空"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @DeleteMapping("/invite-codes/{codeId}")
    public ResponseEntity<Result<Void>> deleteInvitationCode(
            @Parameter(description = "邀请码ID", required = true)
            @PathVariable Long codeId) {
        try {
            if (codeId == null) {
                return ResponseEntity.ok(Result.error(400, "邀请码ID不能为空"));
            }

            boolean success = enterpriseService.deleteInvitationCode(codeId);
            if (success) {
                logger.info("删除邀请码成功: codeId={}", codeId);
                return ResponseEntity.ok(Result.success(null));
            } else {
                return ResponseEntity.ok(Result.error(500, "删除邀请码失败"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("删除邀请码异常: codeId={}", codeId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    // ==================== 企业注销管理 ====================

    /**
     * 发起注销申请
     */
    @Operation(summary = "发起注销申请", description = "企业主动发起注销申请，需要平台管理员审核后才能完成注销。\n\n" +
            "**前置条件**：仅正常状态的企业可申请注销。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注销申请已提交"),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/cancellation/apply")
    public ResponseEntity<Result<CancellationResult>> applyCancellation(
            @Parameter(description = "企业ID", required = true)
            @RequestParam Long entId,
            @Parameter(description = "注销原因")
            @RequestParam(required = false) String reason) {
        try {
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }

            CancellationResult result = enterpriseService.applyCancellation(entId, reason);

            if (result.isSuccess()) {
                logger.info("企业注销申请成功: entId={}", entId);
                return ResponseEntity.ok(Result.success(result));
            } else {
                return ResponseEntity.ok(Result.error(400, result.getMessage()));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("发起注销申请异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 撤回注销申请
     */
    @Operation(summary = "撤回注销申请", description = "企业撤回已提交的注销申请，状态恢复到\"正常\"。\n\n" +
            "**前置条件**：仅注销中的企业可撤回。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤回成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许撤回"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/cancellation/revoke")
    public ResponseEntity<Result<Void>> revokeCancellation(
            @Parameter(description = "企业ID", required = true)
            @RequestParam Long entId) {
        try {
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }

            boolean success = enterpriseService.revokeCancellation(entId);
            if (success) {
                logger.info("企业注销申请已撤回: entId={}", entId);
                return ResponseEntity.ok(Result.success(null));
            } else {
                return ResponseEntity.ok(Result.error(500, "撤回注销申请失败"));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("撤回注销申请异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 获取待审核注销企业列表
     */
    @Operation(summary = "获取待审核注销企业列表", description = "查询所有申请注销待审核的企业列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/cancellation/pending")
    public ResponseEntity<Result<List<Enterprise>>> getPendingCancellationEnterprises() {
        try {
            List<Enterprise> list = enterpriseService.getPendingCancellationEnterprises();
            return ResponseEntity.ok(Result.success(list));
        } catch (Exception e) {
            logger.error("获取待审核注销企业列表异常", e);
            return ResponseEntity.ok(Result.error(500, "查询失败，请稍后重试"));
        }
    }

    /**
     * 审核企业注销申请
     */
    @Operation(summary = "审核企业注销申请", description = "平台管理员审核企业的注销申请。审核通过后企业状态变为\"已注销\"。\n\n" +
            "**前置条件**：仅系统管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/{entId}/cancellation/audit")
    public ResponseEntity<Result<Void>> auditCancellation(
            @Parameter(description = "企业ID", required = true) @PathVariable Long entId,
            @Parameter(description = "审核结果", required = true) @RequestBody AuditRequest request) {
        try {
            if (entId == null) {
                return ResponseEntity.ok(Result.error(400, "企业ID不能为空"));
            }
            if (request.getApproved() == null) {
                return ResponseEntity.ok(Result.error(400, "审核结果不能为空"));
            }

            boolean success = enterpriseService.auditCancellation(entId, request.getApproved());

            if (success) {
                logger.info("企业注销审核完成: entId={}, approved={}", entId, request.getApproved());
                return ResponseEntity.ok(Result.success(null));
            } else {
                return ResponseEntity.ok(Result.error(500, "审核企业注销失败"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("审核企业注销异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 管理员强制注销企业（用于资产数据异常等特殊场景）
     */
    @Operation(summary = "管理员强制注销企业", description = "平台管理员强制注销企业，用于资产数据异常等特殊场景。\n\n" +
            "**前置条件**：仅系统管理员可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "强制注销成功"),
        @ApiResponse(responseCode = "400", description = "强制注销原因不能为空"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/cancellation/force")
    public ResponseEntity<Result<Void>> forceCancellation(
            @Parameter(description = "企业ID", required = true)
            @RequestParam Long entId,
            @Parameter(description = "强制注销原因", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            if (reason == null || reason.isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "强制注销原因不能为空"));
            }

            boolean success = enterpriseService.forceCancellation(entId, reason);
            if (success) {
                logger.warn("管理员强制注销企业: entId={}, reason={}", entId, reason);
                return ResponseEntity.ok(Result.success(null));
            } else {
                return ResponseEntity.ok(Result.error(500, "强制注销失败"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("强制注销企业异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    /**
     * 查询企业资产余额
     */
    @Operation(summary = "查询企业资产余额", description = "查询当前企业在平台上的资产余额信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/asset-balance")
    public ResponseEntity<Result<AssetBalance>> checkAssetBalance(HttpServletRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return ResponseEntity.ok(Result.error(401, "未登录或Token无效"));
            }

            AssetBalance balance = enterpriseService.checkAssetBalance(entId);
            return ResponseEntity.ok(Result.success(balance));

        } catch (Exception e) {
            logger.error("查询资产余额异常", e);
            return ResponseEntity.ok(Result.error(500, "查询资产余额失败"));
        }
    }

    // ==================== 邀请码校验（供auth-service调用）====================

    /**
     * 校验邀请码有效性（供auth-service在用户注册时调用）
     * 返回邀请码详细信息用于绑定企业关系
     */
    @Operation(summary = "校验邀请码有效性", description = "校验邀请码是否有效，返回邀请码详细信息。\n\n" +
            "**注意**：此接口供 auth-service 调用，用于用户注册时验证邀请码。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "校验成功，返回邀请码信息"),
        @ApiResponse(responseCode = "400", description = "邀请码为空、已过期或已用完"),
        @ApiResponse(responseCode = "404", description = "邀请码不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/invitation/validate")
    public ResponseEntity<Result<Map<String, Object>>> validateInvitationCode(
            @Parameter(description = "邀请码", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            if (code == null || code.isEmpty()) {
                code = request.get("invitationCode");  // 兼容 invitationCode 字段名
            }
            if (code == null || code.isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "邀请码不能为空"));
            }

            InvitationCode invitationCode = enterpriseService.getInvitationCodeByCode(code);
            if (invitationCode == null) {
                return ResponseEntity.ok(Result.error(404, "邀请码不存在"));
            }
            if (!invitationCode.isValid()) {
                if (invitationCode.isExpired()) {
                    return ResponseEntity.ok(Result.error(400, "邀请码已过期"));
                }
                if (invitationCode.isExhausted()) {
                    return ResponseEntity.ok(Result.error(400, "邀请码已使用完毕"));
                }
                return ResponseEntity.ok(Result.error(400, "邀请码无效"));
            }

            // 获取邀请企业信息
            Enterprise inviter = enterpriseService.getEnterpriseById(invitationCode.getInviterEntId());
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("inviterEntId", invitationCode.getInviterEntId());
            data.put("inviterEnterpriseName", inviter != null ? inviter.getEnterpriseName() : "");
            data.put("maxUses", invitationCode.getMaxUses());
            data.put("usedCount", invitationCode.getUsedCount());
            data.put("expireTime", invitationCode.getExpireTime() != null ? invitationCode.getExpireTime().toString() : "");

            return ResponseEntity.ok(Result.success(data));

        } catch (Exception e) {
            logger.error("校验邀请码异常: code={}", request.get("code"), e);
            return ResponseEntity.ok(Result.error(500, "校验失败，请稍后重试"));
        }
    }

    /**
     * 使用邀请码（供auth-service在用户注册成功后调用，原子递增usedCount）
     */
    @Operation(summary = "使用邀请码", description = "用户注册成功后调用，原子递增邀请码使用次数。")
    @PostMapping("/invite-codes/use")
    public ResponseEntity<Result<Long>> useInvitationCode(
            @Parameter(description = "邀请码", required = true)
            @RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            if (code == null || code.isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "邀请码不能为空"));
            }
            Long inviterEntId = enterpriseService.useInvitationCode(code);
            logger.info("邀请码使用成功: code={}, inviterEntId={}", code, inviterEntId);
            return ResponseEntity.ok(Result.success(inviterEntId));
        } catch (IllegalArgumentException e) {
            logger.warn("使用邀请码失败: code={}, error={}", request.get("code"), e.getMessage());
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("使用邀请码异常", e);
            return ResponseEntity.ok(Result.error(500, "操作失败，请稍后重试"));
        }
    }

    // ==================== 区块链查询接口 ====================

    /**
     * 通过区块链地址查询企业信息（查询本地DB，非链上）
     * 注意：此接口查询本地数据库中已上链的企业记录，不是直接从区块链查询
     */
    @Operation(summary = "通过区块链地址查询企业（本地DB）", description = "根据区块链地址查询本地数据库中已注册的企业信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "该地址的企业不存在于本地数据库"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/blockchain-address/{address}")
    public ResponseEntity<Result<Enterprise>> getEnterpriseByBlockchainAddressLocal(
            @Parameter(description = "区块链地址", required = true) @PathVariable String address) {
        try {
            Enterprise enterprise = enterpriseService.getEnterpriseByBlockchainAddress(address);
            if (enterprise == null) {
                return ResponseEntity.ok(Result.error(404, "该地址的企业不存在于本地数据库"));
            }
            return ResponseEntity.ok(Result.success(enterprise));
        } catch (Exception e) {
            logger.error("查询企业异常: address={}", address, e);
            return ResponseEntity.ok(Result.error(500, "查询失败"));
        }
    }

    /**
     * 验证企业是否为金融机构
     */
    @Operation(summary = "验证金融机构", description = "检查企业是否为金融机构(entRole=6)。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/check-financial-institution/{entId}")
    public ResponseEntity<Result<FinancialInstitutionCheckDTO>> checkFinancialInstitution(
            @Parameter(description = "企业ID", required = true) @PathVariable Long entId) {
        try {
            boolean isFinInst = enterpriseService.isFinancialInstitution(entId);
            String entRoleName = EntRoleConstant.getRoleName(
                    enterpriseService.getEnterpriseById(entId) != null
                            ? enterpriseService.getEnterpriseById(entId).getEntRole()
                            : null);
            FinancialInstitutionCheckDTO data = new FinancialInstitutionCheckDTO(entId, isFinInst, entRoleName);
            return ResponseEntity.ok(Result.success(data));
        } catch (Exception e) {
            logger.error("验证金融机构异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "验证失败"));
        }
    }

    /**
     * 验证企业是否为金融机构（Feign专用，仅返回布尔值）
     */
    @Operation(summary = "验证金融机构(Feign专用)", description = "检查企业是否为金融机构(entRole=6)，仅返回布尔值供Feign调用。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/check-financial-institution/simple/{entId}")
    public ResponseEntity<Result<Boolean>> checkFinancialInstitutionSimple(
            @Parameter(description = "企业ID", required = true) @PathVariable Long entId) {
        try {
            boolean isFinInst = enterpriseService.isFinancialInstitution(entId);
            return ResponseEntity.ok(Result.success(isFinInst));
        } catch (Exception e) {
            logger.error("验证金融机构异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "验证失败"));
        }
    }

    @Operation(summary = "验证是否为物流企业", description = "根据企业ID验证该企业是否为物流企业（entRole=12）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/check-logistics-enterprise/{entId}")
    public ResponseEntity<Result<Boolean>> checkLogisticsEnterprise(
            @Parameter(description = "企业ID", required = true) @PathVariable Long entId) {
        try {
            boolean isLogistics = enterpriseService.isLogisticsEnterprise(entId);
            return ResponseEntity.ok(Result.success(isLogistics));
        } catch (Exception e) {
            logger.error("验证物流企业异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "验证失败"));
        }
    }

    /**
     * 通过信用代码查询链上企业地址（实际查区块链）
     */
    @Operation(summary = "通过信用代码查询链上企业地址", description = "根据统一社会信用代码查询区块链上注册的企业地址。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回区块链地址"),
        @ApiResponse(responseCode = "404", description = "链上企业不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/chain/code/{creditCode}")
    public ResponseEntity<Result<String>> getEnterpriseByCreditCode(
            @Parameter(description = "信用代码", required = true) @PathVariable String creditCode) {
        try {
            String blockchainAddress = enterpriseService.getEnterpriseAddressByOrgCode(creditCode);
            if (blockchainAddress == null) {
                return ResponseEntity.ok(Result.error(404, "链上企业不存在"));
            }
            return ResponseEntity.ok(Result.success(blockchainAddress));
        } catch (Exception e) {
            logger.error("查询链上企业异常: creditCode={}", creditCode, e);
            return ResponseEntity.ok(Result.error(500, "查询失败"));
        }
    }

    /**
     * 查询链上企业列表
     */
    @Operation(summary = "查询链上企业列表", description = "从区块链上查询所有已注册的企业地址列表。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/chain/list")
    public ResponseEntity<Result<List<String>>> getChainEnterpriseList() {
        try {
            List<String> list = enterpriseService.getEnterpriseListFromChain();
            return ResponseEntity.ok(Result.success(list));
        } catch (Exception e) {
            logger.error("查询链上企业列表异常", e);
            return ResponseEntity.ok(Result.error(500, "查询失败"));
        }
    }

    /**
     * 更新企业信用评级上链
     */
    @Operation(summary = "更新企业信用评级上链", description = "将企业的信用评级信息写入区块链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上链成功，返回交易哈希"),
        @ApiResponse(responseCode = "400", description = "信用评级不能为空"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PutMapping("/{entId}/rating")
    public ResponseEntity<Result<String>> updateCreditRating(
            @Parameter(description = "企业ID", required = true) @PathVariable Long entId,
            @RequestBody Map<String, String> request) {
        try {
            String rating = request.get("rating");
            if (rating == null || rating.isEmpty()) {
                return ResponseEntity.ok(Result.error(400, "信用评级不能为空"));
            }
            String txHash = enterpriseService.updateCreditRatingOnChain(entId, rating);
            return ResponseEntity.ok(Result.success(txHash));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("更新信用评级上链异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败"));
        }
    }

    /**
     * 设置企业信用额度上链
     */
    @Operation(summary = "设置企业信用额度上链", description = "将企业的信用额度信息写入区块链。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上链成功，返回交易哈希"),
        @ApiResponse(responseCode = "400", description = "信用额度格式错误"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PutMapping("/{entId}/credit-limit")
    public ResponseEntity<Result<String>> setCreditLimit(
            @Parameter(description = "企业ID", required = true) @PathVariable Long entId,
            @RequestBody Map<String, Object> request) {
        try {
            Object limitObj = request.get("creditLimit");
            Long creditLimit;
            if (limitObj instanceof Number) {
                creditLimit = ((Number) limitObj).longValue();
            } else if (limitObj instanceof String) {
                creditLimit = Long.parseLong((String) limitObj);
            } else {
                return ResponseEntity.ok(Result.error(400, "信用额度格式错误"));
            }
            String txHash = enterpriseService.setCreditLimitOnChain(entId, creditLimit);
            return ResponseEntity.ok(Result.success(txHash));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("设置信用额度上链异常: entId={}", entId, e);
            return ResponseEntity.ok(Result.error(500, "操作失败"));
        }
    }

    // ==================== 请求/响应对象 ====================

    /**
     * 企业注册请求
     */
    @Schema(description = "企业注册请求")
    static class EnterpriseRegisterRequest {
        @Schema(description = "用户名（登录账号）", example = "enterprise_user")
        private String username;
        @Schema(description = "登录密码", example = "********")
        private String password;
        @Schema(description = "交易密码", example = "********")
        private String payPassword;
        @Schema(description = "企业名称", example = "某科技有限公司")
        private String enterpriseName;
        @Schema(description = "统一社会信用代码", example = "91110000XXXXXXXXXX")
        private String orgCode;
        @Schema(description = "企业角色", example = "1", allowableValues = {"1", "2", "3", "4"})
        private Integer entRole;
        @Schema(description = "企业地址", example = "北京市朝阳区xxx")
        private String localAddress;
        @Schema(description = "联系电话", example = "010-12345678")
        private String contactPhone;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPayPassword() { return payPassword; }
        public void setPayPassword(String payPassword) { this.payPassword = payPassword; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public Integer getEntRole() { return entRole; }
        public void setEntRole(Integer entRole) { this.entRole = entRole; }
        public String getLocalAddress() { return localAddress; }
        public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }

    /**
     * 状态更新请求
     */
    @Schema(description = "状态更新请求")
    static class StatusRequest {
        @Schema(description = "目标状态：0-待审核, 1-正常, 2-已冻结, 3-注销中, 4-已注销", example = "1")
        private BigInteger status;

        public BigInteger getStatus() { return status; }
        public void setStatus(BigInteger status) { this.status = status; }
    }

    /**
     * 企业审核请求
     */
    @Schema(description = "企业审核请求")
    static class AuditRequest {
        @Schema(description = "审核结果：true-通过, false-拒绝", example = "true")
        private Boolean approved;

        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
    }

    /**
     * 登录请求
     */
    @Schema(description = "企业登录请求")
    static class LoginRequest {
        @Schema(description = "用户名", example = "enterprise_user")
        private String username;
        @Schema(description = "密码", example = "********")
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 密码更新请求
     */
    @Schema(description = "密码更新请求")
    static class PasswordUpdateRequest {
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "原密码", example = "********")
        private String oldPassword;
        @Schema(description = "新密码", example = "********")
        private String newPassword;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * 企业详情响应
     */
    @Schema(description = "企业详情响应")
    static class EnterpriseDetailResponse {
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "用户名", example = "enterprise_user")
        private String username;
        @Schema(description = "企业名称", example = "某科技有限公司")
        private String enterpriseName;
        @Schema(description = "统一社会信用代码", example = "91110000XXXXXXXXXX")
        private String orgCode;
        @Schema(description = "企业地址", example = "北京市朝阳区某路123号")
        private String localAddress;
        @Schema(description = "联系电话", example = "010-12345678")
        private String contactPhone;
        @Schema(description = "企业角色", example = "1", allowableValues = {"1", "2", "3", "4"})
        private Integer entRole;
        @Schema(description = "企业角色名称", example = "核心企业")
        private String entRoleName;
        @Schema(description = "状态", example = "1", allowableValues = {"0", "1", "2", "3", "4"})
        private Integer status;
        @Schema(description = "状态名称", example = "正常")
        private String statusName;
        @Schema(description = "区块链地址", example = "0xabc123...")
        private String blockchainAddress;
        @Schema(description = "创建时间", example = "2026-01-01T10:00:00")
        private java.time.LocalDateTime createTime;
        @Schema(description = "更新时间", example = "2026-03-15T14:30:00")
        private java.time.LocalDateTime updateTime;

        // Getters and Setters
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public String getLocalAddress() { return localAddress; }
        public void setLocalAddress(String localAddress) { this.localAddress = localAddress; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public Integer getEntRole() { return entRole; }
        public void setEntRole(Integer entRole) { this.entRole = entRole; }
        public String getEntRoleName() { return entRoleName; }
        public void setEntRoleName(String entRoleName) { this.entRoleName = entRoleName; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getBlockchainAddress() { return blockchainAddress; }
        public void setBlockchainAddress(String blockchainAddress) { this.blockchainAddress = blockchainAddress; }
        public java.time.LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
        public java.time.LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    }

    /**
     * 邀请码响应
     */
    @Schema(description = "邀请码响应")
    static class InvitationCodeResponse {
        @Schema(description = "邀请码", example = "INVITE123456")
        private String code;
        @Schema(description = "最大使用次数", example = "10")
        private Integer maxUses;
        @Schema(description = "过期天数", example = "30")
        private Integer expireDays;
        @Schema(description = "备注", example = "仅限邀请新企业")
        private String remark;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public Integer getMaxUses() { return maxUses; }
        public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
        public Integer getExpireDays() { return expireDays; }
        public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    // ==================== 辅助方法 ====================

    private EnterpriseDetailResponse convertToDetailResponse(Enterprise enterprise) {
        EnterpriseDetailResponse response = new EnterpriseDetailResponse();
        response.setEntId(enterprise.getEntId());
        response.setUsername(enterprise.getUsername());
        response.setEnterpriseName(enterprise.getEnterpriseName());
        response.setOrgCode(enterprise.getOrgCode());
        response.setLocalAddress(enterprise.getLocalAddress());
        response.setContactPhone(enterprise.getContactPhone());
        response.setEntRole(enterprise.getEntRole());
        response.setEntRoleName(EntRoleConstant.getRoleName(enterprise.getEntRole()));
        response.setStatus(enterprise.getStatus());
        response.setStatusName(getStatusName(enterprise.getStatus()));
        response.setBlockchainAddress(enterprise.getBlockchainAddress());
        response.setCreateTime(enterprise.getCreateTime());
        response.setUpdateTime(enterprise.getUpdateTime());
        return response;
    }

    private String getStatusName(Integer status) {
        if (status == null) return null;
        switch (status) {
            case 0: return "待审核";
            case 1: return "正常";
            case 2: return "已冻结";
            case 3: return "注销中";
            case 4: return "已注销";
            default: return "未知状态";
        }
    }
}
