package com.fisco.app.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

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

import com.fisco.app.util.CurrentUser;
import com.fisco.app.util.PageResult;
import com.fisco.app.util.Result;
import com.fisco.app.feign.EnterpriseFeignClient;
import com.fisco.app.dto.LoanApplyRequest;
import com.fisco.app.dto.LoanApproveRequest;
import com.fisco.app.dto.LoanCalculatorRequest;
import com.fisco.app.dto.RepaymentRequest;
import com.fisco.app.entity.Loan;
import com.fisco.app.entity.LoanInstallment;
import com.fisco.app.entity.LoanRepayment;
import com.fisco.app.service.LoanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 * 质押贷款 Controller
 */
@Tag(name = "质押贷款管理", description = "仓单质押贷款申请、审批、放款、还款等全流程管理")
@RestController
@RequestMapping("/api/v1/finance/loan")
public class LoanController {

    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);

    @Autowired
    private LoanService loanService;

    @Autowired
    private EnterpriseFeignClient enterpriseFeignClient;

    @Operation(summary = "申请质押贷款", description = "企业以仓单作为质押物申请贷款。系统创建贷款申请记录，状态为\"申请中\"。\n\n**业务规则**：\n- 仓单ID不能为空\n- 申请金额必须大于0\n- 贷款期限必须大于0天")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "申请成功", content = @Content(schema = @Schema(implementation = LoanResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/apply")
    public Result<LoanResponse> applyLoan(@Valid @RequestBody LoanApplyRequest request) {
        try {
            if (request.getReceiptId() == null) {
                return Result.error(400, "仓单ID不能为空");
            }
            if (request.getAppliedAmount() == null || request.getAppliedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "申请金额必须大于0");
            }
            if (request.getLoanDays() == null || request.getLoanDays() <= 0) {
                return Result.error(400, "贷款期限必须大于0");
            }

            Loan loan = loanService.applyLoan(request);

            logger.info("质押贷款申请成功: loanNo={}, receiptId={}, amount={}",
                    loan.getLoanNo(), request.getReceiptId(), request.getAppliedAmount());

            return Result.success(convertToLoanResponse(loan));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("质押贷款申请异常", e);
            return Result.error(500, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "贷款列表", description = "分页查询贷款列表，支持按状态、企业、日期等条件筛选。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LoanResponse[].class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/list")
    public Result<PageResult<LoanResponse>> listLoans(
            @Parameter(description = "页码", example = "1") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            @Parameter(description = "借款企业ID") @RequestParam(required = false) Long borrowerEntId,
            @Parameter(description = "金融机构ID") @RequestParam(required = false) Long financeEntId,
            @Parameter(description = "申请起始日期(yyyy-MM-dd)", example = "2024-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "申请截止日期(yyyy-MM-dd)", example = "2024-12-31") @RequestParam(required = false) String endDate) {
        try {
            PageResult<Loan> pageResult = loanService.listLoans(pageNum, pageSize, status,
                    borrowerEntId, financeEntId, startDate, endDate);

            List<LoanResponse> convertedList = pageResult.getList().stream()
                    .map(this::convertToLoanResponse)
                    .collect(Collectors.toList());
            PageResult<LoanResponse> result = new PageResult<>(
                    convertedList,
                    pageResult.getPagination().getTotal(),
                    pageResult.getPagination().getPage(),
                    pageResult.getPagination().getPageSize()
            );
            return Result.success(result);

        } catch (Exception e) {
            logger.error("查询贷款列表异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    @Operation(summary = "贷款详情", description = "根据贷款ID查询详细信息，包括贷款金额、利率、状态、关联企业等。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LoanResponse.class))),
        @ApiResponse(responseCode = "400", description = "贷款ID为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "404", description = "贷款不存在", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/{id}")
    public Result<LoanResponse> getLoanById(@Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id) {
        try {
            if (id == null) {
                return Result.error(400, "贷款ID不能为空");
            }

            Loan loan = loanService.getLoanById(id);
            if (loan == null) {
                return Result.error(404, "贷款不存在");
            }
            return Result.success(convertToLoanResponse(loan));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("查询贷款详情异常", e);
            return Result.error(500, "查询失败，请稍后重试");
        }
    }

    @Operation(summary = "审批通过", description = "金融机构审批通过贷款申请，确定最终放款金额、利率和期限。审批通过后状态变为\"已审批\"。\n\n**前置条件**：当前用户必须为金融机构。\n**业务规则**：\n- 审批金额必须大于0\n- 利率必须大于0\n- 贷款期限必须大于0")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审批成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅金融机构可审批", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{id}/approve")
    public Result<Boolean> approveLoan(
            @Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody LoanApproveRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            // 校验是否为金融机构
            Result<Boolean> fiResult = enterpriseFeignClient.isFinancialInstitution(currentEntId);
            if (fiResult == null || fiResult.getCode() != 0 || fiResult.getData() == null || !fiResult.getData()) {
                return Result.error(403, "仅金融机构可以审批贷款");
            }
            if (request.getApprovedAmount() == null || request.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "审批金额必须大于0");
            }
            if (request.getInterestRate() == null || request.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "利率必须大于0");
            }
            if (request.getLoanDays() == null || request.getLoanDays() <= 0) {
                return Result.error(400, "贷款期限必须大于0");
            }

            boolean success = loanService.approveLoan(id, request);
            return Result.success(success);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("审批贷款异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "审批拒绝", description = "金融机构拒绝贷款申请，记录拒绝原因。拒绝后状态变为\"已拒绝\"。\n\n**前置条件**：当前用户必须为金融机构。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "拒绝成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "400", description = "拒绝原因不能为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅金融机构可拒绝", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{id}/reject")
    public Result<Boolean> rejectLoan(
            @Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id,
            @RequestBody RejectRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            // 校验是否为金融机构
            Result<Boolean> fiResult = enterpriseFeignClient.isFinancialInstitution(currentEntId);
            if (fiResult == null || fiResult.getCode() != 0 || fiResult.getData() == null || !fiResult.getData()) {
                return Result.error(403, "仅金融机构可以拒绝贷款");
            }
            if (request.getReason() == null || request.getReason().isBlank()) {
                return Result.error(400, "拒绝原因不能为空");
            }

            boolean success = loanService.rejectLoan(id, request.getReason());
            return Result.success(success);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("拒绝贷款异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "取消申请", description = "借款企业取消尚未审批的贷款申请，记录取消原因。取消后状态变为\"已取消\"。\n\n**业务规则**：仅在审批前可取消。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancelLoan(
            @Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id,
            @RequestBody CancelRequest request) {
        try {
            boolean success = loanService.cancelLoan(id, request.getReason());
            return Result.success(success);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("取消贷款异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "放款", description = "金融机构对已审批通过的贷款执行放款操作，放款后状态变为\"已放款\"。\n\n**前置条件**：当前用户必须为金融机构。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "放款成功", content = @Content(schema = @Schema(implementation = Boolean.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅金融机构可放款", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{id}/disburse")
    public Result<Boolean> disburseLoan(
            @Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id,
            @RequestBody DisburseRequest request) {
        try {
            Long currentEntId = CurrentUser.getEntId();
            if (currentEntId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            // 校验是否为金融机构
            Result<Boolean> fiResult = enterpriseFeignClient.isFinancialInstitution(currentEntId);
            if (fiResult == null || fiResult.getCode() != 0 || fiResult.getData() == null || !fiResult.getData()) {
                return Result.error(403, "仅金融机构可以放款");
            }
            boolean success = loanService.disburseLoan(id, request.getDisbursementVoucher());
            return Result.success(success);

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("放款异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "提交还款", description = "借款企业提交还款计划或直接还款，支持按期还款和提前还款。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "还款成功", content = @Content(schema = @Schema(implementation = LoanRepaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/{id}/repay")
    public Result<LoanRepaymentResponse> repayLoan(
            @Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody RepaymentRequest request) {
        try {
            LoanRepayment repayment = loanService.repayLoan(id, request);
            return Result.success(convertToLoanRepaymentResponse(repayment));

        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("还款异常", e);
            return Result.error(500, "操作失败");
        }
    }

    @Operation(summary = "还款记录列表", description = "查询指定贷款的所有还款记录，包括本金、利息、罚息等明细。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LoanRepaymentResponse[].class))),
        @ApiResponse(responseCode = "400", description = "贷款ID为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/{id}/repayments")
    public Result<List<LoanRepaymentResponse>> listRepayments(@Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id) {
        try {
            List<LoanRepayment> list = loanService.listRepayments(id);
            return Result.success(list.stream().map(this::convertToLoanRepaymentResponse).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("不存在")) {
                return Result.error(404, e.getMessage());
            }
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("查询还款记录异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "分期计划", description = "查询指定贷款的分期还款计划，包括每期应还本金、利息、到期日期等。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LoanInstallmentResponse[].class))),
        @ApiResponse(responseCode = "400", description = "贷款ID为空", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/{id}/installments")
    public Result<List<LoanInstallmentResponse>> listInstallments(@Parameter(description = "贷款ID", required = true) @PathVariable("id") Long id) {
        try {
            List<LoanInstallment> list = loanService.listInstallments(id);
            return Result.success(list.stream().map(this::convertToInstallmentResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("查询分期计划异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "贷款试算", description = "根据贷款金额、利率、期限等参数计算利息和还款计划，用于贷款前参考。\n\n**业务说明**：试算不产生实际贷款，仅供用户参考还款方案。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "试算成功", content = @Content(schema = @Schema(implementation = LoanCalculatorRequest.LoanCalculatorResult.class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @PostMapping("/calculator")
    public Result<LoanCalculatorRequest.LoanCalculatorResult> calculator(
            @RequestBody LoanCalculatorRequest request) {
        try {
            LoanCalculatorRequest.LoanCalculatorResult result = loanService.calculator(request);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("贷款试算异常", e);
            return Result.error(500, "试算失败");
        }
    }

    @Operation(summary = "我的贷款", description = "查询当前企业作为借款方的所有贷款申请记录，支持状态筛选和分页。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LoanResponse[].class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/my")
    public Result<PageResult<LoanResponse>> myLoans(
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码", example = "1") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            PageResult<Loan> pageResult = loanService.myLoans(entId, status, pageNum, pageSize);
            List<LoanResponse> convertedList = pageResult.getList().stream()
                    .map(this::convertToLoanResponse)
                    .collect(Collectors.toList());
            PageResult<LoanResponse> result = new PageResult<>(
                    convertedList,
                    pageResult.getPagination().getTotal(),
                    pageResult.getPagination().getPage(),
                    pageResult.getPagination().getPageSize()
            );
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询我的贷款异常", e);
            return Result.error(500, "查询失败");
        }
    }

    @Operation(summary = "待审批列表", description = "查询当前金融机构待审批的贷款申请列表，仅返回状态为\"申请中\"的记录。\n\n**前置条件**：当前用户必须为金融机构。")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = LoanResponse[].class))),
        @ApiResponse(responseCode = "400", description = "参数错误", content = @Content),
        @ApiResponse(responseCode = "401", description = "未登录或Token无效", content = @Content),
        @ApiResponse(responseCode = "403", description = "无权限：仅金融机构可查询", content = @Content),
        @ApiResponse(responseCode = "500", description = "服务端异常", content = @Content)
    })
    @GetMapping("/pending")
    public Result<PageResult<LoanResponse>> pendingLoans(
            @Parameter(description = "页码", example = "1") @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            Long entId = CurrentUser.getEntId();
            if (entId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            PageResult<Loan> pageResult = loanService.pendingLoans(entId, pageNum, pageSize);
            List<LoanResponse> convertedList = pageResult.getList().stream()
                    .map(this::convertToLoanResponse)
                    .collect(Collectors.toList());
            PageResult<LoanResponse> result = new PageResult<>(
                    convertedList,
                    pageResult.getPagination().getTotal(),
                    pageResult.getPagination().getPage(),
                    pageResult.getPagination().getPageSize()
            );
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询待审批列表异常", e);
            return Result.error(500, "查询失败");
        }
    }

    private LoanResponse convertToLoanResponse(Loan loan) {
        if (loan == null) return null;
        LoanResponse response = new LoanResponse();
        response.setId(loan.getId());
        response.setLoanNo(loan.getLoanNo());
        response.setBorrowerEntId(loan.getBorrowerEntId());
        response.setBorrowerEntName(loan.getBorrowerEntName());
        response.setFinanceEntId(loan.getFinanceEntId());
        response.setFinanceEntName(loan.getFinanceEntName());
        response.setReceiptId(loan.getReceiptId());
        response.setReceiptNo(loan.getReceiptNo());
        response.setGoodsName(loan.getGoodsName());
        response.setWarehouseName(loan.getWarehouseName());
        response.setAppliedAmount(loan.getAppliedAmount());
        response.setApprovedAmount(loan.getApprovedAmount());
        response.setLoanAmount(loan.getLoanAmount());
        response.setAppliedInterestRate(loan.getAppliedInterestRate());
        response.setApprovedInterestRate(loan.getApprovedInterestRate());
        response.setLoanInterestRate(loan.getLoanInterestRate());
        response.setLoanDays(loan.getLoanDays());
        response.setCollateralValue(loan.getCollateralValue());
        response.setPledgeRate(loan.getPledgeRate());
        response.setMaxLoanAmount(loan.getMaxLoanAmount());
        response.setAppliedTime(loan.getAppliedTime());
        response.setApprovedTime(loan.getApprovedTime());
        response.setDisbursedTime(loan.getDisbursedTime());
        response.setLoanStartDate(loan.getLoanStartDate());
        response.setLoanEndDate(loan.getLoanEndDate());
        response.setRepaidPrincipal(loan.getRepaidPrincipal());
        response.setRepaidInterest(loan.getRepaidInterest());
        response.setRepaidPenalty(loan.getRepaidPenalty());
        response.setOutstandingPrincipal(loan.getOutstandingPrincipal());
        response.setOutstandingInterest(loan.getOutstandingInterest());
        response.setOutstandingPenalty(loan.getOutstandingPenalty());
        response.setStatus(loan.getStatus());
        response.setStatusName(loan.getStatusName());
        response.setReceivableId(loan.getReceivableId());
        response.setApproveRemark(loan.getApproveRemark());
        response.setRejectReason(loan.getRejectReason());
        response.setRejectTime(loan.getRejectTime());
        response.setCancelReason(loan.getCancelReason());
        response.setCancelTime(loan.getCancelTime());
        response.setChainTxHash(loan.getChainTxHash());
        response.setCreateTime(loan.getCreateTime());
        response.setUpdateTime(loan.getUpdateTime());
        return response;
    }

    private LoanRepaymentResponse convertToLoanRepaymentResponse(LoanRepayment repayment) {
        if (repayment == null) return null;
        LoanRepaymentResponse response = new LoanRepaymentResponse();
        response.setId(repayment.getId());
        response.setLoanId(repayment.getLoanId());
        response.setRepaymentNo(repayment.getRepaymentNo());
        response.setPrincipalAmount(repayment.getPrincipalAmount());
        response.setInterestAmount(repayment.getInterestAmount());
        response.setPenaltyAmount(repayment.getPenaltyAmount());
        response.setTotalAmount(repayment.getTotalAmount());
        response.setRepaymentType(repayment.getRepaymentType());
        response.setRepaymentTypeName(repayment.getRepaymentTypeName());
        response.setPaymentVoucher(repayment.getPaymentVoucher());
        response.setPaymentAccount(repayment.getPaymentAccount());
        response.setOffsetReceiptId(repayment.getOffsetReceiptId());
        response.setOffsetReceiptNo(repayment.getOffsetReceiptNo());
        response.setOffsetReceivableId(repayment.getOffsetReceivableId());
        response.setOffsetReceivableNo(repayment.getOffsetReceivableNo());
        response.setDisposalMethod(repayment.getDisposalMethod());
        response.setDisposalAmount(repayment.getDisposalAmount());
        response.setSignatureHash(repayment.getSignatureHash());
        response.setRepaymentTime(repayment.getRepaymentTime());
        response.setStatus(repayment.getStatus());
        response.setStatusName(repayment.getStatusName());
        response.setChainTxHash(repayment.getChainTxHash());
        response.setRemark(repayment.getRemark());
        response.setCreateTime(repayment.getCreateTime());
        return response;
    }

    private LoanInstallmentResponse convertToInstallmentResponse(LoanInstallment installment) {
        if (installment == null) return null;
        LoanInstallmentResponse response = new LoanInstallmentResponse();
        response.setId(installment.getId());
        response.setLoanId(installment.getLoanId());
        response.setInstallmentNo(installment.getInstallmentNo());
        response.setDueDate(installment.getDueDate());
        response.setPrincipalAmount(installment.getPrincipalAmount());
        response.setInterestAmount(installment.getInterestAmount());
        response.setTotalAmount(installment.getTotalAmount());
        response.setRepaidAmount(installment.getRepaidAmount());
        response.setStatus(installment.getStatus());
        response.setStatusName(installment.getStatusName());
        response.setCreateTime(installment.getCreateTime());
        response.setUpdateTime(installment.getUpdateTime());
        return response;
    }

    @Data
    @Schema(description = "拒绝贷款请求")
    public static class RejectRequest {
        @Schema(description = "拒绝原因", example = "资料不全")
        private String reason;
    }

    @Data
    @Schema(description = "取消贷款请求")
    public static class CancelRequest {
        @Schema(description = "取消原因", example = "不再需要贷款")
        private String reason;
    }

    @Data
    @Schema(description = "放款请求")
    public static class DisburseRequest {
        @Schema(description = "放款凭证（图片URL或Base64）", example = "http://example.com/disburse.jpg")
        private String disbursementVoucher;
    }

    @Data
    @Schema(description = "贷款响应")
    public static class LoanResponse {
        @Schema(description = "贷款ID", example = "1")
        private Long id;
        @Schema(description = "贷款编号", example = "LN1234567890")
        private String loanNo;
        @Schema(description = "借款企业ID", example = "1")
        private Long borrowerEntId;
        @Schema(description = "借款企业名称", example = "某供应链公司")
        private String borrowerEntName;
        @Schema(description = "金融机构ID", example = "2")
        private Long financeEntId;
        @Schema(description = "金融机构名称", example = "某金融机构")
        private String financeEntName;
        @Schema(description = "仓单ID", example = "1")
        private Long receiptId;
        @Schema(description = "仓单编号", example = "WR1234567890")
        private String receiptNo;
        @Schema(description = "货物名称", example = "铁矿石")
        private String goodsName;
        @Schema(description = "仓库名称", example = "上海某仓库")
        private String warehouseName;
        @Schema(description = "申请金额", example = "100000.00")
        private BigDecimal appliedAmount;
        @Schema(description = "审批金额", example = "95000.00")
        private BigDecimal approvedAmount;
        @Schema(description = "实际放款金额", example = "95000.00")
        private BigDecimal loanAmount;
        @Schema(description = "申请利率", example = "0.05")
        private BigDecimal appliedInterestRate;
        @Schema(description = "审批利率", example = "0.048")
        private BigDecimal approvedInterestRate;
        @Schema(description = "实际贷款利率", example = "0.048")
        private BigDecimal loanInterestRate;
        @Schema(description = "贷款期限（天）", example = "90")
        private Integer loanDays;
        @Schema(description = "质押物价值", example = "120000.00")
        private BigDecimal collateralValue;
        @Schema(description = "质押率", example = "0.8")
        private BigDecimal pledgeRate;
        @Schema(description = "最大可贷金额", example = "96000.00")
        private BigDecimal maxLoanAmount;
        @Schema(description = "申请时间", example = "2026-03-01T10:00:00")
        private java.time.LocalDateTime appliedTime;
        @Schema(description = "审批时间", example = "2026-03-02T14:30:00")
        private java.time.LocalDateTime approvedTime;
        @Schema(description = "放款时间", example = "2026-03-03T09:00:00")
        private java.time.LocalDateTime disbursedTime;
        @Schema(description = "贷款开始日期", example = "2026-03-03")
        private java.time.LocalDate loanStartDate;
        @Schema(description = "贷款结束日期", example = "2026-06-01")
        private java.time.LocalDate loanEndDate;
        @Schema(description = "已还本金", example = "50000.00")
        private BigDecimal repaidPrincipal;
        @Schema(description = "已还利息", example = "1200.00")
        private BigDecimal repaidInterest;
        @Schema(description = "已还罚息", example = "0.00")
        private BigDecimal repaidPenalty;
        @Schema(description = "待还本金", example = "45000.00")
        private BigDecimal outstandingPrincipal;
        @Schema(description = "待还利息", example = "1080.00")
        private BigDecimal outstandingInterest;
        @Schema(description = "待还罚息", example = "0.00")
        private BigDecimal outstandingPenalty;
        @Schema(description = "状态", example = "6", allowableValues = {"1", "2", "3", "4", "5", "6", "7", "8", "9"})
        private Integer status;
        @Schema(description = "状态名称", example = "还款中")
        private String statusName;
        @Schema(description = "应收款ID（融资时）", example = "1")
        private Long receivableId;
        @Schema(description = "审批备注", example = "批准放款")
        private String approveRemark;
        @Schema(description = "拒绝原因", example = "材料不全")
        private String rejectReason;
        @Schema(description = "拒绝时间", example = "2026-03-02T15:00:00")
        private java.time.LocalDateTime rejectTime;
        @Schema(description = "取消原因", example = "企业主动取消")
        private String cancelReason;
        @Schema(description = "取消时间", example = "2026-03-02T16:00:00")
        private java.time.LocalDateTime cancelTime;
        @Schema(description = "区块链交易哈希", example = "0xabc123...")
        private String chainTxHash;
        @Schema(description = "创建时间", example = "2026-03-01T10:00:00")
        private java.time.LocalDateTime createTime;
        @Schema(description = "更新时间", example = "2026-03-15T14:30:00")
        private java.time.LocalDateTime updateTime;
    }

    @Data
    @Schema(description = "还款记录响应")
    public static class LoanRepaymentResponse {
        @Schema(description = "还款记录ID", example = "1")
        private Long id;
        @Schema(description = "贷款ID", example = "1")
        private Long loanId;
        @Schema(description = "还款编号", example = "REP1234567890")
        private String repaymentNo;
        @Schema(description = "本金金额", example = "45000.00")
        private BigDecimal principalAmount;
        @Schema(description = "利息金额", example = "1080.00")
        private BigDecimal interestAmount;
        @Schema(description = "罚息金额", example = "0.00")
        private BigDecimal penaltyAmount;
        @Schema(description = "总金额", example = "46080.00")
        private BigDecimal totalAmount;
        @Schema(description = "还款类型", example = "CASH", allowableValues = {"CASH", "OFFSET"})
        private String repaymentType;
        @Schema(description = "还款类型名称", example = "现金还款")
        private String repaymentTypeName;
        @Schema(description = "支付凭证", example = "voucher123.png")
        private String paymentVoucher;
        @Schema(description = "支付账户", example = "6217***1234")
        private String paymentAccount;
        @Schema(description = "抵债仓单ID", example = "1")
        private Long offsetReceiptId;
        @Schema(description = "抵债仓单编号", example = "WR1234567890")
        private String offsetReceiptNo;
        @Schema(description = "抵债应收款ID", example = "1")
        private Long offsetReceivableId;
        @Schema(description = "抵债应收款编号", example = "AR1234567890")
        private String offsetReceivableNo;
        @Schema(description = "处置方式", example = "抵扣本金")
        private String disposalMethod;
        @Schema(description = "处置金额", example = "0.00")
        private BigDecimal disposalAmount;
        @Schema(description = "签名哈希", example = "0xdef456...")
        private String signatureHash;
        @Schema(description = "还款时间", example = "2026-03-15T14:30:00")
        private java.time.LocalDateTime repaymentTime;
        @Schema(description = "状态", example = "2", allowableValues = {"1", "2", "3"})
        private Integer status;
        @Schema(description = "状态名称", example = "已确认")
        private String statusName;
        @Schema(description = "区块链交易哈希", example = "0xghi789...")
        private String chainTxHash;
        @Schema(description = "备注", example = "正常还款")
        private String remark;
        @Schema(description = "创建时间", example = "2026-03-15T14:30:00")
        private java.time.LocalDateTime createTime;
    }

    @Data
    @Schema(description = "分期计划响应")
    public static class LoanInstallmentResponse {
        @Schema(description = "分期ID", example = "1")
        private Long id;
        @Schema(description = "贷款ID", example = "1")
        private Long loanId;
        @Schema(description = "期号", example = "1")
        private Integer installmentNo;
        @Schema(description = "到期日期", example = "2026-04-15")
        private java.time.LocalDate dueDate;
        @Schema(description = "应还本金", example = "15000.00")
        private BigDecimal principalAmount;
        @Schema(description = "应还利息", example = "360.00")
        private BigDecimal interestAmount;
        @Schema(description = "应还总金额", example = "15360.00")
        private BigDecimal totalAmount;
        @Schema(description = "已还金额", example = "15360.00")
        private BigDecimal repaidAmount;
        @Schema(description = "状态", example = "2", allowableValues = {"1", "2", "3"})
        private Integer status;
        @Schema(description = "状态名称", example = "已还")
        private String statusName;
        @Schema(description = "创建时间", example = "2026-03-01T10:00:00")
        private java.time.LocalDateTime createTime;
        @Schema(description = "更新时间", example = "2026-03-15T14:30:00")
        private java.time.LocalDateTime updateTime;
    }
}
