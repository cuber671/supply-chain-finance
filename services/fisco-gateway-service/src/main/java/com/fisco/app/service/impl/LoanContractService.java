package com.fisco.app.service.impl;

import java.math.BigInteger;

import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.service.BaseContractService;
import com.fisco.app.contract.loan.LoanCore;
import com.fisco.app.contract.loan.LoanRepayment;
import com.fisco.app.contract.loan.WarehouseReceiptCoreExt;

/**
 * 贷款合约服务
 */
@Service
public class LoanContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(LoanContractService.class);

    @Value("${contract.loan-core:}")
    private String loanCoreAddress;

    @Value("${contract.loan-repayment:}")
    private String loanRepaymentAddress;

    @Value("${contract.warehouse-core-ext:}")
    private String warehouseCoreExtAddress;

    private LoanCore loanCoreContract;
    private LoanRepayment loanRepaymentContract;
    private WarehouseReceiptCoreExt warehouseCoreExtContract;

    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，贷款合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载贷款合约");
            return;
        }

        if (loanCoreAddress != null && !loanCoreAddress.isEmpty()) {
            this.loanCoreContract = LoanCore.load(loanCoreAddress, client, cryptoKeyPair);
            logger.info("贷款核心合约加载成功，地址: {}", loanCoreAddress);
        } else {
            logger.warn("贷款核心合约地址未配置");
        }

        if (loanRepaymentAddress != null && !loanRepaymentAddress.isEmpty()) {
            this.loanRepaymentContract = LoanRepayment.load(loanRepaymentAddress, client, cryptoKeyPair);
            logger.info("贷款还款合约加载成功，地址: {}", loanRepaymentAddress);
        } else {
            logger.warn("贷款还款合约地址未配置");
        }

        if (warehouseCoreExtAddress != null && !warehouseCoreExtAddress.isEmpty()) {
            this.warehouseCoreExtContract = WarehouseReceiptCoreExt.load(
                    warehouseCoreExtAddress, client, cryptoKeyPair);
            logger.info("仓单核心扩展合约加载成功，地址: {}", warehouseCoreExtAddress);
        } else {
            logger.warn("仓单核心扩展合约地址未配置");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Contract> T loadContract(String contractAddress) {
        if (contractAddress.equals(loanCoreAddress)) {
            return (T) LoanCore.load(contractAddress, client, cryptoKeyPair);
        } else if (contractAddress.equals(loanRepaymentAddress)) {
            return (T) LoanRepayment.load(contractAddress, client, cryptoKeyPair);
        }
        return null;
    }

    private void checkCoreContract() {
        if (loanCoreContract == null) {
            throw new RuntimeException("贷款核心合约未初始化，请检查区块链连接");
        }
    }

    private void checkRepaymentContract() {
        if (loanRepaymentContract == null) {
            throw new RuntimeException("贷款还款合约未初始化，请检查区块链连接");
        }
    }

    private void checkWarehouseExtContract() {
        if (warehouseCoreExtContract == null) {
            throw new RuntimeException("仓单核心扩展合约未初始化，请检查区块链连接");
        }
    }

    public TransactionReceipt createLoan(
            String loanNo,
            String borrowerHash,
            BigInteger amount,
            BigInteger loanDays,
            String receiptId,
            BigInteger pledgeAmount) {
        checkCoreContract();

        logger.info("创建贷款上链: loanNo={}, borrowerHash={}, amount={}", loanNo, borrowerHash, amount);

        TransactionResponse response = sendTransactionWithAudit(
                loanCoreContract,
                "createLoan",
                new Object[]{loanNo, borrowerHash.getBytes(), new byte[32], amount, loanDays, receiptId, pledgeAmount},
                "LOAN_CREATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("创建贷款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt approveLoan(
            String loanNo,
            BigInteger approvedAmount,
            BigInteger interestRate,
            BigInteger loanDays) {
        checkCoreContract();

        logger.info("审批贷款上链: loanNo={}, approvedAmount={}, interestRate={}",
                loanNo, approvedAmount, interestRate);

        TransactionResponse response = sendTransactionWithAudit(
                loanCoreContract,
                "approveLoan",
                new Object[]{loanNo, approvedAmount, interestRate, loanDays},
                "LOAN_APPROVE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("审批贷款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt cancelLoan(String loanNo, String reason) {
        checkCoreContract();

        logger.info("取消贷款上链: loanNo={}, reason={}", loanNo, reason);

        TransactionResponse response = sendTransactionWithAudit(
                loanCoreContract,
                "cancelLoan",
                new Object[]{loanNo, reason},
                "LOAN_CANCEL"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("取消贷款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt disburseLoan(String loanNo, String receiptId) {
        checkCoreContract();

        logger.info("放款上链: loanNo={}, receiptId={}", loanNo, receiptId);

        // 从链上获取贷款信息以计算完整参数
        BigInteger approvedAmount;
        BigInteger loanDays;
        try {
            approvedAmount = loanCoreContract.loanAmount(loanNo);
            loanDays = loanCoreContract.loanDays(loanNo);
        } catch (ContractException e) {
            logger.error("获取贷款信息失败: loanNo={}", loanNo, e);
            throw new RuntimeException("获取贷款信息失败: " + loanNo, e);
        }
        BigInteger startDate = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        BigInteger endDate = startDate.add(loanDays.multiply(BigInteger.valueOf(86400)));

        TransactionResponse response = sendTransactionWithAudit(
                loanCoreContract,
                "disburseLoan",
                new Object[]{loanNo, approvedAmount, startDate, endDate, receiptId},
                "LOAN_DISBURSE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("放款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt recordRepayment(String loanNo, BigInteger amount, BigInteger installmentIndex) {
        checkRepaymentContract();

        logger.info("记录还款上链: loanNo={}, amount={}, installmentIndex={}",
                loanNo, amount, installmentIndex);

        TransactionResponse response = sendTransactionWithAudit(
                loanRepaymentContract,
                "recordRepayment",
                new Object[]{loanNo, amount, installmentIndex},
                "LOAN_REPAY"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("记录还款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt markOverdue(
            String loanNo,
            BigInteger overdueDays,
            BigInteger penaltyRate,
            BigInteger penaltyAmount) {
        checkCoreContract();

        logger.info("标记逾期上链: loanNo={}, overdueDays={}", loanNo, overdueDays);

        TransactionResponse response = sendTransactionWithAudit(
                loanCoreContract,
                "markOverdue",
                new Object[]{loanNo, overdueDays, penaltyRate, penaltyAmount},
                "LOAN_OVERDUE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("标记逾期失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt markDefaulted(
            String loanNo,
            String disposalMethod,
            BigInteger disposalAmount) {
        checkCoreContract();

        logger.info("违约处置上链: loanNo={}, disposalMethod={}", loanNo, disposalMethod);

        TransactionResponse response = sendTransactionWithAudit(
                loanCoreContract,
                "markDefaulted",
                new Object[]{loanNo, disposalMethod, disposalAmount},
                "LOAN_DEFAULTED"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("违约处置失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt setReceiptLoanId(
            String receiptId,
            String loanNo,
            BigInteger pledgeAmount) {
        checkWarehouseExtContract();

        logger.info("设置仓单-贷款关联: receiptId={}, loanNo={}, pledgeAmount={}",
                receiptId, loanNo, pledgeAmount);

        // 【修复G2】通过审计服务发送交易，确保关键操作可追溯
        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreExtContract,
                "setReceiptLoanId",
                new Object[]{receiptId, loanNo, pledgeAmount},
                "RECEIPT_LOAN_SET"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("设置仓单-贷款关联失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }

        logger.info("设置仓单-贷款关联: receiptId={}, tx={}",
                receiptId, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    public TransactionReceipt updateReceiptLoanId(String receiptId, String newLoanNo) {
        checkWarehouseExtContract();

        logger.info("更新仓单-贷款关联: receiptId={}, newLoanNo={}", receiptId, newLoanNo);

        // 【修复G2】通过审计服务发送交易，确保关键操作可追溯
        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreExtContract,
                "updateReceiptLoanId",
                new Object[]{receiptId, newLoanNo},
                "RECEIPT_LOAN_UPDATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("更新仓单-贷款关联失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }

        logger.info("更新仓单-贷款关联: receiptId={}, tx={}",
                receiptId, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    public String getLoanCore(String loanNo) {
        logger.debug("查询贷款核心信息: loanNo={}", loanNo);
        // 【修复G1】返回真实的区块链贷款核心信息
        checkCoreContract();
        try {
            StringBuilder info = new StringBuilder();
            info.append("loanNo:").append(loanNo).append("|");

            // 查询贷款状态
            BigInteger status = loanCoreContract.getLoanStatus(loanNo);
            info.append("status:").append(status).append("|");

            // 查询未还本金
            BigInteger outstandingPrincipal = loanCoreContract.getOutstandingPrincipal(loanNo);
            info.append("outstandingPrincipal:").append(outstandingPrincipal).append("|");

            // 查询是否存在
            Boolean exists = loanCoreContract.exists(loanNo);
            info.append("exists:").append(exists);

            return info.toString();
        } catch (ContractException e) {
            logger.error("获取贷款核心信息失败: loanNo={}", loanNo, e);
            throw new RuntimeException("获取贷款核心信息失败: " + e.getMessage());
        }
    }

    public BigInteger getLoanStatus(String loanNo) {
        checkCoreContract();
        try {
            return loanCoreContract.getLoanStatus(loanNo);
        } catch (ContractException e) {
            logger.error("获取贷款状态失败: loanNo={}", loanNo, e);
            return BigInteger.ZERO;
        }
    }

    public String getLoanByReceipt(String receiptId) {
        checkCoreContract();
        try {
            return loanCoreContract.getLoanByReceipt(receiptId);
        } catch (ContractException e) {
            logger.error("获取仓单关联贷款失败: receiptId={}", receiptId, e);
            return null;
        }
    }

    public Boolean exists(String loanNo) {
        checkCoreContract();
        try {
            return loanCoreContract.exists(loanNo);
        } catch (ContractException e) {
            logger.error("检查贷款是否存在失败: loanNo={}", loanNo, e);
            return false;
        }
    }
}
