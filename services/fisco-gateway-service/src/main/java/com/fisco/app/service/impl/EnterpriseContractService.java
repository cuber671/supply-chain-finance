package com.fisco.app.service.impl;

import java.math.BigInteger;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.service.BaseContractService;
import com.fisco.app.contract.enterprise.EnterpriseRegistryV2;
import com.fisco.app.contract.enterprise.EnterpriseRegistryV2.EnterpriseRegistrationInput;

/**
 * 企业上链服务
 *
 * 提供企业注册，信息查询、状态更新等区块链操作
 */
@Service
public class EnterpriseContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(EnterpriseContractService.class);

    @Value("${contract.enterprise:}")
    private String enterpriseContractAddress;

    private EnterpriseRegistryV2 enterpriseContract;

    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，企业合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载企业合约");
            return;
        }
        if (enterpriseContractAddress == null || enterpriseContractAddress.isEmpty()) {
            logger.warn("企业合约地址未配置");
            return;
        }
        this.enterpriseContract = EnterpriseRegistryV2.load(
                enterpriseContractAddress,
                client,
                cryptoKeyPair
        );
        logger.info("企业合约加载成功，地址: {}", enterpriseContractAddress);
    }

    private void checkContract() {
        if (enterpriseContract == null) {
            throw new RuntimeException("服务暂不可用，请稍后重试");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends org.fisco.bcos.sdk.v3.contract.Contract> T loadContract(String contractAddress) {
        return (T) EnterpriseRegistryV2.load(contractAddress, client, cryptoKeyPair);
    }

    public TransactionReceipt registerEnterprise(
            String enterpriseAddress,
            String creditCode,
            BigInteger role,
            byte[] metadataHash) {
        checkContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (creditCode == null || creditCode.isBlank()) {
            throw new IllegalArgumentException("统一社会信用代码不能为空");
        }

        // FISCO SDK v3 Bytes32 要求恰好 32 字节，确保 padding
        byte[] paddedMetadataHash = metadataHash != null ? padTo32Bytes(metadataHash) : new byte[32];

        EnterpriseRegistrationInput input = new EnterpriseRegistrationInput(
                enterpriseAddress,
                creditCode,
                role,
                paddedMetadataHash
        );

        logger.info("注册企业上链: address={}, creditCode={}, role={}",
                enterpriseAddress, creditCode, role);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "registerEnterprise",
                new Object[]{input},
                "ENTERPRISE_REGISTER"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("注册企业上链失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public EnterpriseInfo getEnterprise(String enterpriseAddress) throws ContractException {
        checkContract();
        logger.debug("查询企业信息: {}", enterpriseAddress);
        var result = enterpriseContract.getEnterprise(enterpriseAddress);
        return new EnterpriseInfo(result);
    }

    public String getEnterpriseByCreditCode(String creditCode) {
        checkContract();
        logger.debug("根据信用代码查询企业: {}", creditCode);
        try {
            return enterpriseContract.getEnterpriseByCreditCode(creditCode);
        } catch (ContractException e) {
            logger.warn("企业不存在: creditCode={}", creditCode);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getEnterpriseList() {
        checkContract();
        logger.debug("获取企业列表");
        try {
            return (java.util.List<String>) (java.util.List<?>)
                enterpriseContract.getEnterpriseList();
        } catch (ContractException e) {
            logger.error("获取企业列表失败", e);
            return java.util.List.of();
        }
    }

    public boolean isEnterpriseValid(String enterpriseAddress) {
        checkContract();
        logger.debug("验证企业有效性: {}", enterpriseAddress);
        try {
            return enterpriseContract.isEnterpriseValid(enterpriseAddress);
        } catch (ContractException e) {
            logger.warn("企业有效性验证失败: address={}", enterpriseAddress, e);
            return false;
        }
    }

    public TransactionReceipt updateEnterpriseStatus(String enterpriseAddress, BigInteger newStatus) {
        checkContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("状态不能为空");
        }

        logger.info("更新企业状态: address={}, status={}", enterpriseAddress, newStatus);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "updateEnterpriseStatus",
                new Object[]{enterpriseAddress, newStatus, "审核通过"},
                "ENTERPRISE_UPDATE_STATUS"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("更新企业状态失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt updateCreditRating(String enterpriseAddress, BigInteger newRating) {
        checkContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newRating == null) {
            throw new IllegalArgumentException("信用评级不能为空");
        }

        logger.info("更新企业信用评级: address={}, rating={}", enterpriseAddress, newRating);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "updateCreditRating",
                new Object[]{enterpriseAddress, newRating, "信用评级更新"},
                "ENTERPRISE_UPDATE_RATING"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("更新企业信用评级失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public TransactionReceipt setCreditLimit(String enterpriseAddress, BigInteger newLimit) {
        checkContract();

        if (enterpriseAddress == null || enterpriseAddress.isBlank()) {
            throw new IllegalArgumentException("企业地址不能为空");
        }
        if (newLimit == null || newLimit.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("授信额度不能为负数");
        }

        logger.info("设置企业授信额度: address={}, limit={}", enterpriseAddress, newLimit);

        TransactionResponse response = sendTransactionWithAudit(
                enterpriseContract,
                "setCreditLimit",
                new Object[]{enterpriseAddress, newLimit},
                "ENTERPRISE_SET_CREDIT_LIMIT"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("设置企业授信额度失败: {}", errorMsg);
            throw new RuntimeException("操作失败，请稍后重试");
        }
        return receipt;
    }

    public static class EnterpriseInfo {
        private String address;
        private String creditCode;
        private BigInteger role;
        private BigInteger status;
        private BigInteger creditLimit;
        private BigInteger creditRating;
        private BigInteger createdAt;
        private byte[] metadataHash;

        public EnterpriseInfo(
                org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple8<
                        String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, byte[]> tuple) {
            this.address = tuple.getValue1();
            this.creditCode = tuple.getValue2() != null ? tuple.getValue2().toString() : null;
            this.role = tuple.getValue3();
            this.status = tuple.getValue4();
            this.creditLimit = tuple.getValue5();
            this.creditRating = tuple.getValue6();
            this.createdAt = tuple.getValue7();
            this.metadataHash = tuple.getValue8();
        }

        public String getAddress() { return address; }
        public String getCreditCode() { return creditCode; }
        public BigInteger getRole() { return role; }
        public BigInteger getStatus() { return status; }
        public BigInteger getCreditLimit() { return creditLimit; }
        public BigInteger getCreditRating() { return creditRating; }
        public BigInteger getCreatedAt() { return createdAt; }
        public byte[] getMetadataHash() { return metadataHash; }
    }

    /**
     * 将 byte 数组 pad 到 32 字节（FISCO SDK Bytes32 要求恰好 32 字节）
     */
    private byte[] padTo32Bytes(byte[] data) {
        if (data == null) {
            return new byte[32];
        }
        if (data.length == 32) {
            return data;
        }
        byte[] padded = new byte[32];
        System.arraycopy(data, 0, padded, 32 - data.length, data.length);
        return padded;
    }
}
