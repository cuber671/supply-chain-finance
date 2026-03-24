package com.fisco.app.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.entity.LogisticsTrack;
import com.fisco.app.service.LogisticsService;
import com.fisco.app.util.CurrentUser;
import com.fisco.app.util.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 物流控制器
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "物流管理", description = "物流委派单管理、物流追踪、轨迹上报、状态更新")
@RestController
@RequestMapping("/api/v1/logistics")
public class LogisticsController {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsController.class);

    @Autowired
    private LogisticsService logisticsService;

    // ==================== 委派单管理 ====================

    @Operation(summary = "创建物流委派单", description = "货主企业创建物流委派单，指定业务场景、运输数量、承运企业等信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：业务场景、运输数量、计量单位、承运企业ID不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/create")
    public Result<LogisticsDelegate> createDelegate(
            @Parameter(description = "委派单信息", required = true) @RequestBody LogisticsDelegate delegate) {
        try {
            if (delegate.getBusinessScene() == null) {
                return Result.error(400, "业务场景不能为空");
            }
            if (delegate.getTransportQuantity() == null) {
                return Result.error(400, "运输数量不能为空");
            }
            if (delegate.getUnit() == null || delegate.getUnit().isEmpty()) {
                return Result.error(400, "计量单位不能为空");
            }
            if (delegate.getCarrierEntId() == null) {
                return Result.error(400, "承运企业ID不能为空");
            }
            if (delegate.getActionOnArrival() == null) {
                delegate.setActionOnArrival(1);
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            delegate.setOwnerEntId(entId);
            LogisticsDelegate result = logisticsService.createDelegate(delegate);

            logger.info("创建物流委派单成功: voucherNo={}, entId={}", result.getVoucherNo(), entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("创建物流委派单异常", e);
            return Result.error(500, "创建物流委派单失败");
        }
    }

    @Operation(summary = "查询委派单详情", description = "根据委派单编号查询委派单详情。仅委派单所属企业或承运企业可查询。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限查询该委派单"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/delegate/{voucherNo}")
    public Result<LogisticsDelegate> getDelegate(
            @Parameter(description = "委派单编号", required = true) @PathVariable String voucherNo) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单");
            }

            return Result.success(delegate);

        } catch (Exception e) {
            logger.error("查询委派单异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "查询企业委派单列表", description = "查询当前企业用户的所有委派单列表，支持按业务场景和状态筛选。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/delegate/list")
    public Result<List<LogisticsDelegate>> listDelegates(
            @Parameter(description = "业务场景筛选") @RequestParam(required = false) Integer businessScene,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 使用优化后的单次查询，支持 SQL 级别过滤
            List<LogisticsDelegate> delegates = logisticsService.listByEntIdWithFilters(entId, businessScene, status);

            return Result.success(delegates);

        } catch (Exception e) {
            logger.error("查询委派单列表异常", e);
            return Result.error(500, "查询失败");
        }
    }

    // ==================== 物流指派 ====================

    @Operation(summary = "物流指派任务", description = "承运企业指派司机和车辆到物流委派单。委派单编号、司机ID、司机姓名、车牌号均为必填。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "指派成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：委派单编号、司机ID、司机姓名、车牌号不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单，只有承运企业才能指派司机"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/assign")
    public Result<LogisticsDelegate> assignDriver(
            @Parameter(description = "指派信息", required = true) @RequestBody Map<String, Object> params) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            String driverId = params.get("driverId") != null ? params.get("driverId").toString() : null;
            String driverName = params.get("driverName") != null ? params.get("driverName").toString() : null;
            String vehicleNo = params.get("vehicleNo") != null ? params.get("vehicleNo").toString() : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }
            if (driverId == null || driverId.isEmpty()) {
                return Result.error(400, "司机ID不能为空");
            }
            if (driverName == null || driverName.isEmpty()) {
                return Result.error(400, "司机姓名不能为空");
            }
            if (vehicleNo == null || vehicleNo.isEmpty()) {
                return Result.error(400, "车牌号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单，只有承运企业才能指派司机");
            }

            LogisticsDelegate result = logisticsService.assignDriver(voucherNo, driverId, driverName, vehicleNo);
            logger.info("物流指派任务完成: voucherNo={}, entId={}", voucherNo, entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("物流指派异常", e);
            return Result.error(500, "指派失败");
        }
    }

    // ==================== 提货确认 ====================

    @Operation(summary = "仓库提货确认", description = "司机到达仓库后凭授权码确认提货，支持GPS坐标上报。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "提货确认成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：委派单编号不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/pickup")
    public Result<LogisticsDelegate> confirmPickup(
            @Parameter(description = "提货确认信息", required = true) @RequestBody Map<String, Object> params) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            String authCode = params.get("authCode") != null ? params.get("authCode").toString() : null;
            BigDecimal driverLatitude = params.get("driverLatitude") != null
                ? new BigDecimal(params.get("driverLatitude").toString()) : null;
            BigDecimal driverLongitude = params.get("driverLongitude") != null
                ? new BigDecimal(params.get("driverLongitude").toString()) : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate result;
            if (driverLatitude != null && driverLongitude != null) {
                result = logisticsService.confirmPickup(voucherNo, authCode, driverLatitude, driverLongitude);
            } else {
                result = logisticsService.confirmPickup(voucherNo, authCode);
            }

            logger.info("仓库提货确认成功: voucherNo={}, entId={}", voucherNo, entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("提货确认异常", e);
            return Result.error(500, "提货确认失败");
        }
    }

    // ==================== 到货入库 ====================

    @Operation(summary = "到货入库申请", description = "承运方提交到货入库申请，支持全量入库和增量入库两种方式。actionType=1为全量入库，actionType=2为增量入库（需指定目标仓单ID）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：委派单编号不能为空、无效的到货处理动作"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/arrive")
    public Result<LogisticsDelegate> arrive(
            @Parameter(description = "到货入库信息", required = true) @RequestBody Map<String, Object> params) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            Integer actionType = params.get("actionType") != null
                ? Integer.parseInt(params.get("actionType").toString()) : null;
            Long targetReceiptId = params.get("targetReceiptId") != null
                ? Long.parseLong(params.get("targetReceiptId").toString()) : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }
            if (actionType == null || (actionType != 1 && actionType != 2)) {
                return Result.error(400, "无效的到货处理动作");
            }
            if (actionType == 2 && targetReceiptId == null) {
                return Result.error(400, "增量入库时必须指定目标仓单ID");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单");
            }

            LogisticsDelegate result = logisticsService.arrive(voucherNo, actionType, targetReceiptId);
            logger.info("到货入库申请成功: voucherNo={}, actionType={}, entId={}", voucherNo, actionType, entId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("到货入库异常", e);
            return Result.error(500, "到货入库失败");
        }
    }

    // ==================== 物流追踪 ====================

    @Operation(summary = "物流状态追踪", description = "根据委派单编号追踪物流状态，返回完整的物流信息及最新轨迹。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限查询该委派单"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/track")
    public Result<Map<String, Object>> trackLogistics(
            @Parameter(description = "委派单编号") @RequestParam String voucherNo) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单");
            }

            Map<String, Object> result = logisticsService.trackLogistics(voucherNo);
            return Result.success(result);

        } catch (Exception e) {
            logger.error("物流追踪异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "查询物流轨迹列表", description = "根据委派单编号查询所有物流轨迹记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限查询该委派单的轨迹"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/track/list")
    public Result<List<LogisticsTrack>> listTracks(@Parameter(description = "委派单编号") @RequestParam String voucherNo) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单的轨迹");
            }

            List<LogisticsTrack> tracks = logisticsService.listTracks(voucherNo);
            return Result.success(tracks);

        } catch (Exception e) {
            logger.error("查询轨迹列表异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "获取最新轨迹", description = "根据委派单编号获取最新的物流轨迹记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限查询该委派单的轨迹"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/track/latest")
    public Result<LogisticsTrack> getLatestTrack(@Parameter(description = "委派单编号") @RequestParam String voucherNo) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单的轨迹");
            }

            LogisticsTrack track = logisticsService.getLatestTrack(voucherNo);
            return Result.success(track);

        } catch (Exception e) {
            logger.error("获取最新轨迹异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "查询偏航记录", description = "根据委派单编号查询所有偏航记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限查询该委派单的轨迹"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/track/deviations")
    public Result<List<LogisticsTrack>> listDeviations(@Parameter(description = "委派单编号") @RequestParam String voucherNo) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单的轨迹");
            }

            List<LogisticsTrack> deviations = logisticsService.listDeviations(voucherNo);
            return Result.success(deviations);

        } catch (Exception e) {
            logger.error("查询偏航记录异常", e);
            return Result.error(500, "查询失败");
        }
    }

    // ==================== 轨迹上报 ====================

    @Operation(summary = "上报物流轨迹", description = "承运方上报物流轨迹信息，包括位置、速度、方向等。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上报成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限上报该委派单的轨迹"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/track/report")
    public Result<LogisticsTrack> reportTrack(
            @Parameter(description = "轨迹信息", required = true) @RequestBody LogisticsTrack track) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(track.getVoucherNo());
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限上报该委派单的轨迹");
            }

            LogisticsTrack result = logisticsService.reportTrack(track);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("上报轨迹异常", e);
            return Result.error(500, "上报失败");
        }
    }

    // ==================== 状态更新 ====================

    @Operation(summary = "更新物流状态", description = "更新委派单物流状态。状态值范围1-5：1-已指派、2-已提货、3-运输中、4-已到达、5-已交付。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：委派单编号、状态不能为空，状态值需在1-5范围内"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PutMapping("/status")
    public Result<LogisticsDelegate> updateStatus(@RequestBody Map<String, Object> params) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            Integer status = params.get("status") != null
                ? Integer.parseInt(params.get("status").toString()) : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }
            if (status == null) {
                return Result.error(400, "状态不能为空");
            }
            if (status < 1 || status > 5) {
                return Result.error(400, "无效的状态值");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单");
            }

            LogisticsDelegate result = logisticsService.updateStatus(voucherNo, status);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("更新状态异常", e);
            return Result.error(500, "更新失败");
        }
    }

    @Operation(summary = "确认交付", description = "货主确认货物交付，完成物流委派流程。action参数：1-全量交付，2-部分交付（需指定目标仓单ID）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "交付确认成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "只有货主才能确认交付"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/delivery/confirm")
    public Result<LogisticsDelegate> confirmDelivery(@RequestBody Map<String, Object> params) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;
            Integer action = params.get("action") != null
                ? Integer.parseInt(params.get("action").toString()) : 1;
            String targetReceiptId = params.get("targetReceiptId") != null
                ? params.get("targetReceiptId").toString() : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId())) {
                return Result.error(403, "只有货主才能确认交付");
            }

            LogisticsDelegate result = logisticsService.confirmDelivery(voucherNo, action, targetReceiptId);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认交付异常", e);
            return Result.error(500, "确认交付失败");
        }
    }

    @Operation(summary = "使委派单失效", description = "货主使委派单失效，取消物流委派。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/invalidate")
    public Result<LogisticsDelegate> invalidate(@RequestBody Map<String, Object> params) {
        try {
            String voucherNo = params.get("voucherNo") != null ? params.get("voucherNo").toString() : null;

            if (voucherNo == null || voucherNo.isEmpty()) {
                return Result.error(400, "委派单编号不能为空");
            }

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId())) {
                return Result.error(403, "无权限操作该委派单");
            }

            LogisticsDelegate result = logisticsService.invalidate(voucherNo);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("使委派单失效异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "验证物流委派单", description = "验证委派单编号是否有效。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/validate")
    public Result<Boolean> validateDelegate(@Parameter(description = "委派单编号") @RequestParam String voucherNo) {
        try {
            boolean isValid = logisticsService.validateDelegate(voucherNo);
            return Result.success(isValid);

        } catch (Exception e) {
            logger.error("验证委派单异常", e);
            return Result.error(500, "验证失败");
        }
    }
}
