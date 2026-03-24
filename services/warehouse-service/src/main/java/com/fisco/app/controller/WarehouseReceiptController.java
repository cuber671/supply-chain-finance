package com.fisco.app.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.entity.ReceiptEndorsement;
import com.fisco.app.entity.ReceiptOperationLog;
import com.fisco.app.entity.StockOrder;
import com.fisco.app.entity.Warehouse;
import com.fisco.app.entity.WarehouseReceipt;
import com.fisco.app.service.WarehouseReceiptService;
import com.fisco.app.service.WarehouseReceiptService.TraceInfo;
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
 * 仓单管理 Controller
 *
 * 提供仓单全生命周期管理的 REST API
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "仓单管理", description = "仓单全生命周期管理：入库、签发、背书转让、拆分合并、质押锁定、核销出库、仓库管理、溯源查询")
@RestController
@RequestMapping("/api/v1/warehouse")
public class WarehouseReceiptController {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseReceiptController.class);

    @Autowired
    private WarehouseReceiptService warehouseReceiptService;

    // ==================== 入库单管理 ====================

    @Operation(summary = "申请入库", description = "企业用户提交入库申请，创建入库单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：仓库ID、货物名称、重量、单位不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/stock-in/apply")
    public Result<Long> applyStockIn(
            @Parameter(description = "入库申请信息", required = true) @RequestBody StockInApplyRequest request) {
        try {
            // 参数校验
            if (request.getWarehouseId() == null) {
                return Result.error(400, "仓库ID不能为空");
            }
            if (request.getGoodsName() == null || request.getGoodsName().isEmpty()) {
                return Result.error(400, "货物名称不能为空");
            }
            if (request.getWeight() == null) {
                return Result.error(400, "货物重量不能为空");
            }
            if (request.getUnit() == null || request.getUnit().isEmpty()) {
                return Result.error(400, "计量单位不能为空");
            }

            // 仅从JWT获取用户信息，防止越权
            Long entId = CurrentUser.getEntId();
            Long userId = CurrentUser.getUserId();

            if (entId == null || userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            Long stockOrderId = warehouseReceiptService.applyStockIn(
                    request.getWarehouseId(),
                    entId,
                    userId,
                    request.getGoodsName(),
                    request.getWeight(),
                    request.getUnit(),
                    request.getAttachmentUrl()
            );
            return Result.success(stockOrderId);
        } catch (Exception e) {
            logger.error("申请入库失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "确认入库单（仓储方操作）", description = "仓储方确认入库单，完成货物入库流程。仅仓库所属企业可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作：仅仓储方可以确认入库单"),
        @ApiResponse(responseCode = "404", description = "入库单或仓库不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/stock-in/{stockOrderId}/confirm")
    public Result<Boolean> confirmStockOrder(
            @Parameter(description = "入库单ID", required = true) @PathVariable String stockOrderId) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            Long id = parseId(stockOrderId, "入库单ID");
            StockOrder order = warehouseReceiptService.getStockOrderById(id);
            if (order == null) {
                return Result.error(404, "入库单不存在");
            }
            // FIX: 校验仓储方身份 - 用户所属企业必须是仓库所属企业
            Warehouse warehouse = warehouseReceiptService.getWarehouseById(order.getWarehouseId());
            if (warehouse == null) {
                return Result.error(404, "仓库不存在");
            }
            if (!warehouse.getEntId().equals(entId)) {
                return Result.error(403, "无权限操作：仅仓储方可以确认入库单");
            }

            boolean success = warehouseReceiptService.confirmStockOrder(order.getId());
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认入库单失败", e);
            return Result.error(500, "确认入库单失败");
        }
    }

    @Operation(summary = "取消入库单", description = "仓储方取消入库申请。仅仓库所属企业可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作：仅仓储方可以取消入库单"),
        @ApiResponse(responseCode = "404", description = "入库单或仓库不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/stock-in/{stockOrderId}/cancel")
    public Result<Boolean> cancelStockOrder(
            @Parameter(description = "入库单ID", required = true) @PathVariable String stockOrderId) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            Long id = parseId(stockOrderId, "入库单ID");
            StockOrder order = warehouseReceiptService.getStockOrderById(id);
            if (order == null) {
                return Result.error(404, "入库单不存在");
            }
            // FIX: 校验仓储方身份 - 用户所属企业必须是仓库所属企业
            Warehouse warehouse = warehouseReceiptService.getWarehouseById(order.getWarehouseId());
            if (warehouse == null) {
                return Result.error(404, "仓库不存在");
            }
            if (!warehouse.getEntId().equals(entId)) {
                return Result.error(403, "无权限操作：仅仓储方可以取消入库单");
            }

            boolean success = warehouseReceiptService.cancelStockOrder(order.getId());
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("取消入库单失败", e);
            return Result.error(500, "取消入库单失败");
        }
    }

    @Operation(summary = "查询入库单", description = "根据入库单ID或入库单号查询入库单详情，支持数字ID和字符串单号两种查询方式。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数格式错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "404", description = "入库单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/stock-in/{stockOrderIdOrNo}")
    public Result<StockOrder> getStockOrderById(
            @Parameter(description = "入库单ID或入库单号", required = true) @PathVariable String stockOrderIdOrNo) {
        try {
            StockOrder stockOrder;
            // FIX: 支持 ID（数字）或 stockNo（字符串）两种查询模式
            if (stockOrderIdOrNo.matches("^\\d+$")) {
                // 纯数字，按 ID 查询
                Long id = Long.parseLong(stockOrderIdOrNo);
                stockOrder = warehouseReceiptService.getStockOrderById(id);
            } else {
                // 非纯数字，按 stockNo 查询
                stockOrder = warehouseReceiptService.getStockOrderByStockNo(stockOrderIdOrNo);
            }
            if (stockOrder == null) {
                return Result.error(404, "入库单不存在");
            }
            return Result.success(stockOrder);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询企业入库单列表", description = "查询当前企业用户的所有入库单列表（不分页）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/stock-in/list")
    public Result<List<StockOrder>> getStockOrdersByEntId() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<StockOrder> list = warehouseReceiptService.getStockOrdersByEntId(entId);
        return Result.success(list);
    }

    @Operation(summary = "分页查询企业入库单列表", description = "分页查询当前企业用户的所有入库单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：页码和每页数量必填"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/stock-in/list/paginated")
    public Result<IPage<StockOrder>> getStockOrdersByEntIdPaginated(
            @Parameter(description = "页码", required = true) @RequestParam int pageNum,
            @Parameter(description = "每页数量", required = true) @RequestParam int pageSize) {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        IPage<StockOrder> page = warehouseReceiptService.getStockOrdersByEntIdPaginated(entId, pageNum, pageSize);
        return Result.success(page);
    }

    // ==================== 仓单签发 ====================

    @Operation(summary = "签发仓单（仓储方操作）", description = "仓储方根据已确认的入库单签发仓单，仓单上链后生成链上ID。仅仓库所属企业可操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "签发成功，返回仓单ID"),
        @ApiResponse(responseCode = "400", description = "参数错误：入库单ID不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作：仅仓储方可签发仓单"),
        @ApiResponse(responseCode = "404", description = "入库单或仓库不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/mint")
    public Result<Long> mintReceipt(
            @Parameter(description = "签发仓单信息", required = true) @RequestBody MintReceiptRequest request) {
        try {
            // 参数校验
            if (request.getStockOrderId() == null) {
                return Result.error(400, "入库单ID不能为空");
            }

            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            Long entId = CurrentUser.getEntId();

            if (userId == null || entId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            // FIX: 校验仓储方身份 - 用户所属企业必须是入库单对应仓库的企业
            StockOrder stockOrder = warehouseReceiptService.getStockOrderById(request.getStockOrderId());
            if (stockOrder == null) {
                return Result.error(404, "入库单不存在");
            }
            Warehouse warehouse = warehouseReceiptService.getWarehouseById(stockOrder.getWarehouseId());
            if (warehouse == null) {
                return Result.error(404, "仓库不存在");
            }
            if (!warehouse.getEntId().equals(entId)) {
                return Result.error(403, "无权限操作：仅仓储方可签发仓单");
            }

            Long receiptId = warehouseReceiptService.mintReceipt(
                    request.getStockOrderId(),
                    userId,
                    request.getOnChainId()
            );
            return Result.success(receiptId);
        } catch (Exception e) {
            logger.error("签发仓单失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    // ==================== 仓单查询 ====================

    @Operation(summary = "根据ID查询仓单", description = "根据仓单ID查询仓单详情。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数格式错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/{receiptId}")
    public Result<WarehouseReceipt> getReceiptById(
            @Parameter(description = "仓单ID", required = true) @PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(id);
            return Result.success(receipt);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "根据链上ID查询仓单", description = "根据区块链上的仓单ID查询仓单详情。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/by-chain/{onChainId}")
    public Result<WarehouseReceipt> getReceiptByOnChainId(
            @Parameter(description = "链上ID", required = true) @PathVariable String onChainId) {
        WarehouseReceipt receipt = warehouseReceiptService.getReceiptByOnChainId(onChainId);
        return Result.success(receipt);
    }

    @Operation(summary = "查询企业仓单列表", description = "查询当前企业用户的所有仓单列表（不分页）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/list")
    public Result<List<WarehouseReceipt>> getReceiptsByEntId() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<WarehouseReceipt> list = warehouseReceiptService.getReceiptsByEntId(entId);
        return Result.success(list);
    }

    @Operation(summary = "分页查询企业仓单列表", description = "分页查询当前企业用户的所有仓单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：页码和每页数量必填"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/list/paginated")
    public Result<IPage<WarehouseReceipt>> getReceiptsByEntIdPaginated(
            @Parameter(description = "页码", required = true) @RequestParam int pageNum,
            @Parameter(description = "每页数量", required = true) @RequestParam int pageSize) {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        IPage<WarehouseReceipt> page = warehouseReceiptService.getReceiptsByEntIdPaginated(entId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "查询企业在库仓单", description = "查询当前企业用户的所有在库仓单（未被质押锁定或核销的仓单）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/in-stock")
    public Result<List<WarehouseReceipt>> getInStockReceipts() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<WarehouseReceipt> list = warehouseReceiptService.getInStockReceipts(entId);
        return Result.success(list);
    }

    @Operation(summary = "分页查询企业在库仓单", description = "分页查询当前企业用户的所有在库仓单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：页码和每页数量必填"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/in-stock/paginated")
    public Result<IPage<WarehouseReceipt>> getInStockReceiptsPaginated(
            @Parameter(description = "页码", required = true) @RequestParam int pageNum,
            @Parameter(description = "每页数量", required = true) @RequestParam int pageSize) {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        IPage<WarehouseReceipt> page = warehouseReceiptService.getInStockReceiptsPaginated(entId, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "校验仓单所有权（供金融机构的贷款前业务校验）", description = "校验仓单是否属于当前企业用户，返回仓单详情信息。供金融机构在贷款前业务校验使用。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "校验成功"),
        @ApiResponse(responseCode = "400", description = "参数格式错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/{receiptId}/validate-ownership")
    public Result<Map<String, Object>> validateReceiptOwnership(
            @Parameter(description = "仓单ID", required = true) @PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前用户企业信息，请先登录");
            }
            Map<String, Object> result = warehouseReceiptService.validateReceiptOwnership(id, entId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ==================== 背书转让 ====================

    @Operation(summary = "发起背书转让", description = "仓单所有者企业发起仓单背书转让，将仓单背书给目标企业。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发起成功，返回背书记录ID"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权操作该仓单"),
        @ApiResponse(responseCode = "404", description = "仓单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/endorsement/launch")
    public Result<Long> launchEndorsement(
            @Parameter(description = "背书转让信息", required = true) @RequestBody LaunchEndorsementRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            Long entId = CurrentUser.getEntId();

            if (userId == null || entId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            // FIX: 校验仓单归属 - 仅仓单所有者企业可发起背书
            WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(request.getReceiptId());
            if (receipt == null) {
                return Result.error(404, "仓单不存在");
            }
            if (!receipt.getOwnerEntId().equals(entId)) {
                return Result.error(403, "无权操作该仓单");
            }

            Long endorsementId = warehouseReceiptService.launchEndorsement(
                    request.getReceiptId(),
                    userId,
                    request.getTransfereeEntId(),
                    request.getSignatureHash()
            );
            return Result.success(endorsementId);
        } catch (Exception e) {
            logger.error("发起背书转让失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "确认/拒绝背书转让", description = "被背书目标企业确认或拒绝背书转让申请。接受则仓单所有权转移，拒绝则背书申请撤销。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/endorsement/{endorsementId}/confirm")
    public Result<Boolean> confirmEndorsement(
            @Parameter(description = "背书ID", required = true) @PathVariable String endorsementId,
            @Parameter(description = "是否接受", required = true) @RequestParam Boolean accept) {
        try {
            Long id = parseId(endorsementId, "背书ID");
            // 校验权限：仅被背书目标企业可确认
            warehouseReceiptService.checkEndorsementTargetPermission(id);

            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            boolean success = warehouseReceiptService.confirmEndorsement(id, userId, accept);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认背书转让失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "撤回背书", description = "背书发起方在目标企业确认之前撤回背书转让申请。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤回成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/endorsement/{endorsementId}/revoke")
    public Result<Boolean> revokeEndorsement(
            @Parameter(description = "背书ID", required = true) @PathVariable String endorsementId) {
        try {
            Long id = parseId(endorsementId, "背书ID");
            // 校验权限：仅背书发起方可撤回
            warehouseReceiptService.checkEndorsementInitiatorPermission(id);

            boolean success = warehouseReceiptService.revokeEndorsement(id);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("撤回背书失败", e);
            return Result.error(500, "撤回背书失败");
        }
    }

    @Operation(summary = "查询仓单背书记录", description = "根据仓单ID查询该仓单的所有背书转让记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/endorsement/list")
    public Result<List<ReceiptEndorsement>> getEndorsementsByReceiptId(
            @Parameter(description = "仓单ID", required = true) @RequestParam Long receiptId) {
        List<ReceiptEndorsement> list = warehouseReceiptService.getEndorsementsByReceiptId(receiptId);
        return Result.success(list);
    }

    // ==================== 拆分/合并 ====================

    @Operation(summary = "发起拆分申请", description = "仓单所有者企业发起拆分申请，将一个仓单拆分为多个子仓单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功，返回操作记录ID"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权操作该仓单"),
        @ApiResponse(responseCode = "404", description = "仓单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/split/apply")
    public Result<Long> applySplit(
            @Parameter(description = "拆分申请信息", required = true) @RequestBody ApplySplitRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            Long entId = CurrentUser.getEntId();
            if (userId == null || entId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            // FIX: 校验仓单归属 - 仅仓单所有者企业可发起拆分
            WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(request.getReceiptId());
            if (receipt == null) {
                return Result.error(404, "仓单不存在");
            }
            if (!receipt.getOwnerEntId().equals(entId)) {
                return Result.error(403, "无权操作该仓单");
            }

            Long opLogId = warehouseReceiptService.applySplit(
                    request.getReceiptId(),
                    userId,
                    request.getTargetWeights()
            );
            return Result.success(opLogId);
        } catch (Exception e) {
            logger.error("发起拆分申请失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "发起合并申请", description = "仓单所有者企业发起合并申请，将多个仓单合并为一个新仓单。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功，返回操作记录ID"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权操作该仓单"),
        @ApiResponse(responseCode = "404", description = "仓单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/merge/apply")
    public Result<Long> applyMerge(
            @Parameter(description = "合并申请信息", required = true) @RequestBody ApplyMergeRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            Long entId = CurrentUser.getEntId();
            if (userId == null || entId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            // FIX: 校验仓单归属 - 仅仓单所有者企业可发起合并
            for (Long receiptId : request.getReceiptIds()) {
                WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(receiptId);
                if (receipt == null) {
                    return Result.error(404, "仓单不存在: " + receiptId);
                }
                if (!receipt.getOwnerEntId().equals(entId)) {
                    return Result.error(403, "无权操作该仓单: " + receiptId);
                }
            }

            Long opLogId = warehouseReceiptService.applyMerge(
                    request.getReceiptIds(),
                    userId
            );
            return Result.success(opLogId);
        } catch (Exception e) {
            logger.error("发起合并申请失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "执行/驳回拆分合并（仓储方操作）", description = "仓储方执行或驳回拆分合并申请。执行则链上完成拆分/合并操作，驳回则申请作废。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作：仅仓储方可以执行拆分合并"),
        @ApiResponse(responseCode = "404", description = "操作记录不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/split-merge/{opLogId}/execute")
    public Result<Boolean> executeSplitMerge(
            @Parameter(description = "操作记录ID", required = true) @PathVariable String opLogId,
            @Parameter(description = "是否执行", required = true) @RequestParam Boolean execute) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            Long id = parseId(opLogId, "操作记录ID");
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            // FIX: 校验仓储方身份 - 仅仓单所属仓库的企业可执行拆分合并
            ReceiptOperationLog opLog = warehouseReceiptService.getOperationLogById(id);
            if (opLog == null) {
                return Result.error(404, "操作记录不存在");
            }
            // executeEntId 为执行方企业ID（仓储方），只有仓储方才能执行
            if (!opLog.getExecuteEntId().equals(entId)) {
                return Result.error(403, "无权限操作：仅仓储方可以执行拆分合并");
            }

            boolean success = warehouseReceiptService.executeSplitMerge(id, userId, execute);
            return Result.success(success);
        } catch (Exception e) {
            logger.error("执行拆分合并失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "查询拆分合并记录", description = "根据操作记录ID查询拆分合并操作的详细信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数格式错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/split-merge/{opLogId}")
    public Result<ReceiptOperationLog> getOperationLogById(
            @Parameter(description = "操作记录ID", required = true) @PathVariable String opLogId) {
        try {
            Long id = parseId(opLogId, "操作记录ID");
            ReceiptOperationLog opLog = warehouseReceiptService.getOperationLogById(id);
            return Result.success(opLog);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ==================== 质押/解押 ====================

    @Operation(summary = "质押锁定仓单（金融机构操作）", description = "金融机构对仓单进行质押锁定，作为贷款担保。锁定后仓单无法进行转让、拆分、合并等操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "锁定成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：贷款ID不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/{receiptId}/lock")
    public Result<Boolean> lockReceipt(
            @Parameter(description = "仓单ID", required = true) @PathVariable String receiptId,
            @Parameter(description = "贷款信息", required = true) @RequestBody Map<String, Object> params) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            String loanId = params.get("loanId") != null ? params.get("loanId").toString() : null;
            if (loanId == null || loanId.isEmpty()) {
                return Result.error(400, "贷款ID不能为空");
            }
            boolean success = warehouseReceiptService.lockReceipt(id, loanId);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("质押锁定仓单失败", e);
            return Result.error(500, "质押锁定仓单失败");
        }
    }

    @Operation(summary = "还款解押仓单（金融机构操作）", description = "金融机构确认还款完成后解除仓单质押锁定，恢复仓单自由操作权限。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "解押成功"),
        @ApiResponse(responseCode = "400", description = "参数格式错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/receipt/{receiptId}/unlock")
    public Result<Boolean> unlockReceipt(
            @Parameter(description = "仓单ID", required = true) @PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            boolean success = warehouseReceiptService.unlockReceipt(id);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("还款解押仓单失败", e);
            return Result.error(500, "还款解押仓单失败");
        }
    }

    // ==================== 核销出库 ====================

    @Operation(summary = "申请核销出库", description = "仓单所有者企业申请仓单核销出库，提交出库签名哈希。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功，返回出库单ID"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权操作该仓单"),
        @ApiResponse(responseCode = "404", description = "仓单不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/burn/apply")
    public Result<Long> applyBurn(
            @Parameter(description = "核销出库信息", required = true) @RequestBody ApplyBurnRequest request) {
        try {
            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            Long entId = CurrentUser.getEntId();

            if (userId == null || entId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            // FIX: 校验仓单归属 - 仅仓单所有者企业可申请核销
            WarehouseReceipt receipt = warehouseReceiptService.getReceiptById(request.getReceiptId());
            if (receipt == null) {
                return Result.error(404, "仓单不存在");
            }
            if (!receipt.getOwnerEntId().equals(entId)) {
                return Result.error(403, "无权操作该仓单");
            }

            Long stockOrderId = warehouseReceiptService.applyBurn(
                    request.getReceiptId(),
                    userId,
                    request.getSignatureHash()
            );
            return Result.success(stockOrderId);
        } catch (Exception e) {
            logger.error("申请核销出库失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "确认核销出库（仓储方操作）", description = "仓储方确认核销出库申请，完成仓单核销和货物出库操作。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "403", description = "无权限操作：仅仓储方可以确认核销出库"),
        @ApiResponse(responseCode = "404", description = "入库单或仓库不存在"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/burn/{stockOrderId}/confirm")
    public Result<Boolean> confirmBurn(
            @Parameter(description = "入库单ID", required = true) @PathVariable String stockOrderId) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            Long id = parseId(stockOrderId, "入库单ID");
            StockOrder order = warehouseReceiptService.getStockOrderById(id);
            if (order == null) {
                return Result.error(404, "入库单不存在");
            }
            // FIX: 校验仓储方身份 - 用户所属企业必须是仓库所属企业
            Warehouse warehouse = warehouseReceiptService.getWarehouseById(order.getWarehouseId());
            if (warehouse == null) {
                return Result.error(404, "仓库不存在");
            }
            if (!warehouse.getEntId().equals(entId)) {
                return Result.error(403, "无权限操作：仅仓储方可以确认核销出库");
            }

            // 仅从JWT获取用户信息，防止越权
            Long userId = CurrentUser.getUserId();
            if (userId == null) {
                return Result.error(401, "无法获取当前用户信息，请先登录");
            }

            boolean success = warehouseReceiptService.confirmBurn(id, userId);
            return Result.success(success);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认核销出库失败", e);
            return Result.error(500, "确认核销出库失败");
        }
    }

    // ==================== 仓库管理 ====================

    @Operation(summary = "创建仓库", description = "企业用户创建仓库信息，用于仓储服务。仓库名称和地址为必填项。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功，返回仓库ID"),
        @ApiResponse(responseCode = "400", description = "参数错误：仓库名称、地址不能为空"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping("/warehouse/create")
    public Result<Long> createWarehouse(
            @Parameter(description = "仓库信息", required = true) @RequestBody CreateWarehouseRequest request) {
        try {
            // 参数校验
            if (request.getName() == null || request.getName().isEmpty()) {
                return Result.error(400, "仓库名称不能为空");
            }
            if (request.getAddress() == null || request.getAddress().isEmpty()) {
                return Result.error(400, "仓库地址不能为空");
            }

            // 仅从JWT获取企业信息，防止越权
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "无法获取当前企业信息，请先登录");
            }

            Long warehouseId = warehouseReceiptService.createWarehouse(
                    entId,
                    request.getName(),
                    request.getAddress(),
                    request.getContactUser(),
                    request.getContactPhone()
            );
            return Result.success(warehouseId);
        } catch (Exception e) {
            logger.error("创建仓库失败", e);
            return Result.error(500, e.getMessage());
        }
    }

    @Operation(summary = "查询仓库列表", description = "查询当前企业用户的所有仓库列表（不分页）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/warehouse/list")
    public Result<List<Warehouse>> getWarehousesByEntId() {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        List<Warehouse> list = warehouseReceiptService.getWarehousesByEntId(entId);
        return Result.success(list);
    }

    @Operation(summary = "分页查询仓库列表", description = "分页查询当前企业用户的所有仓库。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误：页码和每页数量必填"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/warehouse/list/paginated")
    public Result<IPage<Warehouse>> getWarehousesByEntIdPaginated(
            @Parameter(description = "页码", required = true) @RequestParam int pageNum,
            @Parameter(description = "每页数量", required = true) @RequestParam int pageSize) {
        Long entId = CurrentUser.getEntId();
        if (entId == null) {
            return Result.error(401, "无法获取当前用户企业信息，请先登录");
        }
        IPage<Warehouse> page = warehouseReceiptService.getWarehousesByEntIdPaginated(entId, pageNum, pageSize);
        return Result.success(page);
    }

    // ==================== 溯源查询 ====================

    @Operation(summary = "全路径溯源查询", description = "查询仓单的全生命周期溯源信息，包括入库、签发、背书、拆分合并、质押锁定等所有操作记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数格式错误"),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效"),
        @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @GetMapping("/receipt/{receiptId}/trace")
    public Result<TraceInfo> traceReceipt(
            @Parameter(description = "仓单ID", required = true) @PathVariable String receiptId) {
        try {
            Long id = parseId(receiptId, "仓单ID");
            TraceInfo traceInfo = warehouseReceiptService.traceReceipt(id);
            return Result.success(traceInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ==================== Request DTOs ====================

    public static class StockInApplyRequest {
        @Schema(description = "仓库ID", example = "1")
        private Long warehouseId;
        @Schema(description = "货物名称", example = "钢材")
        private String goodsName;
        @Schema(description = "货物重量", example = "100.5")
        private BigDecimal weight;
        @Schema(description = "计量单位", example = "吨")
        private String unit;
        @Schema(description = "附件URL（可选）", example = "https://example.com/attachment.jpg")
        private String attachmentUrl;

        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public String getGoodsName() { return goodsName; }
        public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getAttachmentUrl() { return attachmentUrl; }
        public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    }

    public static class MintReceiptRequest {
        @Schema(description = "入库单ID", example = "1")
        private Long stockOrderId;
        @Schema(description = "仓库用户ID（可选，已从JWT自动获取）", example = "1")
        private Long warehouseUserId;
        @Schema(description = "链上ID（可选，上链后由系统返回）", example = "0xabc123...")
        private String onChainId;

        public Long getStockOrderId() { return stockOrderId; }
        public void setStockOrderId(Long stockOrderId) { this.stockOrderId = stockOrderId; }
        public Long getWarehouseUserId() { return warehouseUserId; }
        public void setWarehouseUserId(Long warehouseUserId) { this.warehouseUserId = warehouseUserId; }
        public String getOnChainId() { return onChainId; }
        public void setOnChainId(String onChainId) { this.onChainId = onChainId; }
    }

    public static class LaunchEndorsementRequest {
        @Schema(description = "仓单ID", example = "1")
        private Long receiptId;
        @Schema(description = "背书发起用户ID（可选，已从JWT自动获取）", example = "1")
        private Long transferorUserId;
        @Schema(description = "被背书目标企业ID", example = "2")
        private Long transfereeEntId;
        @Schema(description = "签名哈希", example = "0xdef456...")
        private String signatureHash;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public Long getTransferorUserId() { return transferorUserId; }
        public void setTransferorUserId(Long transferorUserId) { this.transferorUserId = transferorUserId; }
        public Long getTransfereeEntId() { return transfereeEntId; }
        public void setTransfereeEntId(Long transfereeEntId) { this.transfereeEntId = transfereeEntId; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    }

    public static class ApplySplitRequest {
        @Schema(description = "待拆分的仓单ID", example = "1")
        private Long receiptId;
        @Schema(description = "申请人用户ID（可选，已从JWT自动获取）", example = "1")
        private Long applyUserId;
        @Schema(description = "拆分后的目标重量数组", example = "[50.25, 50.25]")
        private BigDecimal[] targetWeights;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public Long getApplyUserId() { return applyUserId; }
        public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
        public BigDecimal[] getTargetWeights() { return targetWeights; }
        public void setTargetWeights(BigDecimal[] targetWeights) { this.targetWeights = targetWeights; }
    }

    public static class ApplyMergeRequest {
        @Schema(description = "待合并的仓单ID列表", example = "[1, 2, 3]")
        private List<Long> receiptIds;
        @Schema(description = "申请人用户ID（可选，已从JWT自动获取）", example = "1")
        private Long applyUserId;

        public List<Long> getReceiptIds() { return receiptIds; }
        public void setReceiptIds(List<Long> receiptIds) { this.receiptIds = receiptIds; }
        public Long getApplyUserId() { return applyUserId; }
        public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
    }

    public static class ApplyBurnRequest {
        @Schema(description = "待核销的仓单ID", example = "1")
        private Long receiptId;
        @Schema(description = "申请人用户ID（可选，已从JWT自动获取）", example = "1")
        private Long applyUserId;
        @Schema(description = "核销出库签名哈希", example = "0xghi789...")
        private String signatureHash;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public Long getApplyUserId() { return applyUserId; }
        public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
        public String getSignatureHash() { return signatureHash; }
        public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    }

    public static class CreateWarehouseRequest {
        @Schema(description = "企业ID（可选，已从JWT自动获取）", example = "1")
        private Long entId;
        @Schema(description = "仓库名称", example = "华东一号仓库")
        private String name;
        @Schema(description = "仓库地址", example = "上海市浦东新区某路123号")
        private String address;
        @Schema(description = "联系人姓名（可选）", example = "张三")
        private String contactUser;
        @Schema(description = "联系电话（可选）", example = "13800138000")
        private String contactPhone;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getContactUser() { return contactUser; }
        public void setContactUser(String contactUser) { this.contactUser = contactUser; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }

    /**
     * 解析字符串ID为Long类型
     */
    private Long parseId(String idStr, String fieldName) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式错误，应为数字: " + idStr);
        }
    }
}
