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

        // 初始化合约配置：设置 LoanRepayment 地址和 javaBackend
        initializeContractSettings();
    }

    /**
     * 初始化合约配置
     * 确保 LoanRepayment 合约地址和 javaBackend 已正确设置
     */
    private void initializeContractSettings() {
        if (loanCoreContract == null) {
            logger.warn("LoanCore 合约未初始化，跳过配置初始化");
            return;
        }

        try {
            // 检查当前 javaBackend 设置
            String currentBackend = loanCoreContract.javaBackend();
            String gatewayAddress = cryptoKeyPair.getAddress();
            logger.info("当前 javaBackend: {}, 网关地址: {}", currentBackend, gatewayAddress);

            // 如果 javaBackend 不是网关地址，则设置为网关地址
            if (!gatewayAddress.equalsIgnoreCase(currentBackend)) {
                logger.info("设置 javaBackend 为网关地址: {}", gatewayAddress);
                TransactionReceipt receipt = loanCoreContract.setJavaBackend(gatewayAddress);
                if (isTransactionSuccess(receipt)) {
                    logger.info("javaBackend 设置成功");
                } else {
                    logger.error("javaBackend 设置失败: {}", getTransactionErrorMessage(receipt));
                }
            }

            // 检查并设置 loanRepayment 地址
            if (loanRepaymentContract != null) {
                String currentLoanRepayment = loanCoreContract.loanRepayment();
                String repaymentAddress = loanRepaymentContract.getContractAddress();
                logger.info("当前 loanRepayment: {}, 合约地址: {}", currentLoanRepayment, repaymentAddress);

                if (!repaymentAddress.equalsIgnoreCase(currentLoanRepayment)) {
                    logger.info("设置 loanRepayment 地址: {}", repaymentAddress);
                    TransactionReceipt receipt = loanCoreContract.setLoanRepayment(repaymentAddress);
                    if (isTransactionSuccess(receipt)) {
                        logger.info("loanRepayment 设置成功");
                    } else {
                        logger.error("loanRepayment 设置失败: {}", getTransactionErrorMessage(receipt));
                    }
                }
            }
        } catch (ContractException e) {
            logger.warn("初始化合约配置失败: {}", e.getMessage());
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
            String financeEntHash,
            BigInteger interestRate,
            BigInteger amount,
            BigInteger loanDays,
            String receiptId,
            BigInteger pledgeAmount) {
        checkCoreContract();

        logger.info("创建贷款上链: loanNo={}, borrowerHash={}, amount={}", loanNo, borrowerHash, amount);

        // Convert entity IDs to bytes32 (numeric IDs like "123456" not hex)
        byte[] borrowerHashBytes = entityIdToBytes32(borrowerHash);
        byte[] financeEntHashBytes = entityIdToBytes32(financeEntHash);
        byte[] dataHashBytes = new byte[32]; // dataHash, padded to 32 bytes

        // Call contract method directly
        TransactionReceipt receipt = loanCoreContract.createLoan(
                loanNo, borrowerHashBytes, financeEntHashBytes,
                receiptId, amount, interestRate, loanDays, dataHashBytes);

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

        // Call contract method directly with dataHash
        byte[] dataHash = new byte[32];
        TransactionReceipt receipt = loanCoreContract.approveLoan(
                loanNo, approvedAmount, interestRate, loanDays, dataHash);

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

        // Call contract method directly
        TransactionReceipt receipt = loanCoreContract.cancelLoan(loanNo, reason);

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

        // Call contract method directly
        TransactionReceipt receipt = loanCoreContract.disburseLoan(
                loanNo, approvedAmount, startDate, endDate, receiptId);

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("放款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }
        return receipt;
    }

    public TransactionReceipt recordRepayment(String loanNo, BigInteger amount, BigInteger installmentIndex) {
        return recordRepayment(loanNo, amount, BigInteger.ZERO, installmentIndex);
    }

    public TransactionReceipt recordRepayment(String loanNo, BigInteger amount, BigInteger interestAmount, BigInteger installmentIndex) {
        checkRepaymentContract();
        checkCoreContract();

        logger.info("记录还款上链: loanNo={}, amount={}, interestAmount={}, installmentIndex={}",
                loanNo, amount, interestAmount, installmentIndex);

        // amount 是本金，interestAmount 是利息（分别从 finance-service 传递）
        BigInteger principal = amount;
        BigInteger interest = interestAmount;
        BigInteger penalty = BigInteger.ZERO;
        byte[] dataHash = new byte[32];

        logger.info("还款参数: principal={}, interest={}, penalty={}", principal, interest, penalty);

        // 先记录到 LoanRepayment 合约
        TransactionReceipt receipt = loanRepaymentContract.recordCashRepayment(
                new com.fisco.app.contract.loan.LoanRepayment.CashRepaymentInput(
                        loanNo, loanNo, principal, interest, penalty, dataHash));

        if (!isTransactionSuccess(receipt)) {
            String errorMsg = getTransactionErrorMessage(receipt);
            logger.error("记录还款失败: {}", errorMsg);
            throw new RuntimeException("操作失败: " + errorMsg);
        }

        // 同步更新 LoanCore 合约状态
        // 【YX-02修复】增加重试逻辑，使用指数退避处理临时性EVM错误
        // 注意: loanCoreContract.recordRepayment() 在某些部署环境下可能失败 (Status:16)
        // 这可能是由于ABI/字节码不匹配或EVM执行问题
        // 即使LoanCore同步失败,还款仍记录在LoanRepayment合约中
        logger.info("同步更新 LoanCore 状态: loanNo={}", loanNo);
        boolean syncSuccess = false;
        String lastErrorMsg = null;
        int maxRetries = 3;
        long baseDelay = 1000; // 基础延迟1秒

        for (int attempt = 1; attempt <= maxRetries && !syncSuccess; attempt++) {
            try {
                TransactionReceipt coreReceipt = loanCoreContract.recordRepayment(
                        loanNo, principal, interest, penalty, "STANDARD");

                if (!isTransactionSuccess(coreReceipt)) {
                    lastErrorMsg = getTransactionErrorMessage(coreReceipt);
                    logger.warn("同步 LoanCore 还款状态失败 (尝试 {}/{}): loanNo={}, error={}",
                            attempt, maxRetries, loanNo, lastErrorMsg);
                    // 指数退避延迟
                    if (attempt < maxRetries) {
                        long delay = baseDelay * (long) Math.pow(2, attempt - 1);
                        logger.info("等待 {}ms 后重试...", delay);
                        Thread.sleep(delay);
                    }
                } else {
                    syncSuccess = true;
                    logger.info("同步 LoanCore 还款状态成功: loanNo={}", loanNo);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                lastErrorMsg = "重试被中断";
                break;
            } catch (Exception e) {
                lastErrorMsg = e.getMessage();
                logger.warn("同步 LoanCore 还款状态异常 (尝试 {}/{}): loanNo={}, error={}",
                        attempt, maxRetries, loanNo, e.getMessage());
                if (attempt < maxRetries) {
                    long delay = baseDelay * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 如果同步失败，记录ERROR日志和审计追踪
        if (!syncSuccess) {
            // 【YX-02修复】使用ERROR级别日志，记录完整信息供补偿任务处理
            // 补偿任务应定期扫描ERROR日志来处理这类情况
            logger.error("【严重】LoanCore还款同步失败，需要人工/补偿任务介入: " +
                    "loanNo={}, principal={}, interest={}, penalty={}, " +
                    "LoanRepayment合约已成功记录, LoanCore同步失败, " +
                    "最后错误={}, 重试次数={}",
                    loanNo, principal, interest, penalty, lastErrorMsg, maxRetries);
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

        // Call contract method directly
        TransactionReceipt receipt = loanCoreContract.markOverdue(
                loanNo, overdueDays, penaltyRate, penaltyAmount);

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

        // Call contract method directly
        TransactionReceipt receipt = loanCoreContract.markDefaulted(
                loanNo, disposalMethod, disposalAmount);

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

        // Call contract method directly
        TransactionReceipt receipt = warehouseCoreExtContract.setReceiptLoanId(
                receiptId, loanNo, pledgeAmount);

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

        // Call contract method directly
        TransactionReceipt receipt = warehouseCoreExtContract.updateReceiptLoanId(receiptId, newLoanNo);

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

    /**
     * Convert hex string to byte array (for bytes32 type)
     * Handles both "0x..." hex format and plain hex strings
     *
     * 【QS-06修复】空hex字符串必须抛出异常而非返回全零bytes32
     * 全零bytes32会导致链上ID异常（如仓单ID为全零无法识别）
     */
    private byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            // G4: 空 hex 字符串返回全零会导致链上 ID 异常
            // 修复：抛出异常而非静默返回全零
            throw new IllegalArgumentException("hex 字符串不能为空");
        }
        // Strip 0x prefix if present
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        // Pad to 32 bytes (for bytes32 Solidity type)
        byte[] result = new byte[32];
        byte[] hexBytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length() / 2; i++) {
            hexBytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        System.arraycopy(hexBytes, 0, result, 32 - hexBytes.length, hexBytes.length);
        return result;
    }

    /**
     * Convert entity ID string to bytes32 for blockchain storage.
     * Handles numeric entity IDs (decimal string like "123456") by interpreting
     * them as decimal numbers, not hex strings.
     *
     * Example: entityId="123456" → bytes32 representing decimal 123456
     *          (not hex interpretation which would give [0x12, 0x34, 0x56])
     *
     * 【QS-06修复】空entity ID必须抛出异常而非返回全零bytes32
     */
    private byte[] entityIdToBytes32(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            // G4: 空 entity ID 返回全零会导致链上 ID 异常
            // 修复：抛出异常而非静默返回全零
            throw new IllegalArgumentException("entity ID 不能为空");
        }
        try {
            BigInteger bigInt = new BigInteger(entityId);
            byte[] valueBytes = bigInt.toByteArray();
            byte[] result = new byte[32];
            // BigInteger.toByteArray() returns sign+magnitude
            // If the first byte is 0, it means the actual data starts from index 1
            int start = (valueBytes[0] == 0) ? 1 : 0;
            int len = valueBytes.length - start;
            // Copy to the rightmost position of the 32-byte array
            if (len > 0) {
                System.arraycopy(valueBytes, start, result, 32 - len, len);
            }
            return result;
        } catch (NumberFormatException e) {
            // If not a valid number, treat as hex string
            return hexStringToBytes(entityId);
        }
    }
}
