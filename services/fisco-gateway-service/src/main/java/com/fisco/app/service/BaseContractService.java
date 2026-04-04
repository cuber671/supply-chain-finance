package com.fisco.app.service;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fisco.app.config.BlockchainConfig;

/**
 * 区块链合约服务基类
 *
 * 提供功能：
 * 1. 合约地址管理（从配置中读取）
 * 2. 统一的交易发送方法（同步/异步）
 * 3. 链上交易审计记录
 * 4. 错误处理和重试机制
 *
 * 使用方式：
 * 1. 子类继承此类并标注 @Service
 * 2. 在 @PostConstruct 中加载合约
 * 3. 直接调用合约方法
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public abstract class BaseContractService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired(required = false)
    protected Client client;

    @Autowired(required = false)
    protected CryptoKeyPair cryptoKeyPair;

    @Autowired(required = false)
    protected BlockchainConfig blockchainConfig;

    @Autowired(required = false)
    protected BlockchainAuditService blockchainAuditService;

    /**
     * 是否启用区块链功能
     */
    @Value("${fisco.enabled:true}")
    protected boolean fiscoEnabled;

    /**
     * 交易重试次数
     */
    @Value("${fisco.retry-times:3}")
    protected int retryTimes;

    /**
     * 交易重试间隔（毫秒）
     */
    @Value("${fisco.retry-delay:1000}")
    protected long retryDelay;

    /**
     * 获取当前账户地址
     */
    protected String getCurrentAccountAddress() {
        return cryptoKeyPair != null ? cryptoKeyPair.getAddress() : null;
    }

    /**
     * 加载合约实例
     * 子类需要实现此方法加载对应的合约
     *
     * @param contractAddress 合约部署地址
     * @param <T> 合约类型
     * @return 合约实例
     */
    protected abstract <T extends Contract> T loadContract(String contractAddress);

    /**
     * 发送交易（同步）- 带审计记录
     *
     * @param contract 合约实例
     * @param functionName 函数名
     * @param params 参数
     * @param operationType 操作类型（用于审计）
     * @return 交易响应
     */
    protected TransactionResponse sendTransactionWithAudit(
            Contract contract,
            String functionName,
            Object[] params,
            String operationType) {

        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，跳过交易: {}", functionName);
            return null;
        }

        int attempt = 0;
        while (attempt < retryTimes) {
            try {
                // 获取合约方法并发送交易
                // 获取合约方法并发送交易
                Object result = invokeContract(contract, functionName, params);
                TransactionResponse response = null;
                if (result instanceof TransactionResponse) {
                    response = (TransactionResponse) result;
                } else if (result instanceof TransactionReceipt) {
                    final TransactionReceipt receipt = (TransactionReceipt) result;
                    response = new TransactionResponse(receipt.getStatus(), receipt.getMessage());
                    response.setTransactionReceipt(receipt);
                }

                // 【P2-8修复】检查区块链响应状态，确保交易真正成功后再记录审计
                // 如果响应为空或状态不为OK，视为失败进行重试
                if (response == null) {
                    throw new ContractException("区块链返回空响应");
                }
                // 检查response的isStatusOK方法（如果SDK支持）
                // 注意：这里通过判断TransactionReceipt的状态来验证
                if (response.getTransactionReceipt() != null && !response.getTransactionReceipt().isStatusOK()) {
                    throw new ContractException("区块链交易失败: " + response.getTransactionReceipt().getMessage());
                }

                // 记录审计日志
                if (blockchainAuditService != null && response != null) {
                    String txHash = response.getTransactionReceipt() != null
                        ? response.getTransactionReceipt().getTransactionHash()
                        : null;
                    if (txHash != null) {
                        blockchainAuditService.recordTransaction(txHash, operationType, contract.getContractAddress());
                        logger.info("交易已记录审计, txHash={}, operation={}", txHash, operationType);
                    }
                }

                return response;

            } catch (Exception e) {
                attempt++;
                logger.warn("交易失败 (尝试 {}/{}): {} - {}",
                    attempt, retryTimes, functionName, e.getMessage());

                if (attempt < retryTimes) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    logger.error("交易最终失败: {}", functionName, e);
                    throw new RuntimeException("区块链交易失败: " + e.getMessage(), e);
                }
            }
        }

        return null;
    }

    /**
     * 发送交易（同步）- 不带审计
     */
    protected TransactionResponse sendTransaction(Contract contract, String functionName, Object[] params) {
        return sendTransactionWithAudit(contract, functionName, params, "CONTRACT_INVOKE");
    }

    /**
     * 使用反射调用私有方法执行动态 Function 交易
     */
    protected TransactionReceipt executeTransaction(Contract contract, Function function) {
        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，跳过交易");
            return null;
        }

        int attempt = 0;
        while (attempt < retryTimes) {
            try {
                // 使用反射调用 Contract 的 executeTransaction 方法
                java.lang.reflect.Method method = Contract.class.getDeclaredMethod(
                        "executeTransaction", Function.class);
                method.setAccessible(true);
                Object result = method.invoke(contract, function);

                if (result instanceof TransactionReceipt) {
                    return (TransactionReceipt) result;
                } else if (result instanceof TransactionResponse) {
                    TransactionResponse response = (TransactionResponse) result;
                    return response.getTransactionReceipt();
                }
                throw new ContractException("未知的返回类型: " + result.getClass().getName());
            } catch (Exception e) {
                attempt++;
                logger.warn("交易失败 (尝试 {}/{}): {}", attempt, retryTimes, e.getMessage());
                if (attempt < retryTimes) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 调用合约方法（底层实现）
     */
    protected Object invokeContract(Contract contract, String functionName, Object[] params)
            throws ContractException {

        logger.debug("调用合约方法: {} ", functionName);

        // 使用反射调用合约的对应方法
        try {
            // 查找并调用方法
            var method = findContractMethod(contract, functionName, params);
            if (method != null) {
                Object result = method.invoke(contract, params);
                // 处理不同的返回类型
                if (result instanceof TransactionResponse) {
                    return (TransactionResponse) result;
                } else if (result instanceof TransactionReceipt) {
                    // Return as-is, let caller handle it
                    return result;
                }
                throw new ContractException("未知的返回类型: " + result.getClass().getName());
            }
            throw new ContractException("未找到合约方法: " + functionName);
        } catch (Exception e) {
            throw new ContractException("合约调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用只读方法（不发送交易）
     */
    protected CallResponse callReadOnlyMethod(Contract contract, String functionName, Object[] params) {

        if (!fiscoEnabled) {
            logger.warn("FISCO BCOS 功能已禁用，跳过查询: {}", functionName);
            return null;
        }

        try {
            logger.debug("调用只读方法: {}", functionName);

            var method = findContractMethod(contract, functionName, params);
            if (method != null) {
                return (CallResponse) method.invoke(contract, params);
            }
            logger.error("未找到只读方法: {}", functionName);
            return null;

        } catch (Exception e) {
            logger.error("只读方法调用失败: {}", functionName, e);
            return null;
        }
    }

    /**
     * 查找合约方法（使用反射）
     * G5: 增加参数类型兼容性校验，避免重载方法被误匹配
     */
    private java.lang.reflect.Method findContractMethod(Contract contract, String functionName, Object[] params) {
        if (params == null || params.length == 0) {
            try {
                return contract.getClass().getMethod(functionName);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        // 根据参数数量查找方法，并校验参数类型兼容性
        for (var method : contract.getClass().getMethods()) {
            if (!method.getName().equals(functionName)) {
                continue;
            }
            if (method.getParameterCount() != params.length) {
                continue;
            }
            // G5: 校验参数类型兼容性，避免重载方法误匹配
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean typeMatch = true;
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null && !paramTypes[i].isAssignableFrom(params[i].getClass())) {
                    typeMatch = false;
                    break;
                }
            }
            if (typeMatch) {
                return method;
            }
        }
        return null;
    }

    /**
     * 检查交易Receipt状态
     */
    protected boolean isTransactionSuccess(TransactionReceipt receipt) {
        if (receipt == null) {
            return false;
        }
        return receipt.isStatusOK();
    }

    /**
     * 获取交易Receipt中的错误信息
     */
    protected String getTransactionErrorMessage(TransactionReceipt receipt) {
        if (receipt == null) {
            return "Receipt is null";
        }
        if (receipt.isStatusOK()) {
            return null;
        }
        return String.format("Status: %s, Error: %s",
            receipt.getStatus(),
            receipt.getMessage() != null ? receipt.getMessage() : "Unknown error");
    }

    /**
     * 格式化地址（确保0x前缀）
     */
    protected String formatAddress(String address) {
        if (address == null) {
            return null;
        }
        return address.startsWith("0x") ? address : "0x" + address;
    }

    /**
     * 解析地址（去除0x前缀用于内部存储）
     */
    protected String parseAddress(String address) {
        if (address == null) {
            return null;
        }
        return address.startsWith("0x") ? address.substring(2) : address;
    }

    /**
     * 格式化金额为 BigInteger（单位：wei）
     */
    protected BigInteger toWei(java.math.BigDecimal amount) {
        if (amount == null) {
            return BigInteger.ZERO;
        }
        return amount.multiply(new java.math.BigDecimal("1000000000000000000")).toBigInteger();
    }

    /**
     * 从 wei 转换为标准单位
     */
    protected java.math.BigDecimal fromWei(BigInteger wei) {
        if (wei == null) {
            return java.math.BigDecimal.ZERO;
        }
        return new java.math.BigDecimal(wei).divide(new java.math.BigDecimal("1000000000000000000"));
    }
}
