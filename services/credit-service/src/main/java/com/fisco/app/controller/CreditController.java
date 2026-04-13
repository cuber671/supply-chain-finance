package com.fisco.app.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.annotation.RequireRole;
import com.fisco.app.entity.CreditEvent;
import com.fisco.app.service.CreditService;
import com.fisco.app.service.CreditService.CreditPortrait;
import com.fisco.app.service.CreditService.CreditScoreResult;
import com.fisco.app.service.CreditService.LimitCheckResult;
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
 * 信用管理 Controller
 *
 * 提供企业信用档案管理、信用事件上报、信用评分计算等 API
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "信用管理", description = "企业信用档案管理、信用事件上报、信用评分计算、授信额度管理、黑名单管理")
@RestController
@RequestMapping("/api/v1/credit")
public class CreditController {

    private static final Logger logger = LoggerFactory.getLogger(CreditController.class);

    @Autowired
    private CreditService creditService;

    // ==================== 信用画像查询 ====================

    /**
     * 获取企业信用画像
     */
    @Operation(summary = "获取企业信用画像", description = "获取指定企业的信用画像信息，包括信用评分、信用等级、可用额度等。不传企业ID则查询当前企业。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以查询其他企业信用画像"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/profile")
    public Result<CreditPortraitResponse> getCreditProfile(
            @Parameter(description = "企业ID，不传则查询当前企业", example = "123") @RequestParam(required = false) Long entId,
            HttpServletRequest request) {
        try {
            Long currentEntId = getCurrentEntId(request);
            if (currentEntId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }
            // 当 entId 为空或等于当前企业时，无需特殊权限
            // 当 entId 不等于当前企业时，需要 ADMIN 角色
            if (entId != null && !entId.equals(currentEntId) && !CurrentUser.isAdmin()) {
                return Result.error(403, "仅管理员可以查询其他企业信用画像");
            }
            Long actualEntId = entId != null ? entId : currentEntId;

            CreditPortrait portrait = creditService.getCreditPortrait(actualEntId);
            return Result.success(convertToPortraitResponse(portrait));

        } catch (Exception e) {
            logger.error("获取信用画像异常: ", e);
            return Result.error(500, "获取信用画像失败");
        }
    }

    /**
     * 获取当前企业信用画像
     */
    @Operation(summary = "获取当前企业信用画像", description = "获取当前登录企业的信用画像信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/profile/me")
    public Result<CreditPortraitResponse> getMyCreditProfile(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntId(request);
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            CreditPortrait portrait = creditService.getCreditPortrait(entId);
            return Result.success(convertToPortraitResponse(portrait));

        } catch (Exception e) {
            logger.error("获取当前企业信用画像异常", e);
            return Result.error(500, "获取信用画像异常: " + e.getMessage());
        }
    }

    /**
     * 获取信用评分
     */
    @Operation(summary = "获取信用评分", description = "获取指定企业的信用评分信息，包括评分、信用等级、可用额度等。不传企业ID则查询当前企业。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以查询其他企业信用评分"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/score")
    public Result<CreditScoreResultResponse> getCreditScore(
            @Parameter(description = "企业ID，不传则查询当前企业", example = "123") @RequestParam(required = false) Long entId,
            HttpServletRequest request) {
        try {
            Long currentEntId = getCurrentEntId(request);
            if (currentEntId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }
            // 当 entId 为空或等于当前企业时，无需特殊权限
            // 当 entId 不等于当前企业时，需要 ADMIN 角色
            if (entId != null && !entId.equals(currentEntId) && !CurrentUser.isAdmin()) {
                return Result.error(403, "仅管理员可以查询其他企业信用评分");
            }
            Long actualEntId = entId != null ? entId : currentEntId;

            CreditScoreResult scoreResult = creditService.getCreditScore(actualEntId);
            return Result.success(convertToScoreResponse(scoreResult));

        } catch (Exception e) {
            logger.error("获取信用评分异常: ", e);
            return Result.error(500, "获取信用评分失败");
        }
    }

    // ==================== 信用事件管理 ====================

    /**
     * 上报信用事件
     */
    @Operation(summary = "上报信用事件", description = "管理员上报企业信用事件，记录到区块链并影响企业信用评分。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上报成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：企业ID、事件类型、事件等级不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以上报信用事件"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PostMapping("/event/report")
    public Result<Map<String, Object>> reportCreditEvent(
            @Parameter(description = "信用事件信息", required = true) @RequestBody CreditEventReportRequest request) {
        try {
            // 权限校验
            if (!CurrentUser.isAdmin()) {
                return Result.error(403, "仅管理员可以上报信用事件");
            }
            // 参数校验
            if (request.getEntId() == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getEventType() == null || request.getEventType().isEmpty()) {
                return Result.error(400, "事件类型不能为空");
            }
            if (request.getEventLevel() == null || request.getEventLevel().isEmpty()) {
                return Result.error(400, "事件等级不能为空");
            }

            Long eventId = creditService.reportCreditEvent(
                    request.getEntId(),
                    request.getEventType(),
                    request.getEventLevel(),
                    request.getEventDesc(),
                    request.getScoreChange(),
                    request.getRelatedModule(),
                    request.getRelatedId()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("eventId", eventId);
            result.put("message", "信用事件上报成功");

            logger.info("信用事件上报成功: entId={}, eventId={}, type={}",
                    request.getEntId(), eventId, request.getEventType());

            return Result.success(result);

        } catch (Exception e) {
            logger.error("上报信用事件异常: entId={}", request.getEntId(), e);
            return Result.error(500, "上报信用事件异常: " + e.getMessage());
        }
    }

    /**
     * 物流偏航触发信用扣分
     */
    @Operation(summary = "物流偏航触发信用扣分", description = "当物流运输发生偏航时，自动触发信用扣分。偏航级别决定扣分分值：轻度(-10分)、中度(-15分)、严重(-25分)。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "扣分上报成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：企业ID、物流订单ID不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以触发物流偏航信用扣分"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PostMapping("/event/logistics-deviation")
    public Result<Map<String, Object>> reportLogisticsDeviation(
            @Parameter(description = "偏航事件信息", required = true) @RequestBody LogisticsDeviationRequest request) {
        try {
            // 权限校验
            if (!CurrentUser.isAdmin()) {
                return Result.error(403, "仅管理员可以触发物流偏航信用扣分");
            }
            // 参数校验
            if (request.getEntId() == null) {
                return Result.error(400, "企业ID不能为空");
            }
            if (request.getLogisticsOrderId() == null || request.getLogisticsOrderId().isEmpty()) {
                return Result.error(400, "物流订单ID不能为空");
            }

            // 偏航级别对应的扣分数
            Integer scoreChange;
            String eventLevel;
            String eventDesc;

            if (request.getDeviationLevel() == null || request.getDeviationLevel() <= 1) {
                scoreChange = -10;
                eventLevel = CreditEvent.EVENT_LEVEL_LOW;
                eventDesc = "物流路径轻度偏移，偏离预定路线";
            } else if (request.getDeviationLevel() <= 2) {
                scoreChange = -15;
                eventLevel = CreditEvent.EVENT_LEVEL_MEDIUM;
                eventDesc = "物流路径中度偏移，存在绕路嫌疑";
            } else {
                scoreChange = -25;
                eventLevel = CreditEvent.EVENT_LEVEL_HIGH;
                eventDesc = "物流路径严重偏移，可能存在异常";
            }

            if (request.getDeviationDesc() != null && !request.getDeviationDesc().isEmpty()) {
                eventDesc = request.getDeviationDesc();
            }

            Long eventId = creditService.reportCreditEvent(
                    request.getEntId(),
                    CreditEvent.EVENT_TYPE_LOGISTICS_DEVIATION,
                    eventLevel,
                    eventDesc,
                    scoreChange,
                    "LOGISTICS",
                    request.getLogisticsOrderId()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("eventId", eventId);
            result.put("scoreChange", scoreChange);
            result.put("eventLevel", eventLevel);
            result.put("message", "物流偏航扣分上报成功");

            logger.info("物流偏航触发信用扣分: entId={}, orderId={}, deviationLevel={}, scoreChange={}",
                    request.getEntId(), request.getLogisticsOrderId(), request.getDeviationLevel(), scoreChange);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("物流偏航扣分异常: entId={}", request.getEntId(), e);
            return Result.error(500, "物流偏航扣分异常: " + e.getMessage());
        }
    }

    /**
     * 查询企业信用事件列表
     */
    @Operation(summary = "查询企业信用事件列表", description = "查询指定企业的信用事件记录列表，支持按事件类型筛选。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @GetMapping("/events")
    public Result<List<CreditEventResponse>> listCreditEvents(
            @Parameter(description = "企业ID，不传则查询当前企业", example = "123")
            @RequestParam(required = false) Long entId,
            @Parameter(description = "事件类型", example = "LOAN_OVERDUE")
            @RequestParam(required = false) String eventType,
            HttpServletRequest request) {
        try {
            if (entId == null) {
                entId = getCurrentEntId(request);
            }
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            List<CreditEvent> events;
            if (eventType != null && !eventType.isEmpty()) {
                events = creditService.listCreditEventsByType(entId, eventType);
            } else {
                events = creditService.listCreditEvents(entId);
            }

            return Result.success(convertToEventList(events));

        } catch (Exception e) {
            logger.error("查询信用事件列表异常: ", e);
            return Result.error(500, "查询信用事件列表异常: " + e.getMessage());
        }
    }

    // ==================== 信用额度管理 ====================

    /**
     * 设置授信额度
     */
    @Operation(summary = "设置授信额度", description = "管理员设置企业的授信额度，额度信息将上链存储。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "设置成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：授信额度必须大于0"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以设置授信额度"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PutMapping("/limit")
    public Result<Map<String, Object>> setCreditLimit(
            @Parameter(description = "授信额度", required = true)
            @RequestParam BigDecimal availableLimit) {
        try {
            Long entId = getCurrentEntIdFromUser();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }
            if (availableLimit == null || availableLimit.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "授信额度必须大于0");
            }

            boolean success = creditService.setCreditLimit(entId, availableLimit);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("availableLimit", availableLimit);
            result.put("success", success);

            logger.info("设置授信额度: entId={}, limit={}", entId, availableLimit);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            logger.warn("设置授信额度参数异常: ", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("设置授信额度异常: ", e);
            return Result.error(500, "设置授信额度失败，请稍后重试");
        }
    }

    /**
     * 额度校验
     */
    @Operation(summary = "额度校验", description = "校验企业在贷款、质押等业务场景下的可用信用额度是否满足需求。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "校验成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：需求金额必须大于0"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PostMapping("/limit/check")
    public Result<LimitCheckResultResponse> checkCreditLimit(
            @Parameter(description = "额度校验信息", required = true) @RequestBody CreditLimitCheckRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long entId = request.getEntId();
            if (entId == null) {
                entId = getCurrentEntId(httpRequest);
            }
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            if (request.getRequiredAmount() == null || request.getRequiredAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "需求金额必须大于0");
            }

            LimitCheckResult checkResult = creditService.checkCreditLimit(entId, request.getRequiredAmount());
            return Result.success(convertToLimitCheckResponse(checkResult));

        } catch (Exception e) {
            logger.error("额度校验异常", e);
            return Result.error(500, "额度校验异常: " + e.getMessage());
        }
    }

    /**
     * 额度实时锁死
     */
    @Operation(summary = "额度实时锁死", description = "管理员紧急锁死企业信用额度，锁死后企业无法使用任何信用额度。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以锁死额度"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PostMapping("/limit/lock")
    public Result<Map<String, Object>> lockCreditLimit(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntIdFromUser();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean success = creditService.lockCreditLimit(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("success", success);
            result.put("message", success ? "额度已锁死" : "锁死失败");

            logger.warn("额度实时锁死: entId={}, success={}", entId, success);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("额度锁死异常: ", e);
            return Result.error(500, "额度锁死异常: " + e.getMessage());
        }
    }

    /**
     * 获取可用信用额度
     */
    @Operation(summary = "获取可用信用额度", description = "查询企业的可用信用额度。不传企业ID则查询当前企业。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以查询其他企业授信额度"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/limit/available")
    public Result<Map<String, Object>> getAvailableCreditLimit(
            @Parameter(description = "企业ID，不传则查询当前企业", example = "123") @RequestParam(required = false) Long entId,
            HttpServletRequest request) {
        try {
            Long currentEntId = getCurrentEntIdFromUser();
            if (currentEntId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }
            // 当 entId 为空或等于当前企业时，无需特殊权限
            // 当 entId 不等于当前企业时，需要 ADMIN 角色
            if (entId != null && !entId.equals(currentEntId) && !CurrentUser.isAdmin()) {
                return Result.error(403, "仅管理员可以查询其他企业授信额度");
            }
            Long actualEntId = entId != null ? entId : currentEntId;

            BigDecimal available = creditService.getAvailableCreditLimit(actualEntId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", actualEntId);
            result.put("availableLimit", available);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("获取可用额度异常: ", e);
            return Result.error(500, "获取可用额度异常: " + e.getMessage());
        }
    }

    // ==================== 信用评分计算 ====================

    /**
     * 信用等级重算
     */
    @Operation(summary = "信用等级重算", description = "管理员触发企业信用等级重新计算，根据最新信用事件更新评分和等级。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重算成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以重算信用等级"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PatchMapping("/reevaluate")
    public Result<Map<String, Object>> recalculateCreditLevel(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntIdFromUser();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            String newLevel = creditService.recalculateCreditLevel(entId);
            CreditScoreResult scoreResult = creditService.getCreditScore(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("creditScore", scoreResult.getCreditScore());
            result.put("creditLevel", newLevel);
            result.put("message", "信用等级重算完成");

            logger.info("信用等级重算: entId={}, score={}, level={}",
                    entId, scoreResult.getCreditScore(), newLevel);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("信用等级重算异常: ", e);
            return Result.error(500, "信用等级重算异常: " + e.getMessage());
        }
    }

    /**
     * 批量信用等级重算
     */
    @Operation(summary = "批量信用等级重算", description = "管理员批量触发多个企业信用等级重新计算。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批量重算完成"),
        @ApiResponse(responseCode = "400", description = "参数错误：企业ID列表不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以批量重算"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PatchMapping("/reevaluate/batch")
    public Result<Map<String, Object>> batchRecalculateCreditLevel(
            @Parameter(description = "批量重算企业ID列表", required = true)
            @RequestBody BatchRecalculateRequest request) {
        try {
            if (request.getEntIds() == null || request.getEntIds().isEmpty()) {
                return Result.error(400, "企业ID列表不能为空");
            }

            int successCount = 0;
            int failCount = 0;

            for (Long entId : request.getEntIds()) {
                try {
                    creditService.recalculateCreditLevel(entId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    logger.error("批量重算失败: ", e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", request.getEntIds().size());
            result.put("success", successCount);
            result.put("failed", failCount);
            result.put("message", "批量重算完成");

            logger.info("批量信用等级重算: total={}, success={}, fail={}",
                    request.getEntIds().size(), successCount, failCount);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("批量信用等级重算异常", e);
            return Result.error(500, "批量重算异常: " + e.getMessage());
        }
    }

    // ==================== 信用黑名单 ====================

    /**
     * 检查是否触发信用黑名单
     */
    @Operation(summary = "检查信用黑名单", description = "管理员检查当前企业是否在信用黑名单中。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "检查成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以检查黑名单"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/blacklist/check")
    @RequireRole({"ADMIN"})
    public Result<Map<String, Object>> checkBlacklist(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntIdFromUser();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean isBlacklisted = creditService.checkBlacklist(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("isBlacklisted", isBlacklisted);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("检查黑名单异常: ", e);
            return Result.error(500, "检查黑名单异常: " + e.getMessage());
        }
    }

    /**
     * 触发信用黑名单
     */
    @Operation(summary = "触发信用黑名单", description = "管理员触发将当前企业列入信用黑名单，列入后企业无法使用授信服务。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以触发黑名单"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @PostMapping("/blacklist/trigger")
    public Result<Map<String, Object>> triggerBlacklist(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntIdFromUser();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean success = creditService.triggerBlacklist(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("success", success);
            result.put("message", success ? "已触发信用黑名单" : "触发失败");

            logger.warn("触发信用黑名单: entId={}, success={}", entId, success);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("触发黑名单异常: ", e);
            return Result.error(500, "触发黑名单异常: " + e.getMessage());
        }
    }

    /**
     * 移除信用黑名单
     */
    @Operation(summary = "移除信用黑名单", description = "管理员移除当前企业信用黑名单，需企业信用分恢复到阈值以上。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "移除成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限：仅管理员可以移除黑名单"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @RequireRole({"ADMIN"})
    @DeleteMapping("/blacklist/remove")
    public Result<Map<String, Object>> removeBlacklist(HttpServletRequest request) {
        try {
            Long entId = getCurrentEntIdFromUser();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            boolean success = creditService.removeBlacklist(entId);

            Map<String, Object> result = new HashMap<>();
            result.put("entId", entId);
            result.put("success", success);
            result.put("message", success ? "已移除信用黑名单" : "移除失败，信用分仍低于阈值");

            logger.info("移除信用黑名单: entId={}, success={}", entId, success);

            return Result.success(result);

        } catch (Exception e) {
            logger.error("移除黑名单异常: ", e);
            return Result.error(500, "移除黑名单异常: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private Long getCurrentEntId(HttpServletRequest request) {
        Object entIdAttr = request.getAttribute("ent_id");
        if (entIdAttr != null) {
            try {
                return Long.parseLong(entIdAttr.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long getCurrentEntIdFromUser() {
        return CurrentUser.getEntId();
    }

    private CreditPortraitResponse convertToPortraitResponse(CreditPortrait portrait) {
        if (portrait == null) {
            return null;
        }
        CreditPortraitResponse response = new CreditPortraitResponse();
        response.setEntId(portrait.getEntId());
        response.setEnterpriseName(portrait.getEnterpriseName());
        response.setCreditScore(portrait.getCreditScore());
        response.setCreditLevel(portrait.getCreditLevel());
        response.setAvailableLimit(portrait.getAvailableLimit());
        response.setUsedLimit(portrait.getUsedLimit());
        response.setAvailableBalance(portrait.getAvailableBalance());
        response.setOverdueCount(portrait.getOverdueCount());
        response.setLastEvalTime(portrait.getLastEvalTime());
        response.setIsBlacklisted(portrait.getIsBlacklisted());
        return response;
    }

    private CreditScoreResultResponse convertToScoreResponse(CreditScoreResult scoreResult) {
        if (scoreResult == null) {
            return null;
        }
        CreditScoreResultResponse response = new CreditScoreResultResponse();
        response.setEntId(scoreResult.getEntId());
        response.setCreditScore(scoreResult.getCreditScore());
        response.setCreditLevel(scoreResult.getCreditLevel());
        response.setLastEvalTime(scoreResult.getLastEvalTime());
        response.setAvailableLimit(scoreResult.getAvailableLimit());
        response.setUsedLimit(scoreResult.getUsedLimit());
        response.setOverdueCount(scoreResult.getOverdueCount());
        return response;
    }

    private LimitCheckResultResponse convertToLimitCheckResponse(LimitCheckResult checkResult) {
        if (checkResult == null) {
            return null;
        }
        LimitCheckResultResponse response = new LimitCheckResultResponse();
        response.setPassed(checkResult.isPassed());
        response.setMessage(checkResult.getMessage());
        response.setAvailableLimit(checkResult.getAvailableLimit());
        response.setRequiredAmount(checkResult.getRequiredAmount());
        response.setEntId(checkResult.getEntId());
        return response;
    }

    private List<CreditEventResponse> convertToEventList(List<CreditEvent> events) {
        if (events == null) {
            return null;
        }
        return events.stream().map(this::convertToEventResponse).collect(Collectors.toList());
    }

    private CreditEventResponse convertToEventResponse(CreditEvent event) {
        if (event == null) {
            return null;
        }
        CreditEventResponse response = new CreditEventResponse();
        response.setId(event.getId());
        response.setEntId(event.getEntId());
        response.setEventType(event.getEventType());
        response.setEventLevel(event.getEventLevel());
        response.setEventDesc(event.getEventDesc());
        response.setScoreChange(event.getScoreChange());
        response.setRelatedModule(event.getRelatedModule());
        response.setRelatedId(event.getRelatedId());
        response.setChainTxHash(event.getChainTxHash());
        response.setReportTime(event.getReportTime() != null ? event.getReportTime().toString() : null);
        return response;
    }

    // ==================== 请求/响应类 ====================

    /**
     * 信用事件上报请求
     */
    @Schema(description = "信用事件上报请求")
    public static class CreditEventReportRequest {
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "事件类型", example = "OVERDUE", allowableValues = {"OVERDUE", "DEFAULTER", "EARLY_REPAY", "ON_TIME_REPAY", "GOODS_UNDAMAGED", "STABLE_STORAGE", "LOGISTICS_DEVIATION", "RECEIPT_ABNORMAL", "FREQUENT_CANCEL"})
        private String eventType;
        @Schema(description = "事件等级", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "SEVERE"})
        private String eventLevel;
        @Schema(description = "事件描述（可选）", example = "贷款逾期30天")
        private String eventDesc;
        @Schema(description = "评分变化（可选）", example = "-20")
        private Integer scoreChange;
        @Schema(description = "关联模块（可选）", example = "LOAN")
        private String relatedModule;
        @Schema(description = "关联ID（可选）", example = "100")
        private String relatedId;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventLevel() { return eventLevel; }
        public void setEventLevel(String eventLevel) { this.eventLevel = eventLevel; }
        public String getEventDesc() { return eventDesc; }
        public void setEventDesc(String eventDesc) { this.eventDesc = eventDesc; }
        public Integer getScoreChange() { return scoreChange; }
        public void setScoreChange(Integer scoreChange) { this.scoreChange = scoreChange; }
        public String getRelatedModule() { return relatedModule; }
        public void setRelatedModule(String relatedModule) { this.relatedModule = relatedModule; }
        public String getRelatedId() { return relatedId; }
        public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    }

    /**
     * 物流偏航扣分请求
     */
    @Schema(description = "物流偏航扣分请求")
    public static class LogisticsDeviationRequest {
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "物流订单ID", example = "LOG20260324001")
        private String logisticsOrderId;
        @Schema(description = "偏航级别：1-轻度、2-中度、3-严重", example = "2", allowableValues = {"1", "2", "3"})
        private Integer deviationLevel;
        @Schema(description = "偏航描述（可选）", example = "偏离预定路线10公里")
        private String deviationDesc;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getLogisticsOrderId() { return logisticsOrderId; }
        public void setLogisticsOrderId(String logisticsOrderId) { this.logisticsOrderId = logisticsOrderId; }
        public Integer getDeviationLevel() { return deviationLevel; }
        public void setDeviationLevel(Integer deviationLevel) { this.deviationLevel = deviationLevel; }
        public String getDeviationDesc() { return deviationDesc; }
        public void setDeviationDesc(String deviationDesc) { this.deviationDesc = deviationDesc; }
    }

    /**
     * 额度校验请求
     */
    @Schema(description = "额度校验请求")
    public static class CreditLimitCheckRequest {
        @Schema(description = "企业ID（可选，不传则使用当前企业）", example = "1")
        private Long entId;
        @Schema(description = "需求金额", example = "50000.00")
        private BigDecimal requiredAmount;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public BigDecimal getRequiredAmount() { return requiredAmount; }
        public void setRequiredAmount(BigDecimal requiredAmount) { this.requiredAmount = requiredAmount; }
    }

    /**
     * 批量重算请求
     */
    @Schema(description = "批量重算请求")
    public static class BatchRecalculateRequest {
        @Schema(description = "企业ID列表", example = "[1, 2, 3]")
        private List<Long> entIds;

        public List<Long> getEntIds() { return entIds; }
        public void setEntIds(List<Long> entIds) { this.entIds = entIds; }
    }

    // ==================== 响应类 ====================

    /**
     * 信用画像响应
     */
    @Schema(description = "信用画像响应")
    public static class CreditPortraitResponse {
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "企业名称", example = "某供应链公司")
        private String enterpriseName;
        @Schema(description = "信用评分", example = "750")
        private Integer creditScore;
        @Schema(description = "信用等级", example = "AA", allowableValues = {"AAA", "AA", "A", "BBB", "BB", "B", "CCC", "CC", "C"})
        private String creditLevel;
        @Schema(description = "可用额度", example = "100000.00")
        private BigDecimal availableLimit;
        @Schema(description = "已用额度", example = "30000.00")
        private BigDecimal usedLimit;
        @Schema(description = "可用余额", example = "70000.00")
        private BigDecimal availableBalance;
        @Schema(description = "逾期次数", example = "0")
        private Integer overdueCount;
        @Schema(description = "上次评估时间", example = "2026-03-24 10:00:00")
        private String lastEvalTime;
        @Schema(description = "是否在黑名单", example = "false")
        private Boolean isBlacklisted;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEnterpriseName() { return enterpriseName; }
        public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public BigDecimal getUsedLimit() { return usedLimit; }
        public void setUsedLimit(BigDecimal usedLimit) { this.usedLimit = usedLimit; }
        public BigDecimal getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
        public String getLastEvalTime() { return lastEvalTime; }
        public void setLastEvalTime(String lastEvalTime) { this.lastEvalTime = lastEvalTime; }
        public Boolean getIsBlacklisted() { return isBlacklisted; }
        public void setIsBlacklisted(Boolean isBlacklisted) { this.isBlacklisted = isBlacklisted; }
    }

    /**
     * 信用评分响应
     */
    @Schema(description = "信用评分响应")
    public static class CreditScoreResultResponse {
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "信用评分", example = "750")
        private Integer creditScore;
        @Schema(description = "信用等级", example = "AA", allowableValues = {"AAA", "AA", "A", "BBB", "BB", "B", "CCC", "CC", "C"})
        private String creditLevel;
        @Schema(description = "上次评估时间", example = "2026-03-24 10:00:00")
        private String lastEvalTime;
        @Schema(description = "可用额度", example = "100000.00")
        private BigDecimal availableLimit;
        @Schema(description = "已用额度", example = "30000.00")
        private BigDecimal usedLimit;
        @Schema(description = "逾期次数", example = "0")
        private Integer overdueCount;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
        public String getCreditLevel() { return creditLevel; }
        public void setCreditLevel(String creditLevel) { this.creditLevel = creditLevel; }
        public String getLastEvalTime() { return lastEvalTime; }
        public void setLastEvalTime(String lastEvalTime) { this.lastEvalTime = lastEvalTime; }
        public BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public BigDecimal getUsedLimit() { return usedLimit; }
        public void setUsedLimit(BigDecimal usedLimit) { this.usedLimit = usedLimit; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
    }

    /**
     * 额度校验响应
     */
    @Schema(description = "额度校验响应")
    public static class LimitCheckResultResponse {
        @Schema(description = "是否通过校验", example = "true")
        private boolean passed;
        @Schema(description = "校验结果描述", example = "额度充足，可进行贷款")
        private String message;
        @Schema(description = "可用额度", example = "100000.00")
        private BigDecimal availableLimit;
        @Schema(description = "需求金额", example = "50000.00")
        private BigDecimal requiredAmount;
        @Schema(description = "企业ID", example = "1")
        private Long entId;

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public BigDecimal getAvailableLimit() { return availableLimit; }
        public void setAvailableLimit(BigDecimal availableLimit) { this.availableLimit = availableLimit; }
        public BigDecimal getRequiredAmount() { return requiredAmount; }
        public void setRequiredAmount(BigDecimal requiredAmount) { this.requiredAmount = requiredAmount; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
    }

    /**
     * 信用事件响应
     */
    @Schema(description = "信用事件响应")
    public static class CreditEventResponse {
        @Schema(description = "事件ID", example = "1")
        private Long id;
        @Schema(description = "企业ID", example = "1")
        private Long entId;
        @Schema(description = "事件类型", example = "OVERDUE", allowableValues = {"OVERDUE", "DEFAULTER", "EARLY_REPAY", "ON_TIME_REPAY", "GOODS_UNDAMAGED", "STABLE_STORAGE", "LOGISTICS_DEVIATION", "RECEIPT_ABNORMAL", "FREQUENT_CANCEL"})
        private String eventType;
        @Schema(description = "事件等级", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "SEVERE"})
        private String eventLevel;
        @Schema(description = "事件描述", example = "贷款逾期30天")
        private String eventDesc;
        @Schema(description = "评分变化", example = "-20")
        private Integer scoreChange;
        @Schema(description = "关联模块", example = "FINANCE", allowableValues = {"WAREHOUSE", "LOGISTICS", "FINANCE"})
        private String relatedModule;
        @Schema(description = "关联ID", example = "100")
        private String relatedId;
        @Schema(description = "链上交易哈希", example = "0xabc123...")
        private String chainTxHash;
        @Schema(description = "上报时间", example = "2026-03-24 10:00:00")
        private String reportTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getEventLevel() { return eventLevel; }
        public void setEventLevel(String eventLevel) { this.eventLevel = eventLevel; }
        public String getEventDesc() { return eventDesc; }
        public void setEventDesc(String eventDesc) { this.eventDesc = eventDesc; }
        public Integer getScoreChange() { return scoreChange; }
        public void setScoreChange(Integer scoreChange) { this.scoreChange = scoreChange; }
        public String getRelatedModule() { return relatedModule; }
        public void setRelatedModule(String relatedModule) { this.relatedModule = relatedModule; }
        public String getRelatedId() { return relatedId; }
        public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
        public String getChainTxHash() { return chainTxHash; }
        public void setChainTxHash(String chainTxHash) { this.chainTxHash = chainTxHash; }
        public String getReportTime() { return reportTime; }
        public void setReportTime(String reportTime) { this.reportTime = reportTime; }
    }
}
