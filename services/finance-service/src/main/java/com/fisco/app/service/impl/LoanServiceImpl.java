package com.fisco.app.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fisco.app.feign.BlockchainFeignClient;
import com.fisco.app.feign.CreditFeignClient;
import com.fisco.app.feign.EnterpriseFeignClient;
import com.fisco.app.feign.WarehouseFeignClient;
import com.fisco.app.util.CurrentUser;
import com.fisco.app.util.PageResult;
import com.fisco.app.util.Result;
import com.fisco.app.enums.ResultCodeEnum;
import com.fisco.app.dto.LoanApproveRequest;
import com.fisco.app.dto.LoanApplyRequest;
import com.fisco.app.dto.LoanCalculatorRequest;
import com.fisco.app.dto.RepaymentRequest;
import com.fisco.app.entity.Loan;
import com.fisco.app.entity.LoanInstallment;
import com.fisco.app.entity.LoanRepayment;
import com.fisco.app.mapper.LoanInstallmentMapper;
import com.fisco.app.mapper.LoanMapper;
import com.fisco.app.mapper.LoanRepaymentMapper;
import com.fisco.app.service.LoanService;

/**
 * 质押贷款服务实现
 */
@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanServiceImpl.class);

    private static final BigDecimal DEFAULT_PLEDGE_RATE = new BigDecimal("0.7000");
    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.0800");
    private static final BigDecimal DAILY_PENALTY_RATE = new BigDecimal("0.0005");
    private static final int GRACE_PERIOD_DAYS = 7;

    @Autowired
    private LoanMapper loanMapper;

    @Autowired
    private LoanRepaymentMapper loanRepaymentMapper;

    @Autowired
    private LoanInstallmentMapper loanInstallmentMapper;

    @Autowired(required = false)
    private BlockchainFeignClient blockchainFeignClient;

    @Autowired(required = false)
    private WarehouseFeignClient warehouseFeignClient;

    @Autowired(required = false)
    private EnterpriseFeignClient enterpriseFeignClient;

    @Autowired(required = false)
    private CreditFeignClient creditFeignClient;

    @Override
    @Transactional
    public Loan applyLoan(LoanApplyRequest request) {
        if (request.getReceiptId() == null) {
            throw new IllegalArgumentException("仓单ID不能为空");
        }
        if (request.getAppliedAmount() == null || request.getAppliedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("申请金额必须大于0");
        }
        if (request.getLoanDays() == null || request.getLoanDays() <= 0) {
            throw new IllegalArgumentException("贷款期限必须大于0");
        }

        BigDecimal collateralValue = request.getAppliedAmount().multiply(BigDecimal.ONE);
        BigDecimal pledgeRate = DEFAULT_PLEDGE_RATE;
        BigDecimal maxLoanAmount = collateralValue.multiply(pledgeRate);

        String loanNo = "LOAN" + System.currentTimeMillis();

        Long borrowerEntId = CurrentUser.getEntId();

        Loan loan = new Loan();
        loan.setLoanNo(loanNo);
        loan.setBorrowerEntId(borrowerEntId);
        loan.setReceiptId(request.getReceiptId());
        loan.setAppliedAmount(request.getAppliedAmount());
        loan.setAppliedInterestRate(request.getAppliedInterestRate() != null
            ? request.getAppliedInterestRate() : DEFAULT_INTEREST_RATE);
        loan.setLoanDays(request.getLoanDays());
        loan.setCollateralValue(collateralValue);
        loan.setPledgeRate(pledgeRate);
        loan.setMaxLoanAmount(maxLoanAmount);
        loan.setStatus(Loan.STATUS_PENDING);
        loan.setAppliedTime(LocalDateTime.now());
        loan.setRepaidPrincipal(BigDecimal.ZERO);
        loan.setRepaidInterest(BigDecimal.ZERO);
        loan.setRepaidPenalty(BigDecimal.ZERO);
        loan.setOutstandingPenalty(BigDecimal.ZERO);

        loanMapper.insert(loan);

        // M2: 填充仓单详细信息
        if (warehouseFeignClient != null && request.getReceiptId() != null) {
            try {
                Object receiptObj = warehouseFeignClient.getReceiptById(request.getReceiptId());
                if (receiptObj instanceof Map) {
                    Map<?, ?> receipt = (Map<?, ?>) receiptObj;
                    if (receipt.get("receiptNo") != null) {
                        loan.setReceiptNo(receipt.get("receiptNo").toString());
                    }
                    if (receipt.get("goodsName") != null) {
                        loan.setGoodsName(receipt.get("goodsName").toString());
                    }
                    if (receipt.get("warehouseName") != null) {
                        loan.setWarehouseName(receipt.get("warehouseName").toString());
                    }
                    loanMapper.updateById(loan);
                }
            } catch (Exception e) {
                logger.warn("填充仓单信息失败: receiptId={}", request.getReceiptId(), e);
            }
        }

        logger.info("质押贷款申请成功: loanNo={}, borrowerEntId={}, amount={}, receiptId={}",
                loanNo, borrowerEntId, request.getAppliedAmount(), request.getReceiptId());

        // 区块链上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.LoanCreateRequest request2 = new BlockchainFeignClient.LoanCreateRequest();
                request2.setLoanNo(loanNo);
                request2.setBorrowerHash(borrowerEntId != null ? borrowerEntId.toString() : null);
                // financeEntHash: use borrowerEntId as placeholder since finance entity is determined at approval time
                request2.setFinanceEntHash(borrowerEntId != null ? borrowerEntId.toString() : null);
                // interestRate: use default if not specified in request
                request2.setInterestRate(request.getAppliedInterestRate() != null
                    ? request.getAppliedInterestRate().doubleValue() : DEFAULT_INTEREST_RATE.doubleValue());
                request2.setAmount(request.getAppliedAmount().longValue());
                request2.setLoanDays(request.getLoanDays());
                request2.setReceiptId(request.getReceiptId() != null ? request.getReceiptId().toString() : null);
                request2.setPledgeAmount(collateralValue.longValue());
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.createLoan(request2);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "贷款申请区块链失败: loanNo=" + loanNo + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    loan.setChainTxHash(result.getData());
                    loanMapper.updateById(loan);
                }
                logger.info("贷款上链成功: loanNo={}, chainTxHash={}", loanNo, result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("贷款上链失败: loanNo={}", loanNo, e);
                throw new RuntimeException("区块链操作失败，贷款申请创建已回滚", e);
            }
        }

        return loan;
    }

    @Override
    public Loan getLoanById(Long id) {
        if (id == null) {
            return null;
        }
        return loanMapper.selectById(id);
    }

    @Override
    public PageResult<Loan> listLoans(Integer pageNum, Integer pageSize, Integer status,
                                     Long borrowerEntId, Long financeEntId,
                                     String startDate, String endDate) {
        Page<Loan> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);

        LambdaQueryWrapper<Loan> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Loan::getStatus, status);
        }
        if (borrowerEntId != null) {
            wrapper.eq(Loan::getBorrowerEntId, borrowerEntId);
        }
        if (financeEntId != null) {
            wrapper.eq(Loan::getFinanceEntId, financeEntId);
        }

        wrapper.orderByDesc(Loan::getCreateTime);

        Page<Loan> result = loanMapper.selectPage(page, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    @Transactional
    public boolean approveLoan(Long id, LoanApproveRequest request) {
        Loan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new IllegalArgumentException("贷款不存在");
        }

        if (loan.getStatus() != Loan.STATUS_PENDING) {
            throw new IllegalStateException("贷款状态不是待审批");
        }

        if (request.getApprovedAmount() == null || request.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("审批金额必须大于0");
        }
        if (request.getInterestRate() == null || request.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("利率必须大于0");
        }
        if (request.getLoanDays() == null || request.getLoanDays() <= 0) {
            throw new IllegalArgumentException("贷款期限必须大于0");
        }

        // 金融机构身份验证 - 只有金融机构才能审批贷款
        Long currentEntId = CurrentUser.getEntId();
        if (enterpriseFeignClient != null && currentEntId != null) {
            Result<Boolean> isFiResult = enterpriseFeignClient.isFinancialInstitution(currentEntId);
            if (isFiResult == null || !ResultCodeEnum.SUCCESS.getCode().equals(isFiResult.getCode()) || !Boolean.TRUE.equals(isFiResult.getData())) {
                throw new IllegalStateException("只有金融机构才能执行贷款审批操作");
            }
        }

        // 信用额度校验 - 检查借款企业是否有足够的信用额度
        if (creditFeignClient != null && loan.getBorrowerEntId() != null) {
            try {
                Result<Object> availableLimitResult = creditFeignClient.getAvailableCreditLimit(loan.getBorrowerEntId());
                if (availableLimitResult != null && ResultCodeEnum.SUCCESS.getCode().equals(availableLimitResult.getCode())
                        && availableLimitResult.getData() != null) {
                    BigDecimal availableLimit = new BigDecimal(availableLimitResult.getData().toString());
                    if (request.getApprovedAmount().compareTo(availableLimit) > 0) {
                        throw new IllegalStateException("贷款金额超过企业可用信用额度: 申请金额=" + request.getApprovedAmount()
                                + ", 可用额度=" + availableLimit);
                    }
                }
            } catch (IllegalStateException e) {
                throw e; // 直接抛出业务异常
            } catch (Exception e) {
                logger.warn("信用额度查询失败，跳过额度校验: loanId={}, entId={}, error={}",
                        id, loan.getBorrowerEntId(), e.getMessage());
                // 额度查询失败时不影响审批流程，但记录警告
            }
        }

        loan.setApprovedAmount(request.getApprovedAmount());
        loan.setApprovedInterestRate(request.getInterestRate());
        loan.setLoanDays(request.getLoanDays());
        loan.setLoanInterestRate(request.getInterestRate());
        loan.setStatus(Loan.STATUS_PENDING_DISBURSE);
        loan.setApprovedTime(LocalDateTime.now());
        loan.setApproveRemark(request.getRemark());
        // H4: 记录审批机构
        loan.setFinanceEntId(currentEntId);

        BigDecimal totalInterest = calculateTotalInterest(request.getApprovedAmount(), request.getInterestRate(), request.getLoanDays());
        loan.setOutstandingInterest(totalInterest);
        loan.setOutstandingPrincipal(request.getApprovedAmount());

        loanMapper.updateById(loan);

        logger.info("审批通过: loanNo={}, approvedAmount={}, interestRate={}, loanDays={}",
                loan.getLoanNo(), request.getApprovedAmount(), request.getInterestRate(), request.getLoanDays());

        // 区块链上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.LoanApproveRequest request2 = new BlockchainFeignClient.LoanApproveRequest();
                request2.setLoanNo(loan.getLoanNo());
                request2.setApprovedAmount(request.getApprovedAmount().longValue());
                request2.setInterestRate(request.getInterestRate().doubleValue());
                request2.setLoanDays(request.getLoanDays());
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.approveLoan(request2);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "贷款审批区块链失败: loanNo=" + loan.getLoanNo() + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    loan.setChainTxHash(result.getData());
                    loanMapper.updateById(loan);
                }
                logger.info("贷款审批上链成功: loanNo={}, chainTxHash={}", loan.getLoanNo(), result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("贷款审批上链失败: loanNo={}", loan.getLoanNo(), e);
                throw new RuntimeException("区块链操作失败，贷款审批已回滚", e);
            }
        }

        return true;
    }

    @Override
    @Transactional
    public boolean rejectLoan(Long id, String reason) {
        Loan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new IllegalArgumentException("贷款不存在");
        }

        if (loan.getStatus() != Loan.STATUS_PENDING) {
            throw new IllegalStateException("贷款状态不是待审批");
        }

        // 金融机构身份验证 - 只有金融机构才能拒绝贷款
        Long currentEntId = CurrentUser.getEntId();
        if (enterpriseFeignClient != null && currentEntId != null) {
            Result<Boolean> isFiResult = enterpriseFeignClient.isFinancialInstitution(currentEntId);
            if (isFiResult == null || !ResultCodeEnum.SUCCESS.getCode().equals(isFiResult.getCode()) || !Boolean.TRUE.equals(isFiResult.getData())) {
                throw new IllegalStateException("只有金融机构才能执行贷款拒绝操作");
            }
        }

        loan.setStatus(Loan.STATUS_REJECTED);
        loan.setRejectReason(reason);
        loan.setRejectTime(LocalDateTime.now());

        loanMapper.updateById(loan);

        logger.info("审批拒绝: loanNo={}, reason={}", loan.getLoanNo(), reason);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelLoan(Long id, String reason) {
        Loan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new IllegalArgumentException("贷款不存在");
        }

        if (loan.getStatus() != Loan.STATUS_PENDING && loan.getStatus() != Loan.STATUS_PENDING_DISBURSE
            && loan.getStatus() != Loan.STATUS_DISBURSED) {
            throw new IllegalStateException("贷款状态不支持取消");
        }

        // H3: 取消已放款贷款时解锁仓单
        if (loan.getReceiptId() != null && loan.getStatus() == Loan.STATUS_DISBURSED) {
            if (warehouseFeignClient != null) {
                Map<String, Object> unlockResult = warehouseFeignClient.unlockReceipt(loan.getReceiptId());
                // 修复: 仓单解锁成功码应为0（与lockReceipt一致），而非200
                if (!Integer.valueOf(0).equals(unlockResult.get("code"))) {
                    throw new RuntimeException("仓单解锁失败: " + unlockResult.get("msg"));
                }
            }
        }

        loan.setStatus(Loan.STATUS_CANCELLED);
        loan.setCancelReason(reason);
        loan.setCancelTime(LocalDateTime.now());

        loanMapper.updateById(loan);

        // 区块链上链: 取消贷款需要记录到区块链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.LoanCancelRequest request = new BlockchainFeignClient.LoanCancelRequest();
                request.setLoanNo(loan.getLoanNo());
                request.setReason(reason);
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.cancelLoan(request);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "贷款取消区块链失败: loanNo=" + loan.getLoanNo() + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    loan.setChainTxHash(result.getData());
                    loanMapper.updateById(loan);
                }
                logger.info("贷款取消上链成功: loanNo={}, chainTxHash={}", loan.getLoanNo(), result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("贷款取消上链失败: loanNo={}", loan.getLoanNo(), e);
                throw new RuntimeException("区块链操作失败，贷款取消已回滚", e);
            }
        }

        logger.info("取消贷款: loanNo={}, reason={}", loan.getLoanNo(), reason);

        return true;
    }

    @Override
    @Transactional
    public boolean disburseLoan(Long id, String disbursementVoucher) {
        Loan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new IllegalArgumentException("贷款不存在");
        }

        if (loan.getStatus() != Loan.STATUS_PENDING_DISBURSE) {
            throw new IllegalStateException("贷款状态不是待放款");
        }

        // 金融机构身份验证 - 只有金融机构才能放款
        Long currentEntId = CurrentUser.getEntId();
        if (enterpriseFeignClient != null && currentEntId != null) {
            Result<Boolean> isFiResult = enterpriseFeignClient.isFinancialInstitution(currentEntId);
            if (isFiResult == null || !ResultCodeEnum.SUCCESS.getCode().equals(isFiResult.getCode()) || !Boolean.TRUE.equals(isFiResult.getData())) {
                throw new IllegalStateException("只有金融机构才能执行放款操作");
            }
        }

        // H2: 放款前锁定仓单
        if (loan.getReceiptId() != null) {
            if (warehouseFeignClient == null) {
                throw new IllegalStateException("仓单服务不可用，无法放款");
            }
            Map<String, String> lockParams = Map.of(
                "loanId", loan.getLoanNo(),
                "amount", loan.getLoanAmount() != null ? loan.getLoanAmount().toString() : "0"
            );
            Map<String, Object> lockResult = warehouseFeignClient.lockReceipt(loan.getReceiptId(), lockParams);
            if (!Integer.valueOf(0).equals(lockResult.get("code"))) {
                throw new RuntimeException("仓单锁定失败: " + lockResult.get("msg"));
            }
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(loan.getLoanDays());

        loan.setLoanAmount(loan.getApprovedAmount());
        loan.setStatus(Loan.STATUS_DISBURSED);
        loan.setDisbursedTime(LocalDateTime.now());
        loan.setLoanStartDate(startDate);
        loan.setLoanEndDate(endDate);
        loan.setDisbursementVoucher(disbursementVoucher);

        loanMapper.updateById(loan);

        logger.info("放款成功: loanNo={}, loanAmount={}, disbursementVoucher={}",
                loan.getLoanNo(), loan.getLoanAmount(), disbursementVoucher);

        // 区块链上链
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.LoanDisburseRequest request2 = new BlockchainFeignClient.LoanDisburseRequest();
                request2.setLoanNo(loan.getLoanNo());
                request2.setReceiptId(loan.getReceiptId() != null ? loan.getReceiptId().toString() : null);
                // 【新问题修复】检查响应码确保区块链调用真正成功
                Result<String> result = blockchainFeignClient.disburseLoan(request2);
                if (result == null || result.getCode() != 0) {
                    String errMsg = "贷款放款区块链失败: loanNo=" + loan.getLoanNo() + ", result=" + result;
                    logger.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
                if (result.getData() != null) {
                    loan.setChainTxHash(result.getData());
                    loanMapper.updateById(loan);
                }
                logger.info("贷款放款上链成功: loanNo={}, chainTxHash={}", loan.getLoanNo(), result.getData());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.error("贷款放款上链失败: loanNo={}", loan.getLoanNo(), e);
                throw new RuntimeException("区块链操作失败，贷款放款已回滚", e);
            }
        }

        return true;
    }

    @Override
    @Transactional
    public LoanRepayment repayLoan(Long id, RepaymentRequest request) {
        Loan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new IllegalArgumentException("贷款不存在");
        }

        if (loan.getStatus() != Loan.STATUS_DISBURSED &&
            loan.getStatus() != Loan.STATUS_REPAYING &&
            loan.getStatus() != Loan.STATUS_OVERDUE) {
            throw new IllegalStateException("贷款状态不支持还款");
        }

        // H8: 还款金额服务端校验
        BigDecimal outstandingPrincipal = loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : BigDecimal.ZERO;
        BigDecimal outstandingInterest = loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO;
        if (request.getPrincipalAmount().compareTo(outstandingPrincipal) > 0) {
            throw new IllegalArgumentException("还款本金不能超过未还本金: " + outstandingPrincipal);
        }
        if (request.getInterestAmount().compareTo(outstandingInterest) > 0) {
            throw new IllegalArgumentException("还款利息不能超过未还利息: " + outstandingInterest);
        }

        String repaymentNo = "REPLOAN" + System.currentTimeMillis();

        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoanId(id);
        repayment.setRepaymentNo(repaymentNo);
        repayment.setPrincipalAmount(request.getPrincipalAmount());
        repayment.setInterestAmount(request.getInterestAmount());
        repayment.setPenaltyAmount(request.getPenaltyAmount() != null ? request.getPenaltyAmount() : BigDecimal.ZERO);
        repayment.setTotalAmount(request.getPrincipalAmount().add(request.getInterestAmount()).add(
            request.getPenaltyAmount() != null ? request.getPenaltyAmount() : BigDecimal.ZERO));
        repayment.setRepaymentType(request.getRepaymentType());
        repayment.setPaymentVoucher(request.getPaymentVoucher());
        repayment.setPaymentAccount(request.getPaymentAccount());
        repayment.setOffsetReceiptId(request.getOffsetReceiptId());
        repayment.setSignatureHash(request.getSignatureHash());
        repayment.setRepaymentTime(LocalDateTime.now());
        repayment.setStatus(LoanRepayment.STATUS_CONFIRMED);

        loanRepaymentMapper.insert(repayment);

        BigDecimal newRepaidPrincipal = (loan.getRepaidPrincipal() != null ? loan.getRepaidPrincipal() : BigDecimal.ZERO)
            .add(request.getPrincipalAmount());
        BigDecimal newRepaidInterest = (loan.getRepaidInterest() != null ? loan.getRepaidInterest() : BigDecimal.ZERO)
            .add(request.getInterestAmount());
        BigDecimal newOutstandingPrincipal = (loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : BigDecimal.ZERO)
            .subtract(request.getPrincipalAmount());
        BigDecimal newOutstandingInterest = (loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO)
            .subtract(request.getInterestAmount());

        loan.setRepaidPrincipal(newRepaidPrincipal);
        loan.setRepaidInterest(newRepaidInterest);
        loan.setOutstandingPrincipal(newOutstandingPrincipal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newOutstandingPrincipal);
        loan.setOutstandingInterest(newOutstandingInterest.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newOutstandingInterest);

        if (newOutstandingPrincipal.compareTo(BigDecimal.ZERO) <= 0 &&
            newOutstandingInterest.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.STATUS_SETTLED);
        } else {
            loan.setStatus(Loan.STATUS_REPAYING);
        }

        loanMapper.updateById(loan);

        logger.info("还款成功: loanNo={}, repaymentNo={}, principal={}, interest={}",
                loan.getLoanNo(), repaymentNo, request.getPrincipalAmount(), request.getInterestAmount());

        // 区块链上链 - 【P1-2修复】区块链上链成功后才处理仓单解锁，保障链上链下一致性
        boolean blockchainSuccess = false;
        if (blockchainFeignClient != null) {
            try {
                BlockchainFeignClient.LoanRepayRequest request2 = new BlockchainFeignClient.LoanRepayRequest();
                request2.setLoanNo(loan.getLoanNo());
                request2.setAmount(request.getPrincipalAmount().longValue());
                request2.setInterestAmount(request.getInterestAmount().longValue());
                Result<String> result = blockchainFeignClient.recordLoanRepayment(request2);
                if (result != null && ResultCodeEnum.SUCCESS.getCode().equals(result.getCode()) && result.getData() != null) {
                    loan.setChainTxHash(result.getData());
                    loanMapper.updateById(loan);
                    blockchainSuccess = true;
                    logger.info("贷款还款上链成功: loanNo={}, principal={}, interest={}, chainTxHash={}",
                            loan.getLoanNo(), request.getPrincipalAmount(), request.getInterestAmount(),
                            result != null ? result.getData() : "null");
                } else {
                    // 区块链返回错误码
                    String errMsg = "贷款还款区块链返回错误: loanNo=" + loan.getLoanNo()
                        + ", result=" + result + ", 还款记录已提交但链上未确认";
                    logger.error(errMsg);
                    // 【注意】此处抛出异常会回滚DB，但由于还款记录已插入，建议记录异常后人工处理
                    throw new RuntimeException(errMsg);
                }
            } catch (RuntimeException e) {
                // 区块链调用失败，向上抛出异常触发事务回滚
                throw e;
            } catch (Exception e) {
                String errMsg = "贷款还款上链异常: loanNo=" + loan.getLoanNo() + ", error=" + e.getMessage();
                logger.error(errMsg, e);
                throw new RuntimeException(errMsg, e);
            }
        } else {
            // 无区块链网关时，标记为成功（降级模式）
            blockchainSuccess = true;
            logger.warn("区块链网关不可用，还款以降级模式处理: loanNo={}", loan.getLoanNo());
        }

        // 【P1-2修复】只有在区块链上链成功后才解锁仓单
        // 如果区块链失败，事务回滚，仓单不解锁
        if (blockchainSuccess && loan.getReceiptId() != null && loan.getStatus() == Loan.STATUS_SETTLED) {
            try {
                warehouseFeignClient.unlockReceipt(loan.getReceiptId());
                logger.info("贷款结清已解锁仓单: loanNo={}, receiptId={}", loan.getLoanNo(), loan.getReceiptId());
            } catch (Exception e) {
                // 仓单解锁失败，记录错误但还款流程已完成
                // 建议后续添加补偿机制处理这种边缘情况
                logger.error("贷款结清解锁仓单失败: loanNo={}, receiptId={}, 请人工介入处理",
                        loan.getLoanNo(), loan.getReceiptId(), e);
            }
        }

        return repayment;
    }

    @Override
    public List<LoanRepayment> listRepayments(Long loanId) {
        if (loanId == null) {
            return List.of();
        }
        return loanRepaymentMapper.selectByLoanId(loanId);
    }

    @Override
    public List<LoanInstallment> listInstallments(Long loanId) {
        if (loanId == null) {
            return List.of();
        }
        return loanInstallmentMapper.selectByLoanId(loanId);
    }

    @Override
    public LoanCalculatorRequest.LoanCalculatorResult calculator(LoanCalculatorRequest request) {
        LoanCalculatorRequest.LoanCalculatorResult result = new LoanCalculatorRequest.LoanCalculatorResult();
        result.setPledgeRate(DEFAULT_PLEDGE_RATE);
        return result;
    }

    @Override
    public List<Loan> myLoans(Long borrowerEntId, Integer status) {
        if (borrowerEntId == null) {
            return List.of();
        }
        return loanMapper.selectActiveLoansByBorrower(borrowerEntId);
    }

    @Override
    public PageResult<Loan> myLoans(Long borrowerEntId, Integer status, Integer pageNum, Integer pageSize) {
        if (borrowerEntId == null) {
            return new PageResult<>(List.of(), 0L, pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        }
        Page<Loan> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<Loan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Loan::getBorrowerEntId, borrowerEntId);
        if (status != null) {
            wrapper.eq(Loan::getStatus, status);
        }
        wrapper.orderByDesc(Loan::getCreateTime);
        Page<Loan> result = loanMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public List<Loan> pendingLoans(Long financeEntId) {
        if (financeEntId == null) {
            return List.of();
        }
        return loanMapper.selectPendingByFinanceEntId(financeEntId);
    }

    @Override
    public PageResult<Loan> pendingLoans(Long financeEntId, Integer pageNum, Integer pageSize) {
        if (financeEntId == null) {
            return new PageResult<>(List.of(), 0L, pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        }
        Page<Loan> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<Loan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Loan::getFinanceEntId, financeEntId);
        wrapper.eq(Loan::getStatus, Loan.STATUS_PENDING);
        wrapper.orderByDesc(Loan::getCreateTime);
        Page<Loan> result = loanMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public int checkAndProcessOverdueLoans() {
        List<Loan> overdueLoans = loanMapper.selectOverdueLoans();
        for (Loan loan : overdueLoans) {
            loan.setStatus(Loan.STATUS_OVERDUE);
            loanMapper.updateById(loan);

            // 区块链上链: 逾期标记
            if (blockchainFeignClient != null) {
                try {
                    BlockchainFeignClient.LoanMarkOverdueRequest request = new BlockchainFeignClient.LoanMarkOverdueRequest();
                    request.setLoanNo(loan.getLoanNo());
                    // 【新问题修复】检查响应码确保区块链调用真正成功
                    Result<String> result = blockchainFeignClient.markOverdue(request);
                    if (result == null || result.getCode() != 0) {
                        logger.error("贷款逾期区块链失败: loanNo={}, result={}", loan.getLoanNo(), result);
                        // 不阻止本地逾期处理，但记录错误
                    } else {
                        if (result.getData() != null) {
                            loan.setChainTxHash(result.getData());
                            loanMapper.updateById(loan);
                        }
                        logger.info("贷款逾期上链成功: loanNo={}, chainTxHash={}", loan.getLoanNo(), result.getData());
                    }
                } catch (Exception e) {
                    logger.error("贷款逾期上链失败: loanNo={}", loan.getLoanNo(), e);
                    // 不阻止本地逾期处理，但记录错误
                }
            }

            logger.info("标记逾期: loanNo={}", loan.getLoanNo());
        }
        return overdueLoans.size();
    }

    @Override
    public List<LoanInstallment> generateInstallmentPlan(Long loanId, Integer installmentCount) {
        Loan loan = loanMapper.selectById(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("贷款不存在");
        }

        List<LoanInstallment> installments = new ArrayList<>();
        BigDecimal principalPerInstallment = loan.getLoanAmount().divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        BigDecimal interestPerInstallment = loan.getOutstandingInterest().divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        LocalDate startDate = loan.getLoanStartDate();
        long daysPerInstallment = loan.getLoanDays() / installmentCount;

        for (int i = 1; i <= installmentCount; i++) {
            LoanInstallment installment = new LoanInstallment();
            installment.setLoanId(loanId);
            installment.setInstallmentNo(i);
            installment.setDueDate(startDate.plusDays(daysPerInstallment * i));
            installment.setPrincipalAmount(principalPerInstallment);
            installment.setInterestAmount(interestPerInstallment);
            installment.setTotalAmount(principalPerInstallment.add(interestPerInstallment));
            installment.setRepaidAmount(BigDecimal.ZERO);
            installment.setStatus(LoanInstallment.STATUS_PENDING);

            loanInstallmentMapper.insert(installment);
            installments.add(installment);
        }

        return installments;
    }

    @Override
    public BigDecimal calculateTotalRepayable(Loan loan) {
        BigDecimal principal = loan.getLoanAmount() != null ? loan.getLoanAmount() : BigDecimal.ZERO;
        BigDecimal interest = loan.getOutstandingInterest() != null ? loan.getOutstandingInterest() : BigDecimal.ZERO;
        return principal.add(interest);
    }

    @Override
    public BigDecimal calculateInterest(Loan loan, LocalDate calculateDate) {
        if (loan.getLoanStartDate() == null || loan.getOutstandingPrincipal() == null) {
            return BigDecimal.ZERO;
        }
        long days = ChronoUnit.DAYS.between(loan.getLoanStartDate(), calculateDate);
        if (days <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal dailyRate = (loan.getLoanInterestRate() != null ? loan.getLoanInterestRate() : DEFAULT_INTEREST_RATE)
            .divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);
        return loan.getOutstandingPrincipal().multiply(dailyRate).multiply(BigDecimal.valueOf(days));
    }

    @Override
    public BigDecimal calculatePenalty(Loan loan, BigDecimal penaltyRate) {
        if (loan.getLoanEndDate() == null) {
            return BigDecimal.ZERO;
        }
        LocalDate today = LocalDate.now();
        if (!today.isAfter(loan.getLoanEndDate())) {
            return BigDecimal.ZERO;
        }
        long overdueDays = ChronoUnit.DAYS.between(loan.getLoanEndDate(), today) - GRACE_PERIOD_DAYS;
        if (overdueDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal penalty = (loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : BigDecimal.ZERO)
            .multiply(penaltyRate != null ? penaltyRate : DAILY_PENALTY_RATE)
            .multiply(BigDecimal.valueOf(overdueDays));
        return penalty;
    }

    private BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal annualRate, Integer days) {
        BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);
        return principal.multiply(dailyRate).multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }
}
