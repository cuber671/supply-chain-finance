package com.fisco.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fisco.app.entity.BlockchainTransactionRecord;
import com.fisco.app.mapper.BlockchainTransactionRecordMapper;
import com.fisco.app.service.BlockchainAuditService;
import com.fisco.app.util.AuditContext;

import lombok.extern.slf4j.Slf4j;

/**
 * 链上行为审计服务实现类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
public class BlockchainAuditServiceImpl implements BlockchainAuditService {

    @Autowired
    private BlockchainTransactionRecordMapper transactionRecordMapper;

    @Override
    public void recordTransaction(String txHash, String operation, String contractName) {
        Long userId = AuditContext.getUserId();
        Long entId = AuditContext.getEntId();
        String jti = AuditContext.getJti();

        BlockchainTransactionRecord record = new BlockchainTransactionRecord();
        record.setTxHash(txHash);
        record.setMethodName(operation);
        record.setContractName(contractName);
        record.setUserId(userId);
        record.setEntId(entId);
        record.setJti(jti);
        record.setCreateTime(LocalDateTime.now());

        transactionRecordMapper.insert(record);

        log.info("链上交易审计记录已创建: txHash={}, operation={}, userId={}, entId={}",
                txHash, operation, userId, entId);
    }

    @Override
    public String getJtiByTxHash(String txHash) {
        LambdaQueryWrapper<BlockchainTransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlockchainTransactionRecord::getTxHash, txHash)
               .select(BlockchainTransactionRecord::getJti);

        BlockchainTransactionRecord record = transactionRecordMapper.selectOne(wrapper);
        return record != null ? record.getJti() : null;
    }

    @Override
    public BlockchainTransactionRecord getRecordByTxHash(String txHash) {
        LambdaQueryWrapper<BlockchainTransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlockchainTransactionRecord::getTxHash, txHash);

        return transactionRecordMapper.selectOne(wrapper);
    }

    @Override
    public List<BlockchainTransactionRecord> getRecordsByUserId(Long userId) {
        LambdaQueryWrapper<BlockchainTransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlockchainTransactionRecord::getUserId, userId)
               .orderByDesc(BlockchainTransactionRecord::getCreateTime);

        return transactionRecordMapper.selectList(wrapper);
    }

    @Override
    public List<BlockchainTransactionRecord> getRecordsByEntId(Long entId) {
        LambdaQueryWrapper<BlockchainTransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlockchainTransactionRecord::getEntId, entId)
               .orderByDesc(BlockchainTransactionRecord::getCreateTime);

        return transactionRecordMapper.selectList(wrapper);
    }
}
