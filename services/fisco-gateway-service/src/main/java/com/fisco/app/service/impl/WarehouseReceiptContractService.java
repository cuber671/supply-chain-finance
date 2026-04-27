package com.fisco.app.service.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.service.BaseContractService;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore.ReceiptInput;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore.MergeInput;
import com.fisco.app.contract.warehouse.WarehouseReceiptCore.SplitInput;
import com.fisco.app.contract.warehouse.WarehouseReceiptOps;
import com.fisco.app.contract.warehouse.WarehouseReceiptOps.EndorsementInput;

/**
 * 仓单上链服务
 */
@Service
public class WarehouseReceiptContractService extends BaseContractService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseReceiptContractService.class);

    @Value("${contract.addresses.warehouse-core:}")
    private String warehouseCoreAddress;

    @Value("${contract.addresses.warehouse-ops:}")
    private String warehouseOpsAddress;

    private WarehouseReceiptCore warehouseCoreContract;
    private WarehouseReceiptOps warehouseOpsContract;

    @javax.annotation.PostConstruct
    public void init() {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，仓单合约服务不可用");
            return;
        }
        if (client == null || cryptoKeyPair == null) {
            logger.error("区块链客户端未初始化，无法加载仓单合约");
            return;
        }
        if (warehouseCoreAddress != null && !warehouseCoreAddress.isEmpty()) {
            this.warehouseCoreContract = WarehouseReceiptCore.load(
                    warehouseCoreAddress,
                    client
            );
            logger.info("仓单核心合约加载成功，地址: {}", warehouseCoreAddress);
        } else {
            logger.warn("仓单核心合约地址未配置");
        }
        if (warehouseOpsAddress != null && !warehouseOpsAddress.isEmpty()) {
            this.warehouseOpsContract = WarehouseReceiptOps.load(
                    warehouseOpsAddress,
                    client
            );
            logger.info("仓单运营合约加载成功，地址: {}", warehouseOpsAddress);
        } else {
            logger.warn("仓单运营合约地址未配置");
        }

        // 【诊断】检查合约的 admin 和 javaBackend 设置
        initializeContractSettings();
    }

    private void initializeContractSettings() {
        if (warehouseCoreContract == null) {
            logger.warn("仓单核心合约未初始化，跳过配置检查");
            return;
        }

        try {
            // 查询当前 admin 设置
            String currentAdmin = warehouseCoreContract.admin();
            String gatewayAddress = cryptoKeyPair.getAddress();
            logger.info("========================================");
            logger.info("仓单合约诊断信息:");
            logger.info("  合约地址: {}", warehouseCoreAddress);
            logger.info("  当前 admin: {}", currentAdmin);
            logger.info("  网关地址: {}", gatewayAddress);
            logger.info("  admin == 网关: {}", gatewayAddress.equalsIgnoreCase(currentAdmin));
            logger.info("========================================");

            // 如果 admin 不匹配网关地址，记录警告
            if (!gatewayAddress.equalsIgnoreCase(currentAdmin)) {
                logger.warn("admin 与网关地址不匹配！只有 admin 才能执行 issueReceipt 等管理操作");
                logger.warn("请使用控制台调用 setAdmin 解决这个问题");
            }
        } catch (ContractException e) {
            logger.error("查询仓单合约配置失败: {}", e.getMessage());
        }
    }

    private void checkCoreContract() {
        if (warehouseCoreContract == null) {
            throw new RuntimeException("仓单核心合约未初始化，请检查区块链连接");
        }
    }

    private void checkOpsContract() {
        if (warehouseOpsContract == null) {
            throw new RuntimeException("仓单运营合约未初始化，请检查区块链连接");
        }
    }

    /**
     * 将 byte[] 转换为十六进制字符串，用于日志输出
     */
    private String bytesToHexString(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 8); i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        if (bytes.length > 8) sb.append("...");
        return "0x" + sb.toString();
    }

    /**
     * 将十六进制字符串转换为 byte[]
     */
    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[32];
        String clean = hex.startsWith("0x") ? hex.substring(2) : hex;
        if (clean.length() % 2 != 0) clean = "0" + clean;
        byte[] bytes = new byte[clean.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends org.fisco.bcos.sdk.v3.contract.Contract> T loadContract(String contractAddress) {
        return (T) WarehouseReceiptCore.load(contractAddress, client);
    }

    public ReceiptInfo getReceipt(String receiptId) throws ContractException {
        checkCoreContract();
        logger.debug("查询仓单信息: {}", receiptId);
        var result = warehouseCoreContract.getReceipt(receiptId);
        return new ReceiptInfo(result);
    }

    @SuppressWarnings("unchecked")
    public List<String> getReceiptIdsByOwner(String owner, BigInteger offset, BigInteger limit) {
        checkCoreContract();
        logger.debug("查询所有者仓单列表: owner={}, offset={}, limit={}", owner, offset, limit);
        try {
            return (List<String>) (List<?>)
                warehouseCoreContract.getReceiptIdsByOwner(owner, offset, limit);
        } catch (ContractException e) {
            logger.error("查询所有者仓单列表失败", e);
            return List.of();
        }
    }

    public TransactionReceipt issueReceipt(
            String receiptId,
            byte[] ownerHash,
            byte[] warehouseHash,
            byte[] goodsDetailHash,
            byte[] locationPhotoHash,
            byte[] contractHash,
            BigInteger weight,
            String unit,
            BigInteger quantity,
            BigInteger storageDate,
            BigInteger expiryDate) {
        checkCoreContract();

        ReceiptInput input = new ReceiptInput(
                receiptId,
                ownerHash != null ? ownerHash : new byte[32],
                warehouseHash != null ? warehouseHash : new byte[32],
                goodsDetailHash != null ? goodsDetailHash : new byte[32],
                locationPhotoHash != null ? locationPhotoHash : new byte[32],
                contractHash != null ? contractHash : new byte[32],
                weight,
                unit != null ? unit : "吨",
                quantity != null ? quantity : BigInteger.ONE,
                storageDate != null ? storageDate : BigInteger.ZERO,
                expiryDate != null ? expiryDate : BigInteger.ZERO
        );

        // 【诊断】详细日志
        logger.info("签发仓单参数: receiptId={}, ownerHash={}, warehouseHash={}",
            receiptId,
            ownerHash != null ? bytesToHexString(ownerHash) : "null",
            warehouseHash != null ? bytesToHexString(warehouseHash) : "null");
        logger.info("签发仓单参数: weight={}, unit={}, quantity={}, storageDate={}, expiryDate={}",
            weight, unit, quantity, storageDate, expiryDate);
        logger.info("签发仓单参数: goodsDetailHash={}, locationPhotoHash={}, contractHash={}",
            goodsDetailHash != null ? bytesToHexString(goodsDetailHash) : "null",
            locationPhotoHash != null ? bytesToHexString(locationPhotoHash) : "null",
            contractHash != null ? bytesToHexString(contractHash) : "null");

        // Call contract method directly
        TransactionReceipt receipt = warehouseCoreContract.issueReceipt(input);

        // 【诊断】详细记录交易收据信息
        logger.info("【交易诊断】issueReceipt 交易完成: hash={}, status={}, block={}",
            receipt.getTransactionHash(), receipt.getStatus(), receipt.getBlockNumber());
        logger.info("【交易诊断】receipt.getMessage()={}, isStatusOK={}",
            receipt.getMessage(), receipt.isStatusOK());
        logger.info("【交易诊断】receipt.getOutput()={}", receipt.getOutput());

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("签发仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    // ==================== String 参数重载（供 Controller 调用） ====================

    public TransactionReceipt issueReceipt(
            String receiptId,
            String ownerHash,
            String warehouseHash,
            String goodsDetailHash,
            String locationPhotoHash,
            String contractHash,
            BigInteger weight,
            String unit,
            BigInteger quantity,
            Long storageDate,
            Long expiryDate) {
        return issueReceipt(
                receiptId,
                hexToBytes(ownerHash),
                hexToBytes(warehouseHash),
                hexToBytes(goodsDetailHash),
                hexToBytes(locationPhotoHash),
                hexToBytes(contractHash),
                weight,
                unit,
                quantity,
                storageDate != null ? BigInteger.valueOf(storageDate) : null,
                expiryDate != null ? BigInteger.valueOf(expiryDate) : null
        );
    }

    public TransactionReceipt launchEndorsement(String receiptId, String fromHash, String toHash) {
        return launchEndorsement(receiptId, hexToBytes(fromHash), hexToBytes(toHash));
    }

    public TransactionReceipt confirmEndorsement(String receiptId, String fromHash, String toHash) {
        return confirmEndorsement(receiptId, hexToBytes(fromHash), hexToBytes(toHash));
    }

    public TransactionReceipt splitReceipt(
            String originalReceiptId,
            List<String> newReceiptIds,
            List<Long> weights,
            List<String> ownerHashes,
            List<String> warehouseHashes,
            String unit) {
        checkCoreContract();

        if (newReceiptIds == null || newReceiptIds.isEmpty()) {
            throw new IllegalArgumentException("新仓单ID列表不能为空");
        }
        if (weights == null || weights.size() != newReceiptIds.size()) {
            throw new IllegalArgumentException("重量列表长度必须与新仓单ID列表一致");
        }

        List<BigInteger> bigWeights = weights.stream()
                .map(w -> BigInteger.valueOf(w == null ? 0L : w))
                .collect(java.util.stream.Collectors.toList());

        List<byte[]> bytesOwnerHashes = new ArrayList<>();
        List<String> safeOwnerHashes = ownerHashes != null ? ownerHashes : new ArrayList<>();
        for (int i = 0; i < newReceiptIds.size(); i++) {
            bytesOwnerHashes.add(i < safeOwnerHashes.size()
                    ? hexToBytes(safeOwnerHashes.get(i)) : new byte[32]);
        }

        List<byte[]> bytesWarehouseHashes = new ArrayList<>();
        List<String> safeWarehouseHashes = warehouseHashes != null ? warehouseHashes : new ArrayList<>();
        for (int i = 0; i < newReceiptIds.size(); i++) {
            bytesWarehouseHashes.add(i < safeWarehouseHashes.size()
                    ? hexToBytes(safeWarehouseHashes.get(i)) : new byte[32]);
        }

        SplitInput input = new SplitInput(
                originalReceiptId,
                newReceiptIds,
                bigWeights,
                bytesOwnerHashes,
                bytesWarehouseHashes,
                unit != null ? unit : "吨"
        );

        logger.info("拆分仓单(String): original={}, newCount={}", originalReceiptId, newReceiptIds.size());

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "splitReceipt",
                new Object[]{input},
                "WAREHOUSE_SPLIT"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("拆分仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt mergeReceipts(List<String> sourceReceiptIds, String targetReceiptId,
            String targetOwnerHash, String unit, Long totalWeight) {
        return mergeReceipts(
                sourceReceiptIds,
                targetReceiptId,
                hexToBytes(targetOwnerHash),
                unit,
                totalWeight != null ? BigInteger.valueOf(totalWeight) : null
        );
    }

    public TransactionReceipt burnReceipt(String receiptId, String signatureHash) {
        return burnReceipt(receiptId, hexToBytes(signatureHash));
    }

    public TransactionReceipt transferReceipt(String receiptId, String newOwnerHash) {
        return transferReceipt(receiptId, hexToBytes(newOwnerHash));
    }

    public TransactionReceipt launchEndorsement(String receiptId, byte[] fromHash, byte[] toHash) {
        checkOpsContract();

        EndorsementInput input = new EndorsementInput(
                receiptId,
                fromHash != null ? fromHash : new byte[32],
                toHash != null ? toHash : new byte[32],
                "STANDARD"
        );

        logger.info("发起仓单背书: receiptId={}", receiptId);

        // Call contract method directly
        TransactionReceipt receipt = warehouseOpsContract.launchEndorsement(input);

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("发起背书失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt confirmEndorsement(String receiptId, byte[] fromHash, byte[] toHash) {
        checkCoreContract();

        logger.info("确认仓单背书: receiptId={}, toHash={}", receiptId, toHash);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "updateOwner",
                new Object[]{receiptId, toHash != null ? toHash : new byte[32]},
                "WAREHOUSE_CONFIRM_ENDORSEMENT"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("确认背书失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt mergeReceipts(List<String> sourceReceiptIds, String targetReceiptId,
            byte[] targetOwnerHash, String unit, BigInteger totalWeight) {
        checkCoreContract();

        if (sourceReceiptIds == null || sourceReceiptIds.isEmpty()) {
            throw new IllegalArgumentException("源仓单ID列表不能为空");
        }

        MergeInput input = new MergeInput(
                sourceReceiptIds,
                targetReceiptId,
                targetOwnerHash != null ? targetOwnerHash : new byte[32],
                unit != null ? unit : "吨",
                totalWeight != null ? totalWeight : BigInteger.ZERO
        );

        logger.info("合并仓单: sources={}, target={}", sourceReceiptIds, targetReceiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "mergeReceipts",
                new Object[]{input},
                "WAREHOUSE_MERGE"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("合并仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt lockReceipt(String receiptId) {
        checkCoreContract();

        logger.info("质押仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "lockReceipt",
                new Object[]{receiptId},
                "WAREHOUSE_LOCK"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("质押仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt unlockReceipt(String receiptId) {
        checkCoreContract();

        logger.info("解除质押仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "unlockReceipt",
                new Object[]{receiptId},
                "WAREHOUSE_UNLOCK"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("解除质押失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt setInTransit(String receiptId) {
        checkCoreContract();

        logger.info("设置仓单为物流转运中: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "setInTransit",
                new Object[]{receiptId},
                "WAREHOUSE_SET_IN_TRANSIT"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("设置仓单为转运中失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt restoreFromTransit(String receiptId) {
        checkCoreContract();

        logger.info("仓单从转运中恢复到在库: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "restoreFromTransit",
                new Object[]{receiptId},
                "WAREHOUSE_RESTORE_FROM_TRANSIT"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("仓单从转运中恢复失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt burnReceipt(String receiptId, byte[] signatureHash) {
        checkCoreContract();

        logger.info("核销仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "burnReceipt",
                new Object[]{receiptId, signatureHash != null ? signatureHash : new byte[32]},
                "WAREHOUSE_BURN"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("核销仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt transferReceipt(String receiptId, byte[] newOwnerHash) {
        checkCoreContract();

        logger.info("转让仓单: receiptId={}", receiptId);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "transferReceipt",
                new Object[]{receiptId, newOwnerHash != null ? newOwnerHash : new byte[32]},
                "WAREHOUSE_TRANSFER"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("转让仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt cancelReceipt(String receiptId, String reason) {
        checkCoreContract();

        logger.info("取消仓单: receiptId={}, reason={}", receiptId, reason);

        TransactionResponse response = sendTransactionWithAudit(
                warehouseCoreContract,
                "cancelReceipt",
                new Object[]{receiptId, reason},
                "WAREHOUSE_CANCEL"
        );

        TransactionReceipt receipt = response != null ? response.getTransactionReceipt() : null;
        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("取消仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
    }

    public static class ReceiptInfo {
        private String receiptId;
        private byte[] ownerHash;
        private byte[] warehouseHash;
        private BigInteger status;
        private String warehouse;
        private BigInteger weight;
        private BigInteger createTime;

        public ReceiptInfo(
                org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple7<
                        String, byte[], byte[], BigInteger, String, BigInteger, BigInteger> tuple) {
            this.receiptId = tuple.getValue1();
            this.ownerHash = tuple.getValue2();
            this.warehouseHash = tuple.getValue3();
            this.status = tuple.getValue4();
            this.warehouse = tuple.getValue5();
            this.weight = tuple.getValue6();
            this.createTime = tuple.getValue7();
        }

        public String getReceiptId() { return receiptId; }
        public byte[] getOwnerHash() { return ownerHash; }
        public byte[] getWarehouseHash() { return warehouseHash; }
        public BigInteger getStatus() { return status; }
        public String getWarehouse() { return warehouse; }
        public BigInteger getWeight() { return weight; }
        public BigInteger getCreateTime() { return createTime; }
    }
}
