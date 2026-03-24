package com.fisco.app.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.util.CurrentUser;
import com.fisco.app.util.Result;
import com.fisco.app.entity.Receivable;
import com.fisco.app.entity.RepaymentRecord;
import com.fisco.app.service.FinanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 金融管理 Controller
 */
@Tag(name = "金融管理")
@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

    @Autowired
    private FinanceService financeService;

    @Operation(summary = "生成应收款", description = "基于物流单生成应收款记录。系统根据物流单号获取货物数量，结合单价计算应收款初始金额，并同步写入区块链。\n\n**前置条件**：当前用户必须为系统管理员。\n**业务规则**：应收款初始金额 = 单价 × 货物数量（从物流服务获取）。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "生成成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误：物流单ID为空或单价小于等于0", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅系统管理员可操作", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/receivable/generate")
    public Result<ReceivableResponse> generateReceivable(
            @Parameter(description = "生成应收款信息", required = true) @RequestBody GenerateReceivableRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            if (!CurrentUser.isAdmin()) {
                return Result.error(403, "只有系统管理员可以生成应收款");
            }
            if (request.getVoucherId() == null) {
                return Result.error(400, "物流单ID不能为空");
            }
            if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "单价必须大于0");
            }

            Receivable receivable = financeService.generateReceivable(
                    request.getVoucherId(),
                    request.getUnitPrice()
            );

            logger.info("生成应收款成功: receivableNo={}, voucherId={}",
                    receivable.getReceivableNo(), request.getVoucherId());

            return Result.success(convertToReceivableResponse(receivable));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("生成应收款异常", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "确认应收款", description = "债务人确认应收款真实性，确认后状态从\"待确认\"变为\"生效中\"，并同步写入区块链。\n\n**前置条件**：当前用户必须为债务人企业。\n**业务规则**：仅当应收款状态为\"待确认(1)\"时可确认。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "确认成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许确认", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅债务人可确认", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/receivable/confirm")
    public Result<ReceivableResponse> confirmReceivable(
            @Parameter(description = "确认应收款信息", required = true) @RequestBody ConfirmReceivableRequest request) {
        try {
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }

            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }

            if (!currentEntId.equals(receivable.getDebtorEntId())) {
                return Result.error(403, "只有债务人才能确认此应收款");
            }

            Receivable confirmed = financeService.confirmReceivable(
                    request.getReceivableId(),
                    request.getSignature()
            );

            return Result.success(convertToReceivableResponse(confirmed));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("确认应收款异常", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "调整应收款金额", description = "管理员调整应收款金额，支持两种场景：\n1. **物流损耗扣减(adjustType=1)**：因物流损耗需扣减应收款金额，金额必须为负数\n2. **仓单拆分同步(adjustType=2)**：仓单拆分后同步调整应收款金额，金额必须为负数\n\n**前置条件**：当前用户必须为系统管理员。\n**业务规则**：调整后金额不能为负数。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "调整成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或调整后金额为负", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅系统管理员可操作", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PatchMapping("/receivable/adjust")
    public Result<ReceivableResponse> adjustReceivable(
            @Parameter(description = "调整应收款信息", required = true) @RequestBody AdjustReceivableRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            if (!CurrentUser.isAdmin()) {
                return Result.error(403, "只有系统管理员可以调整应收款");
            }
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getAdjustType() == null || (request.getAdjustType() != 1 && request.getAdjustType() != 2)) {
                return Result.error(400, "调整类型必须是1(物流损耗扣减)或2(仓单拆分同步)");
            }
            if (request.getAmount() == null) {
                return Result.error(400, "调整金额不能为空");
            }

            Receivable receivable = financeService.adjustReceivable(
                    request.getReceivableId(),
                    request.getAdjustType(),
                    request.getAmount()
            );

            return Result.success(convertToReceivableResponse(receivable));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("调整应收款异常", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "查询应收款详情", description = "根据应收款ID查询详细信息，包括金额、状态、关联企业等完整信息。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "应收款ID为空", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/receivable/{id}")
    public Result<ReceivableResponse> getReceivableById(
            @Parameter(description = "应收款ID", required = true) @PathVariable Long id) {
        try {
            Receivable receivable = financeService.getReceivableById(id);
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }
            return Result.success(convertToReceivableResponse(receivable));
        } catch (Exception e) {
            logger.error("查询应收款异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "根据编号查询应收款", description = "根据应收款编号（ receivableNo ）查询详细信息，如 AR1234567890 格式。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "应收款编号为空", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/receivable/no/{receivableNo}")
    public Result<ReceivableResponse> getReceivableByNo(
            @Parameter(description = "应收款编号", required = true) @PathVariable String receivableNo) {
        try {
            Receivable receivable = financeService.getReceivableByNo(receivableNo);
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }
            return Result.success(convertToReceivableResponse(receivable));
        } catch (Exception e) {
            logger.error("查询应收款异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "查询债权人的应收款列表", description = "查询当前企业作为债权人（应收款接收方）的所有应收款列表。\n\n**业务说明**：债权人是指应收款的接收方，有权收取相应款项。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ReceivableResponse[].class))),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/receivable/creditor/list")
    public Result<List<ReceivableResponse>> listByCreditor() {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            List<Receivable> list = financeService.listByCreditor(entId);
            return Result.success(list.stream().map(this::convertToReceivableResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("查询异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "查询债务人的应收款列表", description = "查询当前企业作为债务人（应付方）的所有应收款列表。\n\n**业务说明**：债务人是指应付款的支付方，有义务按期支付相应款项。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ReceivableResponse[].class))),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/receivable/debtor/list")
    public Result<List<ReceivableResponse>> listByDebtor() {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            List<Receivable> list = financeService.listByDebtor(entId);
            return Result.success(list.stream().map(this::convertToReceivableResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("查询异常", e);
            return Result.error(500, "查询失败");
        }
    }

    /**
     * 查询指定企业的应收款列表（管理员）
     * 根据roleType查询该企业作为债权人或债务人的所有应收款
     */
    @Operation(summary = "查询指定企业的应收款列表（管理员）", description = "管理员查询指定企业的应收款列表，可按角色类型筛选是该企业的债权人列表还是债务人列表。\n\n**权限**：仅系统管理员可调用。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ReceivableResponse[].class))),
        @ApiResponse(responseCode = "400", description = "企业ID为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅系统管理员可操作", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/receivable/ent/{entId}")
    public Result<List<ReceivableResponse>> listByEnterprise(
            @Parameter(description = "企业ID", required = true) @PathVariable("entId") Long entId,
            @Parameter(description = "角色类型：creditor-债权人, debtor-债务人", example = "creditor") @RequestParam(defaultValue = "creditor") String roleType) {
        try {
            if (entId == null) {
                return Result.error(400, "企业ID不能为空");
            }
            List<Receivable> receivables;
            if ("debtor".equalsIgnoreCase(roleType)) {
                receivables = financeService.listByDebtor(entId);
            } else {
                receivables = financeService.listByCreditor(entId);
            }
            return Result.success(receivables.stream().map(this::convertToReceivableResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("查询企业应收款异常: entId={}, roleType={}", entId, roleType, e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "现金还款", description = "债务人通过现金方式偿还应收款。系统记录还款信息并更新应收款余额，余额为0时状态变为\"已结清\"，否则变为\"部分还款\"。还款信息同步写入区块链。\n\n**业务规则**：\n- 还款金额必须大于0\n- 还款金额不能超过待还余额\n- 支持上传支付凭证")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "还款成功", content = @Content(schema = @Schema(implementation = RepaymentRecordResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或还款金额超限", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/receivable/repayment/cash")
    public Result<RepaymentRecordResponse> cashRepayment(
            @Parameter(description = "还款信息", required = true) @RequestBody CashRepaymentRequest request) {
        try {
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "还款金额必须大于0");
            }

            RepaymentRecord record = financeService.cashRepayment(
                    request.getReceivableId(),
                    request.getAmount(),
                    request.getPaymentVoucher()
            );

            return Result.success(convertToRepaymentRecordResponse(record));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("现金还款异常", e);
            return Result.error(500, "还款失败");
        }
    }

    @Operation(summary = "仓单抵债", description = "债务人使用仓单（仓储凭证）抵偿应收款。系统会校验仓单是否属于债务人，抵债信息同步写入区块链。\n\n**业务规则**：\n- 仓单必须属于债务人才能用于抵债\n- 抵债价格必须大于0")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "抵债成功", content = @Content(schema = @Schema(implementation = RepaymentRecordResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或仓单不属于债务人", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅系统管理员可操作", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/receivable/repayment/offset")
    public Result<RepaymentRecordResponse> offsetWithCollateral(
            @Parameter(description = "抵债信息", required = true) @RequestBody OffsetWithCollateralRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            if (!CurrentUser.isAdmin()) {
                return Result.error(403, "只有系统管理员可以执行仓单抵债");
            }
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getReceiptId() == null) {
                return Result.error(400, "仓单ID不能为空");
            }
            if (request.getOffsetPrice() == null || request.getOffsetPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "抵债价格必须大于0");
            }

            RepaymentRecord record = financeService.offsetWithCollateral(
                    request.getReceivableId(),
                    request.getReceiptId(),
                    request.getOffsetPrice(),
                    request.getSignatureHash()
            );

            return Result.success(convertToRepaymentRecordResponse(record));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("仓单抵债异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "查询还款记录", description = "查询指定应收款的所有还款记录，包括现金还款和仓单抵债记录。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = RepaymentRecordResponse[].class))),
        @ApiResponse(responseCode = "400", description = "应收款ID为空", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/receivable/{id}/repayments")
    public Result<List<RepaymentRecordResponse>> listRepayments(
            @Parameter(description = "应收款ID", required = true) @PathVariable Long id) {
        try {
            List<RepaymentRecord> list = financeService.listRepayments(id);
            return Result.success(list.stream().map(this::convertToRepaymentRecordResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("查询还款记录异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "应收款融资", description = "债权人将应收款转让给金融机构获取融资。融资后应收款标记为已融资状态。\n\n**前置条件**：当前用户必须为债权人。\n**业务规则**：仅未融资的应收款可发起融资。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "融资成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误或应收款已融资", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅债权人可发起融资", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/receivable/finance")
    public Result<ReceivableResponse> financeReceivable(
            @Parameter(description = "融资信息", required = true) @RequestBody FinanceReceivableRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            if (request.getReceivableId() == null) {
                return Result.error(400, "应收款ID不能为空");
            }
            if (request.getFinanceAmount() == null || request.getFinanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "融资金额必须大于0");
            }
            if (request.getFinanceEntId() == null) {
                return Result.error(400, "金融机构ID不能为空");
            }

            // 校验当前企业是否为债权人
            Receivable receivable = financeService.getReceivableById(request.getReceivableId());
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }
            if (!currentEntId.equals(receivable.getCreditorEntId())) {
                return Result.error(403, "只有债权人才能发起融资");
            }

            receivable = financeService.financeReceivable(
                    request.getReceivableId(),
                    request.getFinanceAmount(),
                    request.getFinanceEntId()
            );

            return Result.success(convertToReceivableResponse(receivable));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("应收款融资异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "应收款结算", description = "债务人完成应收款最终结算，将余额清零并同步写入区块链。结算后应收款状态变为\"已结清\"。\n\n**前置条件**：当前用户必须为债务人。\n**业务规则**：仅余额为0时可结算。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "结算成功", content = @Content(schema = @Schema(implementation = ReceivableResponse.class))),
        @ApiResponse(responseCode = "400", description = "应收款ID为空或余额不为0", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅债务人可结算", content = @Content),
        @ApiResponse(responseCode = "404", description = "应收款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/receivable/{id}/settle")
    public Result<ReceivableResponse> settleReceivable(
            @Parameter(description = "应收款ID", required = true) @PathVariable Long id) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }

            // 校验当前企业是否为债务人
            Receivable receivable = financeService.getReceivableById(id);
            if (receivable == null) {
                return Result.error(404, "应收款不存在");
            }
            if (!currentEntId.equals(receivable.getDebtorEntId())) {
                return Result.error(403, "只有债务人才能结算应收款");
            }

            receivable = financeService.settleReceivable(id);
            return Result.success(convertToReceivableResponse(receivable));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("应收款结算异常", e);
            return Result.error(500, "操作失败");
        }
    }

    private ReceivableResponse convertToReceivableResponse(Receivable receivable) {
        if (receivable == null) return null;
        ReceivableResponse response = new ReceivableResponse();
        response.setId(receivable.getId());
        response.setReceivableNo(receivable.getReceivableNo());
        response.setBusinessScene(receivable.getBusinessScene());
        response.setSourceVoucherId(receivable.getSourceVoucherId());
        response.setCreditorEntId(receivable.getCreditorEntId());
        response.setDebtorEntId(receivable.getDebtorEntId());
        response.setInitialAmount(receivable.getInitialAmount());
        response.setAdjustedAmount(receivable.getAdjustedAmount());
        response.setCollectedAmount(receivable.getCollectedAmount());
        response.setBalanceUnpaid(receivable.getBalanceUnpaid());
        response.setCurrency(receivable.getCurrency());
        response.setDueDate(receivable.getDueDate());
        response.setStatus(receivable.getStatus());
        response.setStatusName(getStatusName(receivable.getStatus()));
        response.setIsFinanced(receivable.getIsFinanced());
        response.setChainTxHash(receivable.getChainTxHash());
        response.setRemark(receivable.getRemark());
        response.setCreateTime(receivable.getCreateTime());
        response.setUpdateTime(receivable.getUpdateTime());
        return response;
    }

    private RepaymentRecordResponse convertToRepaymentRecordResponse(RepaymentRecord record) {
        if (record == null) return null;
        RepaymentRecordResponse response = new RepaymentRecordResponse();
        response.setId(record.getId());
        response.setReceivableId(record.getReceivableId());
        response.setRepaymentNo(record.getRepaymentNo());
        response.setRepaymentType(record.getRepaymentType());
        response.setAmount(record.getAmount());
        response.setCurrency(record.getCurrency());
        response.setPaymentVoucher(record.getPaymentVoucher());
        response.setReceiptId(record.getReceiptId());
        response.setOffsetPrice(record.getOffsetPrice());
        response.setSignatureHash(record.getSignatureHash());
        response.setRepaymentTime(record.getRepaymentTime());
        response.setChainTxHash(record.getChainTxHash());
        response.setRemark(record.getRemark());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待确认";
            case 2: return "生效中";
            case 3: return "部分还款";
            case 4: return "已结清";
            case 5: return "逾期";
            default: return "未知";
        }
    }

    @lombok.Data
    @Schema(description = "生成应收款请求")
    public static class GenerateReceivableRequest {
        @Schema(description = "物流单ID", example = "1234567890")
        private Long voucherId;
        @Schema(description = "单价（元）", example = "100.00")
        private BigDecimal unitPrice;
    }

    @lombok.Data
    @Schema(description = "确认应收款请求")
    public static class ConfirmReceivableRequest {
        @Schema(description = "应收款ID", example = "1")
        private Long receivableId;
        @Schema(description = "债务人签名（Base64编码）", example = "c2lnbmF0dXJl...")
        private String signature;
    }

    @lombok.Data
    @Schema(description = "调整应收款请求")
    public static class AdjustReceivableRequest {
        @Schema(description = "应收款ID", example = "1")
        private Long receivableId;
        @Schema(description = "调整类型：1-物流损耗扣减, 2-仓单拆分同步", example = "1")
        private Integer adjustType;
        @Schema(description = "调整金额（扣减为负数）", example = "-50.00")
        private BigDecimal amount;
    }

    @lombok.Data
    @Schema(description = "现金还款请求")
    public static class CashRepaymentRequest {
        @Schema(description = "应收款ID", example = "1")
        private Long receivableId;
        @Schema(description = "还款金额", example = "1000.00")
        private BigDecimal amount;
        @Schema(description = "支付凭证（图片URL或Base64）", example = "http://example.com/voucher.jpg")
        private String paymentVoucher;
    }

    @lombok.Data
    @Schema(description = "仓单抵债请求")
    public static class OffsetWithCollateralRequest {
        @Schema(description = "应收款ID", example = "1")
        private Long receivableId;
        @Schema(description = "仓单ID", example = "123")
        private Long receiptId;
        @Schema(description = "抵债价格", example = "5000.00")
        private BigDecimal offsetPrice;
        @Schema(description = "签名哈希（Base64编码）", example = "a2V5a2V5...")
        private String signatureHash;
    }

    @lombok.Data
    @Schema(description = "应收款融资请求")
    public static class FinanceReceivableRequest {
        @Schema(description = "应收款ID", example = "1")
        private Long receivableId;
        @Schema(description = "融资金额", example = "50000.00")
        private BigDecimal financeAmount;
        @Schema(description = "金融机构ID", example = "10")
        private Long financeEntId;
    }

    @lombok.Data
    @Schema(description = "应收款响应")
    public static class ReceivableResponse {
        @Schema(description = "应收款ID")
        private Long id;
        @Schema(description = "应收款编号", example = "AR1234567890")
        private String receivableNo;
        @Schema(description = "业务场景")
        private Integer businessScene;
        @Schema(description = "源物流单ID")
        private Long sourceVoucherId;
        @Schema(description = "债权人企业ID")
        private Long creditorEntId;
        @Schema(description = "债务人企业ID")
        private Long debtorEntId;
        @Schema(description = "初始金额")
        private BigDecimal initialAmount;
        @Schema(description = "调整后金额")
        private BigDecimal adjustedAmount;
        @Schema(description = "已收金额")
        private BigDecimal collectedAmount;
        @Schema(description = "待还余额")
        private BigDecimal balanceUnpaid;
        @Schema(description = "币种", example = "CNY")
        private String currency;
        @Schema(description = "到期日期")
        private java.time.LocalDateTime dueDate;
        @Schema(description = "状态：1-待确认, 2-生效中, 3-部分还款, 4-已结清, 5-逾期")
        private Integer status;
        @Schema(description = "状态名称")
        private String statusName;
        @Schema(description = "是否已融资：0-未融资, 1-已融资")
        private Integer isFinanced;
        @Schema(description = "区块链交易哈希")
        private String chainTxHash;
        @Schema(description = "备注")
        private String remark;
        @Schema(description = "创建时间")
        private java.time.LocalDateTime createTime;
        @Schema(description = "更新时间")
        private java.time.LocalDateTime updateTime;
    }

    @lombok.Data
    @Schema(description = "还款记录响应")
    public static class RepaymentRecordResponse {
        @Schema(description = "记录ID")
        private Long id;
        @Schema(description = "应收款ID")
        private Long receivableId;
        @Schema(description = "还款编号", example = "REP1234567890")
        private String repaymentNo;
        @Schema(description = "还款类型：1-现金, 2-仓单抵债")
        private Integer repaymentType;
        @Schema(description = "还款金额")
        private BigDecimal amount;
        @Schema(description = "币种", example = "CNY")
        private String currency;
        @Schema(description = "支付凭证")
        private String paymentVoucher;
        @Schema(description = "仓单ID（抵债时）")
        private Long receiptId;
        @Schema(description = "抵债价格")
        private BigDecimal offsetPrice;
        @Schema(description = "签名哈希")
        private String signatureHash;
        @Schema(description = "还款时间")
        private java.time.LocalDateTime repaymentTime;
        @Schema(description = "区块链交易哈希")
        private String chainTxHash;
        @Schema(description = "备注")
        private String remark;
        @Schema(description = "创建时间")
        private java.time.LocalDateTime createTime;
    }
}
