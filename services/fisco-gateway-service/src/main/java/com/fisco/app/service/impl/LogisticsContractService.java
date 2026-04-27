package com.fisco.app.service.impl;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.service.BaseContractService;
import com.fisco.app.contract.logistics.LogisticsCore;
import com.fisco.app.contract.logistics.LogisticsOps;

/**
 * 物流合约服务
 *
 * 【QS-04修复说明】物流合约(LogisticsCore)设计为极简存储：
 * - 链上仅存储 status(uint8)，记录物流状态流转
 * - 其他业务数据(ownerHash, carrierHash, receiptId, quantity等)存储在Java DB的LogisticsDelegate表中
 *
 * 设计取舍：
 * - 优点：合约极简，存储成本低，状态明确
 * - 缺点：链上数据不完整，无法仅通过链上数据还原完整业务上下文
 *
 * Java层补偿措施：
 * - 所有上链操作前先写入Java DB，确保数据一致性
 * - 链上status变更时，Java层同步更新DB中的status
 * - 通过voucherNo作为关联键，Java DB可查询完整业务数据
 *
 * 如需完整链上数据，需重新部署物流合约（不在本次修复范围内）
 */
@Service
public class LogisticsContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(LogisticsContractService.class);

    @Value("${contract.logistics-core:}")
    private String logisticsCoreAddress;

    @Value("${contract.logistics-ops:}")
    private String logisticsOpsAddress;

    private LogisticsCore logisticsCore;
    private LogisticsOps logisticsOps;

    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，物流合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载物流合约");
            return;
        }

        if (logisticsCoreAddress != null && !logisticsCoreAddress.isEmpty()) {
            this.logisticsCore = LogisticsCore.load(logisticsCoreAddress, client);
            logger.info("物流核心合约加载成功，地址: {}", logisticsCoreAddress);
        } else {
            logger.warn("物流核心合约地址未配置");
        }

        if (logisticsOpsAddress != null && !logisticsOpsAddress.isEmpty()) {
            this.logisticsOps = LogisticsOps.load(logisticsOpsAddress, client);
            logger.info("物流操作合约加载成功，地址: {}", logisticsOpsAddress);
        } else {
            logger.warn("物流操作合约地址未配置");
        }
    }

    private void checkCoreContract() {
        if (logisticsCore == null) {
            throw new RuntimeException("物流核心合约未初始化，请检查区块链连接");
        }
    }

    private void checkOpsContract() {
        if (logisticsOps == null) {
            throw new RuntimeException("物流操作合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 将实体ID（十进制数字字符串）转换为32字节数组
     */
    protected byte[] entityIdToBytes32(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return new byte[32];
        }
        try {
            byte[] idBytes = entityId.getBytes();
            byte[] result = new byte[32];
            System.arraycopy(idBytes, 0, result, 0, Math.min(idBytes.length, 32));
            return result;
        } catch (Exception e) {
            logger.error("entityIdToBytes32 转换失败: {}", entityId, e);
            return new byte[32];
        }
    }

    /**
     * 将十六进制字符串转换为 byte[]
     */
    protected byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[32];
        }
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((Character.digit(hex.charAt(i * 2), 16) << 4)
                    + Character.digit(hex.charAt(i * 2 + 1), 16));
        }
        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends org.fisco.bcos.sdk.v3.contract.Contract> T loadContract(String contractAddress) {
        return (T) LogisticsCore.load(contractAddress, client);
    }

    public TransactionReceipt createLogisticsDelegate(
            String voucherNo,
            int businessScene,
            String receiptId,
            BigInteger transportQuantity,
            String unit,
            byte[] ownerHash,
            byte[] carrierHash,
            byte[] sourceWhHash,
            byte[] targetWhHash,
            BigInteger validUntil) {
        checkCoreContract();

        logger.info("链上创建物流委派单: voucherNo={}, businessScene={}", voucherNo, businessScene);

        // 直接调用合约的三数组参数方法
        TransactionReceipt receipt = logisticsCore.createLogisticsDelegate(
                Arrays.asList(voucherNo, receiptId, unit),
                Arrays.asList(BigInteger.valueOf(businessScene), transportQuantity, validUntil),
                Arrays.asList(ownerHash, carrierHash, sourceWhHash, targetWhHash)
        );

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上创建物流委派单失败: {}", errorMsg);
            throw new RuntimeException("链上创建物流委派单失败: " + errorMsg);
        }

        logger.info("链上创建物流委派单成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    // ==================== String 参数重载方法 ====================

    /**
     * 创建物流委托单（接收 String 参数，内部进行类型转换）
     * ownerHash, carrierHash, sourceWhHash, targetWhHash 是 entity ID，使用 entityIdToBytes32 转换
     */
    public TransactionReceipt createLogisticsDelegate(
            String voucherNo,
            int businessScene,
            String receiptId,
            Long transportQuantity,
            String unit,
            String ownerHash,
            String carrierHash,
            String sourceWhHash,
            String targetWhHash,
            Long validUntil) {
        return createLogisticsDelegate(
                voucherNo,
                businessScene,
                receiptId,
                transportQuantity != null ? BigInteger.valueOf(transportQuantity) : null,
                unit,
                entityIdToBytes32(ownerHash),
                entityIdToBytes32(carrierHash),
                entityIdToBytes32(sourceWhHash),
                entityIdToBytes32(targetWhHash),
                validUntil != null ? BigInteger.valueOf(validUntil) : null
        );
    }

    public TransactionReceipt pickup(String voucherNo, BigInteger quantity) {
        checkCoreContract();

        logger.info("链上提货确认: voucherNo={}, quantity={}", voucherNo, quantity);

        Function function = new Function(
                "pickup",
                Arrays.asList(new Utf8String(voucherNo), new Uint256(quantity)),
                Collections.emptyList());

        TransactionReceipt receipt = executeTransaction(logisticsCore, function);

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上提货确认失败: {}", errorMsg);
            throw new RuntimeException("链上提货确认失败: " + errorMsg);
        }

        logger.info("链上提货确认成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    public TransactionReceipt arriveAndAddQuantity(String voucherNo, String targetReceiptId, BigInteger quantity) {
        checkCoreContract();

        logger.info("链上到货增加数量: voucherNo={}, targetReceiptId={}, quantity={}",
                voucherNo, targetReceiptId, quantity);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "arriveAndAddQuantity",
                new Object[]{voucherNo, targetReceiptId, quantity},
                "LOGISTICS_ARRIVE_ADD"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上到货增加数量失败: {}", errorMsg);
            throw new RuntimeException("链上到货增加数量失败: " + errorMsg);
        }

        logger.info("链上到货增加数量成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    public TransactionReceipt arriveAndCreateReceipt(
            String voucherNo,
            String newReceiptId,
            BigInteger weight,
            String unit,
            byte[] ownerHash,
            byte[] warehouseHash) {
        checkCoreContract();

        logger.info("链上到货创建仓单: voucherNo={}, newReceiptId={}, weight={}",
                voucherNo, newReceiptId, weight);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "arriveAndCreateReceipt",
                new Object[]{voucherNo, newReceiptId, weight, unit, ownerHash, warehouseHash},
                "LOGISTICS_ARRIVE_CREATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上到货创建仓单失败: {}", errorMsg);
            throw new RuntimeException("链上到货创建仓单失败: " + errorMsg);
        }

        logger.info("链上到货创建仓单成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    /**
     * 到货创建仓单（接收 String 参数，内部进行类型转换）
     * ownerHash 和 warehouseHash 是 entity ID，使用 entityIdToBytes32 转换
     */
    public TransactionReceipt arriveAndCreateReceipt(
            String voucherNo,
            String newReceiptId,
            Long weight,
            String unit,
            String ownerHash,
            String warehouseHash) {
        return arriveAndCreateReceipt(
                voucherNo,
                newReceiptId,
                weight != null ? BigInteger.valueOf(weight) : null,
                unit,
                entityIdToBytes32(ownerHash),
                entityIdToBytes32(warehouseHash)
        );
    }

    /**
     * 分配承运人（接收 String 参数，内部进行类型转换）
     * carrierHash 是 entity ID，使用 entityIdToBytes32 转换
     */
    public TransactionReceipt assignCarrier(String voucherNo, String carrierHash) {
        return assignCarrier(voucherNo, entityIdToBytes32(carrierHash));
    }

    public TransactionReceipt assignCarrier(String voucherNo, byte[] carrierHash) {
        checkCoreContract();

        logger.info("链上分配承运人: voucherNo={}", voucherNo);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "assignCarrier",
                new Object[]{voucherNo, carrierHash},
                "LOGISTICS_ASSIGN"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上分配承运人失败: {}", errorMsg);
            throw new RuntimeException("链上分配承运人失败: " + errorMsg);
        }

        logger.info("链上分配承运人成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    public TransactionReceipt confirmDelivery(String voucherNo, int action, String targetReceiptId) {
        checkCoreContract();

        logger.info("链上确认交付: voucherNo={}, action={}, targetReceiptId={}", voucherNo, action, targetReceiptId);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "confirmDelivery",
                new Object[]{voucherNo, BigInteger.valueOf(action), targetReceiptId},
                "LOGISTICS_CONFIRM_DELIVERY"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上确认交付失败: {}", errorMsg);
            throw new RuntimeException("链上确认交付失败: " + errorMsg);
        }

        logger.info("链上确认交付成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    public TransactionReceipt updateStatus(String voucherNo, int newStatus) {
        checkCoreContract();

        logger.info("链上更新状态: voucherNo={}, status={}", voucherNo, newStatus);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "updateStatus",
                new Object[]{voucherNo, BigInteger.valueOf(newStatus)},
                "LOGISTICS_UPDATE_STATUS"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上更新状态失败: {}", errorMsg);
            throw new RuntimeException("链上更新状态失败: " + errorMsg);
        }

        logger.info("链上更新状态成功: voucherNo={}, status={}, txHash={}",
                voucherNo, newStatus, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }

    @SuppressWarnings("unchecked")
    public List<BigInteger> getLogisticsTrack(String voucherNo) {
        checkOpsContract();

        try {
            return (List<BigInteger>) logisticsOps.getLogisticsTrack(voucherNo);
        } catch (ContractException e) {
            logger.error("获取物流轨迹失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("获取物流轨迹失败: " + e.getMessage(), e);
        }
    }

    public Boolean validateLogisticsDelegate(String voucherNo) {
        checkOpsContract();

        try {
            return logisticsOps.validateLogisticsDelegate(voucherNo);
        } catch (ContractException e) {
            logger.error("验证物流委托失败: voucherNo={}", voucherNo, e);
            throw new RuntimeException("验证物流委托失败: " + e.getMessage(), e);
        }
    }

    public TransactionReceipt invalidate(String voucherNo) {
        checkCoreContract();

        logger.info("链上使委派单失效: voucherNo={}", voucherNo);

        TransactionResponse response = sendTransactionWithAudit(
                logisticsCore,
                "invalidate",
                new Object[]{voucherNo},
                "LOGISTICS_INVALIDATE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("链上使委派单失效失败: {}", errorMsg);
            throw new RuntimeException("链上使委派单失效失败: " + errorMsg);
        }

        logger.info("链上使委派单失效成功: voucherNo={}, txHash={}",
                voucherNo, receipt != null ? receipt.getTransactionHash() : "N/A");
        return receipt;
    }
}
