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

    BigInteger getBlockNumber();

    String getBlockByNumber(BigInteger blockNumber);

    String getBlockHashByNumber(BigInteger blockNumber);

    String getChainId();

    TransactionReceipt getTransactionReceipt(String txHash);

    TransactionReceipt sendRawTransaction(String to, String data);

    String getCurrentAccountAddress();

    String getBalance(String address);

    CallResponse callContract(String contractAddress, String abi, String method, List<Object> params);

    Object sendContractTransaction(String contractAddress, String abi, String method, List<Object> params);

    List<String> getGroupList();

    String getGroupInfo();

    boolean isConnected();
}
