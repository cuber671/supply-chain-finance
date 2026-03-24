package com.fisco.app.service;

import java.util.List;

import com.fisco.app.entity.BlockchainTransactionRecord;

/**
 * 链上行为审计服务接口
 * 用于关联 txHash 与 JWT 登录记录，实现链上行为审计回溯
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface BlockchainAuditService {

    void recordTransaction(String txHash, String operation, String contractName);

    String getJtiByTxHash(String txHash);

    BlockchainTransactionRecord getRecordByTxHash(String txHash);

    List<BlockchainTransactionRecord> getRecordsByUserId(Long userId);

    List<BlockchainTransactionRecord> getRecordsByEntId(Long entId);
}
