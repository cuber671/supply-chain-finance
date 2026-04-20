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

import javax.validation.Valid;

import com.fisco.app.dto.ArriveRequest;
import com.fisco.app.dto.AssignDriverRequest;
import com.fisco.app.dto.ConfirmDeliveryRequest;
import com.fisco.app.dto.ConfirmPickupRequest;
import com.fisco.app.dto.InvalidateRequest;
import com.fisco.app.entity.LogisticsDelegate;
import com.fisco.app.entity.LogisticsTrack;
import com.fisco.app.service.LogisticsService;
import com.fisco.app.util.CurrentUser;
import com.fisco.app.util.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    /**
     * 创建物流委派单
     *
     * 三种业务场景（businessScene 必填）：
     * - 场景一(1)：直接移库 - 货主将仓单项下货物从A仓库转移到B仓库，所有权不转移。创建时锁定仓单，交付后解锁。
     * - 场景二(2)：转让后移库 - 仓单背书转让后，受让方安排物流将货物转移到自己控制的仓库。不锁定仓单（仓单已在受让方名下）。
     * - 场景三(3)：发货入库 - 货物从供应商发往仓库，到货后创建新仓单。无仓单关联，到货时创建新仓单。
     *
     * 通用必填参数：businessScene、transportQuantity、unit、carrierEntId
     *
     * 场景一额外必填：receiptId（关联仓单）、sourceWhId（起运地仓库）、targetWhId（目的地仓库）
     * 场景二额外必填：endorseId（关联背书记录）、targetWhId（目的地仓库，必须属于受让方）
     * 场景三额外必填：targetWhId（目的地仓库）
     *
     * @param delegate 委派单信息，其中 ownerEntId 由系统自动设置为当前登录企业，无需传入
     * @return 创建成功的委派单（含系统生成的 voucherNo）
     */
    @Operation(summary = "创建物流委派单",
        description = "货主企业创建物流委派单，支持三种业务场景。\n\n" +
        "**场景一（直接移库）**：货主将仓单项下货物从A仓库转移到B仓库，所有权不转移。创建时锁定仓单，交付后解锁。\n" +
        "  必填：receiptId（关联仓单）、sourceWhId（起运地仓库）、targetWhId（目的地仓库）\n\n" +
        "**场景二（转让后移库）**：仓单背书转让后，受让方安排物流将货物转移到自己控制的仓库。\n" +
        "  必填：endorseId（关联背书记录）、targetWhId（目的地仓库，必须属于受让方）\n\n" +
        "**场景三（发货入库）**：货物从供应商发往仓库，到货后创建新仓单。\n" +
        "  必填：targetWhId（目的地仓库）\n\n" +
        "**通用必填**：businessScene（1/2/3）、transportQuantity、unit、carrierEntId\n" +
        "**自动填充**：ownerEntId（当前登录企业）、voucherNo（系统生成）\n" +
        "**说明**：ownerEntId 由系统从JWT Token中提取，无需传入。管理员账户（entId=null）不可创建委派单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：业务场景无效、必填参数为空、运输数量为负数、承运企业不是物流企业、仓单/背书/仓库不满足场景约束"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "管理员账户无法创建物流委派单"),
        @ApiResponse(responseCode = "500", description = "服务端异常：区块链上链失败、仓单服务不可用等")
    })
    @PostMapping("/create")
    public Result<LogisticsDelegate> createDelegate(
            @Parameter(description = "委派单信息", required = true) @Valid @RequestBody LogisticsDelegate delegate) {
        try {
            if (delegate.getBusinessScene() == null) {
                return Result.error(400, "业务场景不能为空");
            }
            if (delegate.getBusinessScene() < 1 || delegate.getBusinessScene() > 3) {
                return Result.error(400, "无效的业务场景，仅支持1(直接移库)/2(转让后移库)/3(发货入库)");
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

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                // entId=null 可能是管理员（admin login，entId=null，userId!=null）
                // 管理员不是企业身份，无法创建以企业为主体的物流委派单
                Long userId = CurrentUser.getUserId();
                if (userId != null && CurrentUser.isAdmin()) {
                    return Result.error(403, "管理员账户无法创建物流委派单，请使用企业账户登录");
                }
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
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
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

    @Operation(summary = "根据ID查询委派单", description = "根据物流单ID查询委派单信息，解决 FinanceService 传入数字ID的问题。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限查询该委派单"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/delegate/by-id/{id}")
    public Result<LogisticsDelegate> getDelegateById(
            @Parameter(description = "物流单ID", required = true) @PathVariable Long id) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateById(id);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限查询该委派单");
            }

            return Result.success(delegate);

        } catch (Exception e) {
            logger.error("根据ID查询委派单异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "查询企业委派单列表", description = "查询当前企业用户的所有委派单列表，支持按业务场景和状态筛选。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = List.class))),
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

    @Operation(summary = "物流指派任务", description = "承运企业指派司机和车辆到物流委派单。指派后委派单状态从PENDING(1)变为ASSIGNED(2)，可进行提货确认。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "指派成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：必填字段为空或委派单状态不允许指派"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单，只有承运企业才能指派司机"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/assign")
    public Result<LogisticsDelegate> assignDriver(
            @Parameter(description = "指派信息", required = true) @Valid @RequestBody AssignDriverRequest request) {
        try {
            String voucherNo = request.getVoucherNo();
            String driverId = request.getDriverId();
            String driverName = request.getDriverName();
            String vehicleNo = request.getVehicleNo();

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

    @Operation(summary = "仓库提货确认", description = "司机到达仓库后凭授权码确认提货，支持GPS坐标上报。指派后委派单状态从ASSIGNED(2)变为IN_TRANSIT(3)，可进行运输中状态更新。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "提货确认成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：必填字段为空或授权码错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单，只有承运企业才能确认提货"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/pickup")
    public Result<LogisticsDelegate> confirmPickup(
            @Parameter(description = "提货确认信息", required = true) @Valid @RequestBody ConfirmPickupRequest request) {
        try {
            String voucherNo = request.getVoucherNo();
            String authCode = request.getAuthCode();
            BigDecimal driverLatitude = request.getDriverLatitude() != null
                ? BigDecimal.valueOf(request.getDriverLatitude()) : null;
            BigDecimal driverLongitude = request.getDriverLongitude() != null
                ? BigDecimal.valueOf(request.getDriverLongitude()) : null;

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            if (!entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单，只有承运企业才能确认提货");
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

    @Operation(summary = "到货入库申请", description =
        "目标仓库（仓储方）确认货物到达并执行入库操作。" +
        "状态流转：IN_TRANSIT(3) → IN_TRANSIT(3)【保持】，后续由货主调用 /delivery/confirm 确认交付。" + "<br><br>" +
        "<b>权限说明：</b>仅目标仓库所属企业（仓储方）可调用。" + "<br><br>" +
        "<b>仅 voucherNo 必填，其余均可省略，后端自动推断：</b>" + "<br>" +
        "- warehouseId：默认使用委派单的目标仓库" + "<br>" +
        "- arrivedWeight：默认使用委派单的运输数量" + "<br>" +
        "- actionType：根据 arrivedWeight 与 transportQuantity 比较自动判断（全量=1，部分=2）" + "<br><br>" +
        "<b>actionType=1 全量交付：</b>调用区块链arriveAndCreateReceipt创建新仓单，货物完整到达目标仓库。" + "<br>" +
        "<b>actionType=2 部分交付：</b>将到货信息记录到remark（arrive_records），不调用区块链，" +
        "到货信息包含目的仓库ID和到货重量，后续confirmDelivery从arrive_records解析目的仓库并调用splitReceipt完成仓单拆分。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：委派单编号不能为空、无效的到货处理动作、仓库ID或到货重量为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单，仅目标仓库（仓储方）可确认到货入库"),
        @ApiResponse(responseCode = "404", description = "委派单不存在或目标仓库不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/arrive")
    public Result<LogisticsDelegate> arrive(
            @Parameter(description = "到货入库信息", required = true) @Valid @RequestBody ArriveRequest request) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(request.getVoucherNo());
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }

            // 权限校验：仅目标仓库（仓储方）可确认到货入库
            // 需要获取 targetWhId 对应的 warehouseEntId 进行校验
            Map<String, Object> warehouseInfo = logisticsService.getWarehouseById(delegate.getTargetWhId());
            if (warehouseInfo == null || warehouseInfo.get("data") == null) {
                return Result.error(404, "目标仓库不存在");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> whData = (Map<String, Object>) warehouseInfo.get("data");
            Long warehouseEntId = Long.parseLong(whData.get("entId").toString());
            if (!entId.equals(warehouseEntId)) {
                return Result.error(403, "无权限操作该委派单，仅目标仓库（仓储方）可确认到货入库");
            }

            LogisticsDelegate result = logisticsService.arrive(
                request.getVoucherNo(),
                request.getActionType(),
                request.getTargetReceiptId(),
                request.getWarehouseId(),
                request.getArrivedWeight()
            );

            // actionType 为空时自动判断
            if (request.getActionType() == null) {
                logger.info("到货入库申请成功(自动判断actionType): voucherNo={}, actionType={}, entId={}",
                    request.getVoucherNo(),
                    result.getRemark() != null && result.getRemark().contains("arrive_records=") ? 2 : 1,
                    entId);
            } else {
                logger.info("到货入库申请成功: voucherNo={}, actionType={}, entId={}",
                    request.getVoucherNo(), request.getActionType(), entId);
            }
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
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = Map.class))),
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
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = List.class))),
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
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LogisticsTrack.class))),
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
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = List.class))),
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
        @ApiResponse(responseCode = "200", description = "上报成功", content = @Content(schema = @Schema(implementation = LogisticsTrack.class))),
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

    @Operation(summary = "更新物流状态（内部/管理接口）", description = "通用状态更新接口，建议使用 /delivery/confirm（交付确认）或 /invalidate（失效）代替。状态值范围1-5：1-待指派、2-已指派、3-运输中、4-已交付、5-已失效。状态流转：1→2→3→4/5。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
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

    @Operation(summary = "确认交付", description =
        "货主确认货物交付，完成物流委派流程。状态从IN_TRANSIT(3)变为DELIVERED(4)。" + "<br><br>" +
        "<b>action=1 全量交付：</b>调用clearWaitLogistics解锁原仓单，仓单状态从WAIT_LOGISTICS变为IN_STOCK。" + "<br>" +
        "<b>action=2 部分交付：</b>从arrive接口记录的arrive_records解析目的仓库，调用splitReceipt拆分仓单，" +
        "原仓单变为SPLIT_MERGED，创建两个新仓单（目的仓库存放运输量，源仓库存放剩余量）。" + "<br><br>" +
        "注意：action=2时无需指定targetReceiptId，从arrive_records自动解析。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "交付确认成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：必填字段为空或委派单状态不允许交付"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单，仅货主才能确认交付"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/delivery/confirm")
    public Result<LogisticsDelegate> confirmDelivery(
            @Parameter(description = "确认交付信息", required = true) @Valid @RequestBody ConfirmDeliveryRequest request) {
        try {
            String voucherNo = request.getVoucherNo();
            Integer action = request.getAction() != null ? request.getAction() : 1;
            String targetReceiptId = request.getTargetReceiptId();

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            // 权限校验：仅货主可确认交付
            boolean isOwner = entId.equals(delegate.getOwnerEntId());
            if (!isOwner) {
                return Result.error(403, "无权限操作该委派单，仅货主才能确认交付");
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

    @Operation(summary = "使委派单失效", description = "货主或承运企业使委派单失效，取消物流委派。委派单从任意状态(非运输中)变为INVALID(5)，直接移库场景会自动清除仓单的待物流状态。注意：运输中(IN_TRANSIT)的委派单禁止失效。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功", content = @Content(schema = @Schema(implementation = LogisticsDelegate.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：委派单状态为运输中禁止失效"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作该委派单，只有货主或承运企业才能使委派单失效"),
        @ApiResponse(responseCode = "404", description = "委派单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/invalidate")
    public Result<LogisticsDelegate> invalidate(
            @Parameter(description = "失效信息", required = true) @Valid @RequestBody InvalidateRequest request) {
        try {
            String voucherNo = request.getVoucherNo();

            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            LogisticsDelegate delegate = logisticsService.getDelegateByVoucherNo(voucherNo);
            if (delegate == null) {
                return Result.error(404, "委派单不存在");
            }
            if (!entId.equals(delegate.getOwnerEntId()) && !entId.equals(delegate.getCarrierEntId())) {
                return Result.error(403, "无权限操作该委派单，只有货主或承运企业才能使委派单失效");
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
        @ApiResponse(responseCode = "200", description = "验证成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
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
