package com.fisco.app.service;

import java.math.BigInteger;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;

/**
 * 区块链基础服务接口
 *
 * 提供区块链网络的基础操作能力，包括：
 * - 区块信息查询
 * - 交易操作
 * - 账户管理
 * - 合约调用
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface BlockchainService {

    /**
     * 获取当前区块高度
     *
     * @return 当前区块 number（最新已确认区块）
     */
    BigInteger getBlockNumber();

    /**
     * 根据区块 number 获取区块详细信息
     *
     * @param blockNumber 区块 number
     * @return 区块信息（JSON格式，包含区块哈希、交易列表、时间戳等）
     */
    String getBlockByNumber(BigInteger blockNumber);

    /**
     * 根据区块 number 获取区块哈希值
     *
     * @param blockNumber 区块 number
     * @return 区块哈希值（十六进制字符串）
     */
    String getBlockHashByNumber(BigInteger blockNumber);

    /**
     * 获取链ID
     *
     * @return 链ID（用于区分不同区块链网络）
     */
    String getChainId();

    /**
     * 根据交易哈希获取交易Receipt
     *
     * @param txHash 交易哈希
     * @return 交易Receipt（包含执行状态、logs、交易消耗等）
     */
    TransactionReceipt getTransactionReceipt(String txHash);

    /**
     * 发送裸交易
     *
     * 将已签名的交易数据发送到区块链网络。
     *
     * @param to 目标地址（合约地址或外部账户地址）
     * @param data 交易数据（已签名的交易payload）
     * @return 交易Receipt
     */
    TransactionReceipt sendRawTransaction(String to, String data);

    /**
     * 获取当前SDK使用的账户地址
     *
     * @return 当前账户的区块链地址
     */
    String getCurrentAccountAddress();

    /**
     * 查询指定地址的账户余额
     *
     * @param address 区块链地址
     * @return 余额（以区块链原生代币单位，如Wei）
     */
    String getBalance(String address);

    /**
     * 调用合约的只读方法（Call，不上链）
     *
     * @param contractAddress 合约地址
     * @param abi 合约ABI定义（JSON格式）
     * @param method 调用的方法名
     * @param params 方法参数列表
     * @return 调用结果（CallResponse包含返回值）
     */
    CallResponse callContract(String contractAddress, String abi, String method, List<Object> params);

    /**
     * 发送合约交易（Transaction，上链）
     *
     * 调用合约的写方法，会生成交易记录并上链。
     *
     * @param contractAddress 合约地址
     * @param abi 合约ABI定义（JSON格式）
     * @param method 调用的方法名
     * @param params 方法参数列表
     * @return 交易结果对象
     */
    Object sendContractTransaction(String contractAddress, String abi, String method, List<Object> params);

    /**
     * 获取群组列表
     *
     * @return 节点所属的群组ID列表
     */
    List<String> getGroupList();

    /**
     * 获取当前群组信息
     *
     * @return 群组详细信息（JSON格式）
     */
    String getGroupInfo();

    /**
     * 检查区块链连接状态
     *
     * @return 是否已连接到区块链节点
     */
    boolean isConnected();
}