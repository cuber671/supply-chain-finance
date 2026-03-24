package com.fisco.app.service;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple5;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.contract.credit.CreditLimitCore;
import com.fisco.app.contract.credit.CreditLimitScore;
import com.fisco.app.contract.credit.CreditLimitCore.CreditEvent;
import com.fisco.app.contract.credit.CreditLimitScore.ScoreRecord;

/**
 * 信用合约上链服务
 *
 * 提供信用额度管理、信用评分计算等区块链操作
 * 简化版本：提供接口但不直接操作区块链（区块链操作通过fisco-gateway-service完成）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Service
public class CreditContractService {

    private static final Logger logger = LoggerFactory.getLogger(CreditContractService.class);

    @Value("${contract.credit-core:}")
    private String creditCoreContractAddress;

    @Value("${contract.credit-score:}")
    private String creditScoreContractAddress;

    @Autowired(required = false)
    private Client client;

    @Autowired(required = false)
    private CryptoKeyPair cryptoKeyPair;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

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
                    creditCoreContractAddress, client, cryptoKeyPair);
            logger.info("信用额度核心合约加载成功，地址: {}", creditCoreContractAddress);
        }

        if (creditScoreContractAddress != null && !creditScoreContractAddress.isEmpty()) {
            this.creditScoreContract = CreditLimitScore.load(
                    creditScoreContractAddress, client, cryptoKeyPair);
            logger.info("信用评分合约加载成功，地址: {}", creditScoreContractAddress);
        }
    }

    protected Contract loadContract(String contractAddress) {
        if (creditCoreContractAddress != null && !creditCoreContractAddress.isEmpty()) {
            return CreditLimitCore.load(contractAddress, client, cryptoKeyPair);
        }
        return null;
    }

    // ==================== 信用额度管理 ====================

    public TransactionReceipt setCreditLimit(String enterpriseAddress, BigInteger newLimit) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过设置授信额度: {}", enterpriseAddress);
            return null;
        }

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

    public TransactionReceipt useCredit(String enterpriseAddress, BigInteger amount, String operationType) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过使用信用额度: {}", enterpriseAddress);
            return null;
        }

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
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过释放信用额度: {}", enterpriseAddress);
            return null;
        }

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
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过调整已用额度: {}", enterpriseAddress);
            return null;
        }

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
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过上报信用事件: {}", enterpriseAddress);
            return null;
        }

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

    // ==================== 信用查询 ====================

    public Tuple5<String, BigInteger, BigInteger, BigInteger, BigInteger> getCreditInfo(String enterpriseAddress) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，返回空信用信息: {}", enterpriseAddress);
            return null;
        }

        logger.debug("查询信用额度信息: {}", enterpriseAddress);
        try {
            return creditCoreContract.getCreditInfo(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("查询信用额度信息失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public boolean checkCreditLimit(String enterpriseAddress, BigInteger amount) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回true: {}", enterpriseAddress);
            return true;
        }

        try {
            return creditCoreContract.checkCreditLimit(enterpriseAddress, amount);
        } catch (ContractException e) {
            logger.error("检查可用额度失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public boolean hasCreditRecord(String enterpriseAddress) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回false: {}", enterpriseAddress);
            return false;
        }

        try {
            return creditCoreContract.hasCreditRecord(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("检查信用记录失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public BigInteger getCreditEventCount(String enterpriseAddress) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回0: {}", enterpriseAddress);
            return BigInteger.ZERO;
        }

        try {
            return creditCoreContract.getCreditEventCount(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取信用事件数量失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public List<CreditEvent> getCreditEvents(String enterpriseAddress, BigInteger offset, BigInteger limit) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，返回空列表: {}", enterpriseAddress);
            return null;
        }

        try {
            return creditCoreContract.getCreditEvents(enterpriseAddress, offset, limit);
        } catch (ContractException e) {
            logger.error("获取信用事件列表失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    // ==================== 信用评分 ====================

    public TransactionReceipt calculateScore(String enterpriseAddress, CreditLimitScore.ScoreFactors factors) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过计算信用评分: {}", enterpriseAddress);
            return null;
        }

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }

        logger.info("计算信用评分: address={}", enterpriseAddress);
        TransactionReceipt receipt = creditScoreContract.calculateScore(enterpriseAddress, factors);
        if (receipt.isStatusOK()) {
            logger.info("计算信用评分成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("计算信用评分失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    public TransactionReceipt adjustScore(String enterpriseAddress, BigInteger newScore, String reason) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过调整信用评分: {}", enterpriseAddress);
            return null;
        }

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newScore == null || newScore.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("评分不能为负数");
        }

        logger.info("调整信用评分: address={}, newScore={}, reason={}", enterpriseAddress, newScore, reason);
        TransactionReceipt receipt = creditScoreContract.adjustScore(enterpriseAddress, newScore, reason);
        if (receipt.isStatusOK()) {
            logger.info("调整信用评分成功: {}, status: {}", enterpriseAddress, receipt.getStatus());
        } else {
            logger.error("调整信用评分失败: {}, status: {}", enterpriseAddress, receipt.getStatus());
        }
        return receipt;
    }

    public BigInteger getCurrentScore(String enterpriseAddress) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回0: {}", enterpriseAddress);
            return BigInteger.ZERO;
        }

        try {
            return creditScoreContract.getCurrentScore(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取当前评分失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public Tuple3<BigInteger, BigInteger, BigInteger> getLatestScore(String enterpriseAddress) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，返回空记录: {}", enterpriseAddress);
            return null;
        }

        try {
            return creditScoreContract.getLatestScore(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取最新评分记录失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public BigInteger getScoreHistoryCount(String enterpriseAddress) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回0: {}", enterpriseAddress);
            return BigInteger.ZERO;
        }

        try {
            return creditScoreContract.getScoreHistoryCount(enterpriseAddress);
        } catch (ContractException e) {
            logger.error("获取评分历史数量失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public List<ScoreRecord> getScoreHistory(String enterpriseAddress, BigInteger offset, BigInteger limit) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，返回空列表: {}", enterpriseAddress);
            return null;
        }

        try {
            return creditScoreContract.getScoreHistory(enterpriseAddress, offset, limit);
        } catch (ContractException e) {
            logger.error("获取评分历史失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public boolean checkScoreLevel(String enterpriseAddress, BigInteger minScore) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回true: {}", enterpriseAddress);
            return true;
        }

        try {
            return creditScoreContract.checkScoreLevel(enterpriseAddress, minScore);
        } catch (ContractException e) {
            logger.error("检查评分等级失败: {}", enterpriseAddress, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    public String getScoreLevel(BigInteger score) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，默认返回D: {}", score);
            return "D";
        }

        try {
            return creditScoreContract.getScoreLevel(score);
        } catch (ContractException e) {
            logger.error("获取评分等级失败: score={}", score, e);
            throw new RuntimeException("查询失败，请稍后重试");
        }
    }

    // ==================== 批量操作 ====================

    public TransactionReceipt batchSetCreditLimit(List<String> enterprises, List<BigInteger> limits) {
        if (!fiscoEnabled || creditCoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过批量设置授信额度");
            return null;
        }

        if (enterprises == null || enterprises.isEmpty()) {
            throw new IllegalArgumentException("企业地址列表不能为空");
        }
        if (limits == null || limits.isEmpty()) {
            throw new IllegalArgumentException("额度列表不能为空");
        }
        if (enterprises.size() != limits.size()) {
            throw new IllegalArgumentException("企业地址数量与额度数量不匹配");
        }

        logger.info("批量设置授信额度: count={}, firstAddress={}", enterprises.size(), enterprises.get(0));
        TransactionReceipt receipt = creditCoreContract.batchSetCreditLimit(enterprises, limits);
        if (receipt.isStatusOK()) {
            logger.info("批量设置授信额度成功: count={}, status: {}", enterprises.size(), receipt.getStatus());
        } else {
            logger.error("批量设置授信额度失败: count={}, status: {}", enterprises.size(), receipt.getStatus());
        }
        return receipt;
    }

    public TransactionReceipt batchCalculateScore(List<String> enterprises,
            List<CreditLimitScore.ScoreFactors> factorsArray) {
        if (!fiscoEnabled || creditScoreContract == null) {
            logger.warn("FISCO BCOS 功能已禁用或合约未初始化，跳过批量计算评分");
            return null;
        }

        if (enterprises == null || enterprises.isEmpty()) {
            throw new IllegalArgumentException("企业地址列表不能为空");
        }
        if (factorsArray == null || factorsArray.isEmpty()) {
            throw new IllegalArgumentException("评分因素列表不能为空");
        }
        if (enterprises.size() != factorsArray.size()) {
            throw new IllegalArgumentException("企业地址数量与评分因素数量不匹配");
        }

        logger.info("批量计算评分: count={}, firstAddress={}", enterprises.size(), enterprises.get(0));
        TransactionReceipt receipt = creditScoreContract.batchCalculateScore(enterprises, factorsArray);
        if (receipt.isStatusOK()) {
            logger.info("批量计算评分成功: count={}, status: {}", enterprises.size(), receipt.getStatus());
        } else {
            logger.error("批量计算评分失败: count={}, status: {}", enterprises.size(), receipt.getStatus());
        }
        return receipt;
    }
}
