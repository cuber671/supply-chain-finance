package com.fisco.app.service.impl;

import java.math.BigInteger;

import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.service.BaseContractService;
import com.fisco.app.contract.CreditLimitCore;
import com.fisco.app.contract.CreditLimitScore;

/**
 * 信用合约上链服务
 *
 * 提供信用额度管理、信用评分计算等区块链操作
 * 集成到 fisco-gateway-service，作为信用业务的唯一区块链操作入口
 */
@Service
public class CreditContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(CreditContractService.class);

    @Value("${contract.credit-core:}")
    private String creditCoreContractAddress;

    @Value("${contract.credit-score:}")
    private String creditScoreContractAddress;

    private CreditLimitCore creditCoreContract;
    private CreditLimitScore creditScoreContract;

    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，信用合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载信用合约");
            return;
        }

        logger.info("使用 SDK 密钥对，地址: {}", cryptoKeyPair.getAddress());

        if (creditCoreContractAddress != null && !creditCoreContractAddress.isEmpty()) {
            this.creditCoreContract = CreditLimitCore.load(
                    creditCoreContractAddress, client);
            logger.info("信用额度核心合约加载成功，地址: {}", creditCoreContractAddress);
        }

        if (creditScoreContractAddress != null && !creditScoreContractAddress.isEmpty()) {
            this.creditScoreContract = CreditLimitScore.load(
                    creditScoreContractAddress, client);
            logger.info("信用评分合约加载成功，地址: {}", creditScoreContractAddress);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Contract> T loadContract(String contractAddress) {
        if (creditCoreContractAddress != null && !creditCoreContractAddress.isEmpty()) {
            return (T) CreditLimitCore.load(contractAddress, client);
        }
        return null;
    }

    private void checkCoreContract() {
        if (creditCoreContract == null) {
            throw new RuntimeException("信用核心合约未初始化，请稍后重试");
        }
    }

    private void checkScoreContract() {
        if (creditScoreContract == null) {
            throw new RuntimeException("信用评分合约未初始化，请稍后重试");
        }
    }

    // ==================== 信用额度管理 ====================

    public TransactionReceipt setCreditLimit(String enterpriseAddress, BigInteger newLimit) {
        checkCoreContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newLimit == null || newLimit.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("授信额度不能为负数");
        }

        logger.info("设置授信额度: address={}, limit={}", enterpriseAddress, newLimit);
        TransactionReceipt receipt = creditCoreContract.setCreditLimit(enterpriseAddress, newLimit);
        if (receipt.isStatusOK()) {
            logger.info("设置授信额度成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("设置授信额度失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    public boolean checkCreditLimit(String enterpriseAddress, BigInteger amount) {
        checkCoreContract();

        try {
            return creditCoreContract.checkCreditLimit(enterpriseAddress, amount);
        } catch (ContractException e) {
            logger.error("检查可用额度失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public TransactionReceipt useCredit(String enterpriseAddress, BigInteger amount, String operationType) {
        checkCoreContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("使用金额必须大于0");
        }

        logger.info("使用信用额度: address={}, amount={}, type={}", enterpriseAddress, amount, operationType);
        TransactionReceipt receipt = creditCoreContract.useCredit(enterpriseAddress, amount, operationType);
        if (receipt.isStatusOK()) {
            logger.info("使用信用额度成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("使用信用额度失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    public TransactionReceipt releaseCredit(String enterpriseAddress, BigInteger amount, String operationType) {
        checkCoreContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("释放金额必须大于0");
        }

        logger.info("释放信用额度: address={}, amount={}, type={}", enterpriseAddress, amount, operationType);
        TransactionReceipt receipt = creditCoreContract.releaseCredit(enterpriseAddress, amount, operationType);
        if (receipt.isStatusOK()) {
            logger.info("释放信用额度成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("释放信用额度失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    public TransactionReceipt adjustUsedCredit(String enterpriseAddress, BigInteger adjustment) {
        checkCoreContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (adjustment == null) {
            throw new IllegalArgumentException("调整金额不能为空");
        }

        logger.info("调整已用额度: address={}, adjustment={}", enterpriseAddress, adjustment);
        TransactionReceipt receipt = creditCoreContract.adjustUsedCredit(enterpriseAddress, adjustment);
        if (receipt.isStatusOK()) {
            logger.info("调整已用额度成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("调整已用额度失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    // ==================== 信用事件上报 ====================

    public TransactionReceipt reportCreditEvent(String enterpriseAddress, BigInteger eventType,
            BigInteger impact, byte[] eventDataHash) {
        checkCoreContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("事件类型不能为空");
        }

        logger.info("上报信用事件: address={}, eventType={}, impact={}", enterpriseAddress, eventType, impact);
        TransactionReceipt receipt = creditCoreContract.reportCreditEvent(enterpriseAddress, eventType, impact, eventDataHash);
        if (receipt.isStatusOK()) {
            logger.info("上报信用事件成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("上报信用事件失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    // ==================== 信用评分 ====================

    public TransactionReceipt calculateScore(String enterpriseAddress) {
        checkScoreContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }

        logger.info("计算信用评分: address={}", enterpriseAddress);
        TransactionReceipt receipt = creditScoreContract.calculateScore(enterpriseAddress, null);
        if (receipt.isStatusOK()) {
            logger.info("计算信用评分成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("计算信用评分失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }
}