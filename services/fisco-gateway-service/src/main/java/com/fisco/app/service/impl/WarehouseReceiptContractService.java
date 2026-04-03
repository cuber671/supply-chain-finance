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
                    client,
                    cryptoKeyPair
            );
            logger.info("仓单核心合约加载成功，地址: {}", warehouseCoreAddress);
        } else {
            logger.warn("仓单核心合约地址未配置");
        }
        if (warehouseOpsAddress != null && !warehouseOpsAddress.isEmpty()) {
            this.warehouseOpsContract = WarehouseReceiptOps.load(
                    warehouseOpsAddress,
                    client,
                    cryptoKeyPair
            );
            logger.info("仓单运营合约加载成功，地址: {}", warehouseOpsAddress);
        } else {
            logger.warn("仓单运营合约地址未配置");
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

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends org.fisco.bcos.sdk.v3.contract.Contract> T loadContract(String contractAddress) {
        return (T) WarehouseReceiptCore.load(contractAddress, client, cryptoKeyPair);
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

        logger.info("签发仓单: receiptId={}, warehouseHash={}, weight={}", receiptId, warehouseHash, weight);

        // Call contract method directly
        TransactionReceipt receipt = warehouseCoreContract.issueReceipt(input);

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("签发仓单失败: {}", errorMsg);
            throw new RuntimeException("链上交易失败: " + errorMsg);
        }
        return receipt;
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
                "transferReceipt",
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

    public TransactionReceipt splitReceipt(
            String originalReceiptId,
            List<String> newReceiptIds,
            List<BigInteger> weights,
            List<byte[]> ownerHashes,
            String unit) {
        checkCoreContract();

        if (newReceiptIds == null || newReceiptIds.isEmpty()) {
            throw new IllegalArgumentException("新仓单ID列表不能为空");
        }
        if (weights == null || weights.size() != newReceiptIds.size()) {
            throw new IllegalArgumentException("重量列表长度必须与新仓单ID列表一致");
        }
        if (ownerHashes == null) {
            ownerHashes = new ArrayList<>();
        }
        while (ownerHashes.size() < newReceiptIds.size()) {
            ownerHashes.add(new byte[32]);
        }

        SplitInput input = new SplitInput(
                originalReceiptId,
                newReceiptIds,
                weights,
                ownerHashes,
                unit != null ? unit : "吨"
        );

        logger.info("拆分仓单: original={}, newCount={}", originalReceiptId, newReceiptIds.size());

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
