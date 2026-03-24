package com.fisco.app.service.impl;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.service.BaseContractService;
import com.fisco.app.contract.receivable.ReceivableCore;
import com.fisco.app.contract.receivable.ReceivableCore.ReceivableInput;
import com.fisco.app.contract.receivable.ReceivableRepayment;

/**
 * 应收款合约服务
 */
@Service
public class ReceivableContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(ReceivableContractService.class);

    @Value("${contract.receivable-core:}")
    private String receivableCoreAddress;

    @Value("${contract.receivable-repayment:}")
    private String receivableRepaymentAddress;

    private ReceivableCore receivableCoreContract;
    private ReceivableRepayment receivableRepaymentContract;

    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，应收款合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载应收款合约");
            return;
        }

        if (receivableCoreAddress != null && !receivableCoreAddress.isBlank()) {
            this.receivableCoreContract = ReceivableCore.load(
                    receivableCoreAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("应收款核心合约加载成功，地址: {}", receivableCoreAddress);
        } else {
            logger.warn("应收款核心合约地址未配置");
        }

        if (receivableRepaymentAddress != null && !receivableRepaymentAddress.isBlank()) {
            this.receivableRepaymentContract = ReceivableRepayment.load(
                    receivableRepaymentAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("应收款还款合约加载成功，地址: {}", receivableRepaymentAddress);
        } else {
            logger.warn("应收款还款合约地址未配置");
        }
    }

    private void checkCoreContract() {
        if (receivableCoreContract == null) {
            throw new RuntimeException("应收款核心合约未初始化，请检查区块链连接");
        }
    }

    private void checkRepaymentContract() {
        if (receivableRepaymentContract == null) {
            throw new RuntimeException("应收款还款合约未初始化，请检查区块链连接");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends org.fisco.bcos.sdk.v3.contract.Contract> T loadContract(String contractAddress) {
        if (contractAddress.equals(receivableCoreAddress)) {
            return (T) ReceivableCore.load(contractAddress, client, cryptoKeyPair);
        } else if (contractAddress.equals(receivableRepaymentAddress)) {
            return (T) ReceivableRepayment.load(contractAddress, client, cryptoKeyPair);
        }
        return null;
    }

    public TransactionReceipt createReceivable(
            String receivableId,
            BigInteger initialAmount,
            BigInteger dueDate,
            byte[] buyerSellerPairHash,
            byte[] invoiceHash,
            byte[] contractHash,
            byte[] goodsDetailHash,
            BigInteger businessScene) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (initialAmount == null || initialAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("初始金额必须大于0");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("到期日期不能为空");
        }

        ReceivableInput input = new ReceivableInput(
                receivableId,
                initialAmount,
                dueDate,
                buyerSellerPairHash != null ? buyerSellerPairHash : new byte[32],
                invoiceHash != null ? invoiceHash : new byte[32],
                contractHash != null ? contractHash : new byte[32],
                goodsDetailHash != null ? goodsDetailHash : new byte[32],
                businessScene != null ? businessScene : BigInteger.ONE
        );

        logger.info("创建应收款上链: receivableId={}, initialAmount={}", receivableId, initialAmount);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "createReceivable",
                new Object[]{input},
                "RECEIVABLE_CREATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("创建应收款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt confirmReceivable(String receivableId, byte[] signature) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        logger.info("确认应收款上链: receivableId={}", receivableId);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "confirmReceivable",
                new Object[]{receivableId, signature != null ? signature : new byte[0]},
                "RECEIVABLE_CONFIRM"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("确认应收款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt adjustReceivable(
            String receivableId,
            BigInteger adjustedAmount,
            BigInteger adjustType) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (adjustedAmount == null || adjustedAmount.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("调整金额不能为负数");
        }

        logger.info("调整应收款上链: receivableId={}, adjustedAmount={}, adjustType={}",
                receivableId, adjustedAmount, adjustType);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "adjustReceivable",
                new Object[]{receivableId, adjustedAmount, adjustType},
                "RECEIVABLE_ADJUST"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("调整应收款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt financeReceivable(
            String receivableId,
            BigInteger financeAmount,
            String financeEntity) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (financeAmount == null || financeAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("融资金额必须大于0");
        }

        logger.info("应收款融资上链: receivableId={}, financeAmount={}",
                receivableId, financeAmount);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "financeReceivable",
                new Object[]{receivableId, financeAmount, financeEntity},
                "RECEIVABLE_FINANCE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("应收款融资上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt settleReceivable(String receivableId) {
        checkCoreContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        logger.info("应收款结算上链: receivableId={}", receivableId);

        TransactionResponse response = sendTransactionWithAudit(
                receivableCoreContract,
                "settleReceivable",
                new Object[]{receivableId},
                "RECEIVABLE_SETTLE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("应收款结算上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public Object getReceivable(String receivableId) {
        checkCoreContract();
        try {
            return receivableCoreContract.getReceivable(receivableId);
        } catch (ContractException e) {
            logger.warn("应收款不存在: receivableId={}", receivableId);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getReceivablesByPair(byte[] buyerSellerPairHash) {
        checkCoreContract();
        try {
            return (List<String>) receivableCoreContract.getReceivablesByPair(buyerSellerPairHash);
        } catch (ContractException e) {
            logger.error("查询应收款列表失败", e);
            return List.of();
        }
    }

    public BigInteger getReceivableStatus(String receivableId) {
        checkCoreContract();
        try {
            return receivableCoreContract.getReceivableStatus(receivableId);
        } catch (ContractException e) {
            logger.warn("查询应收款状态失败: receivableId={}", receivableId);
            return BigInteger.ZERO;
        }
    }

    public TransactionReceipt recordRepayment(
            String receivableId,
            BigInteger repaymentAmount,
            BigInteger repaymentType) {
        checkRepaymentContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (repaymentAmount == null || repaymentAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("还款金额必须大于0");
        }

        logger.info("记录还款上链: receivableId={}, repaymentAmount={}, repaymentType={}",
                receivableId, repaymentAmount, repaymentType);

        TransactionResponse response = sendTransactionWithAudit(
                receivableRepaymentContract,
                "recordRepayment",
                new Object[]{receivableId, repaymentAmount, repaymentType},
                "REPAYMENT_RECORD"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("记录还款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt recordFullRepayment(String receivableId) {
        checkRepaymentContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }

        logger.info("记录全额还款上链: receivableId={}", receivableId);

        TransactionResponse response = sendTransactionWithAudit(
                receivableRepaymentContract,
                "recordFullRepayment",
                new Object[]{receivableId},
                "REPAYMENT_FULL"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("记录全额还款上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt offsetDebtWithCollateral(
            String receivableId,
            String receiptId,
            BigInteger offsetAmount,
            byte[] signatureHash) {
        checkRepaymentContract();

        if (receivableId == null || receivableId.isBlank()) {
            throw new IllegalArgumentException("应收款ID不能为空");
        }
        if (receiptId == null || receiptId.isBlank()) {
            throw new IllegalArgumentException("仓单ID不能为空");
        }

        logger.info("以物抵债上链: receivableId={}, receiptId={}, offsetAmount={}",
                receivableId, receiptId, offsetAmount);

        TransactionResponse response = sendTransactionWithAudit(
                receivableRepaymentContract,
                "offsetDebtWithCollateral",
                new Object[]{receivableId, receiptId, offsetAmount,
                    signatureHash != null ? signatureHash : new byte[0]},
                "OFFSET_DEBT_COLLATERAL"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("以物抵债上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }
}
