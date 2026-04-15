package com.fisco.app.task;

import java.time.LocalDateTime;
import java.util.List;

import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fisco.app.entity.BlockchainTransactionRecord;
import com.fisco.app.mapper.BlockchainTransactionRecordMapper;
import com.fisco.app.service.BlockchainService;

import lombok.extern.slf4j.Slf4j;

/**
 * 区块链交易补偿定时任务
 *
 * 用于处理区块链交易失败后的自动补偿：
 * 1. 查询状态为失败（STATUS_FAILED）且重试次数未耗尽的记录
 * 2. 向区块链查询交易的真实状态
 * 3. 如果交易已确认，更新状态为成功（STATUS_SUCCESS）
 * 4. 如果重试次数耗尽，更新状态为永久失败（STATUS_RETRY_EXHAUSTED）
 *
 * 注意：此任务仅能检测交易是否已上链，无法重新发送失败交易
 * 重新发送需要业务层实现完整的交易构造和签名流程
 */
@Slf4j
@Component
public class BlockchainTransactionRetryTask {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainTransactionRetryTask.class);

    @Autowired
    private BlockchainTransactionRecordMapper transactionRecordMapper;

    @Autowired(required = false)
    private BlockchainService blockchainService;

    /**
     * 每5分钟执行一次交易状态补偿检查
     * 使用cron表达式: 秒 分 时 日 月 周
     * 0 0-59/5 * * * ? = 每5分钟
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void retryFailedTransactions() {
        if (blockchainService == null || !blockchainService.isConnected()) {
            logger.warn("区块链服务未连接，跳过交易补偿任务");
            return;
        }

        logger.info("开始执行区块链交易补偿任务");
        try {
            // 查询需要补偿的交易：状态为失败且重试次数未耗尽
            LambdaQueryWrapper<BlockchainTransactionRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlockchainTransactionRecord::getStatus, BlockchainTransactionRecord.STATUS_FAILED)
                   .lt(BlockchainTransactionRecord::getRetryCount, BlockchainTransactionRecord.MAX_RETRY_COUNT)
                   .orderByAsc(BlockchainTransactionRecord::getCreateTime);  // 按创建时间顺序处理

            List<BlockchainTransactionRecord> failedTransactions = transactionRecordMapper.selectList(wrapper);

            if (failedTransactions.isEmpty()) {
                logger.info("没有需要补偿的失败交易");
                return;
            }

            logger.info("发现 {} 笔需要补偿的失败交易", failedTransactions.size());

            int successCount = 0;
            int stillFailedCount = 0;
            int exhaustedCount = 0;

            for (BlockchainTransactionRecord record : failedTransactions) {
                try {
                    boolean result = checkAndUpdateTransactionStatus(record);
                    if (result) {
                        successCount++;
                    } else {
                        // 增加重试次数
                        int newRetryCount = record.getRetryCount() + 1;
                        record.setRetryCount(newRetryCount);
                        record.setLastRetryTime(LocalDateTime.now());

                        if (newRetryCount >= BlockchainTransactionRecord.MAX_RETRY_COUNT) {
                            // 重试次数耗尽，标记为永久失败
                            record.setStatus(BlockchainTransactionRecord.STATUS_RETRY_EXHAUSTED);
                            record.setErrorMsg("重试次数耗尽，交易可能已丢失");
                            exhaustedCount++;
                            logger.warn("交易重试次数耗尽: txHash={}, retryCount={}",
                                    record.getTxHash(), newRetryCount);
                        } else {
                            stillFailedCount++;
                        }

                        transactionRecordMapper.updateById(record);
                    }
                } catch (Exception e) {
                    logger.error("处理交易补偿时异常: txHash={}, error={}",
                            record.getTxHash(), e.getMessage());
                    // 即使异常也更新重试次数
                    record.setRetryCount(record.getRetryCount() + 1);
                    record.setLastRetryTime(LocalDateTime.now());
                    record.setErrorMsg("补偿任务异常: " + e.getMessage());
                    if (record.getRetryCount() >= BlockchainTransactionRecord.MAX_RETRY_COUNT) {
                        record.setStatus(BlockchainTransactionRecord.STATUS_RETRY_EXHAUSTED);
                    }
                    transactionRecordMapper.updateById(record);
                }
            }

            logger.info("区块链交易补偿任务完成: 成功确认={}, 仍失败={}, 次数耗尽={}",
                    successCount, stillFailedCount, exhaustedCount);

        } catch (Exception e) {
            logger.error("区块链交易补偿任务执行失败", e);
        }
    }

    /**
     * 检查并更新交易状态
     *
     * @param record 交易记录
     * @return true 如果交易已确认成功，false 如果仍失败或状态未知
     */
    private boolean checkAndUpdateTransactionStatus(BlockchainTransactionRecord record) {
        if (record.getTxHash() == null || record.getTxHash().isEmpty()) {
            logger.warn("交易记录缺少txHash，无法查询: recordId={}", record.getId());
            record.setErrorMsg("交易记录缺少txHash");
            return false;
        }

        try {
            TransactionReceipt receipt = blockchainService.getTransactionReceipt(record.getTxHash());

            if (receipt != null && receipt.getBlockNumber() != null) {
                // 交易已确认上链，更新状态
                record.setStatus(BlockchainTransactionRecord.STATUS_SUCCESS);
                record.setBlockNumber(receipt.getBlockNumber().longValue());
                record.setErrorMsg(null);
                transactionRecordMapper.updateById(record);

                logger.info("交易补偿成功: txHash={}, blockNumber={}",
                        record.getTxHash(), receipt.getBlockNumber());
                return true;
            } else {
                // 交易仍未确认
                record.setErrorMsg("交易未确认，可能仍在pending状态");
                return false;
            }
        } catch (Exception e) {
            logger.warn("查询交易状态失败: txHash={}, error={}", record.getTxHash(), e.getMessage());
            record.setErrorMsg("查询失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 手动触发补偿任务（供管理接口调用）
     *
     * @param recordId 交易记录ID，如果为null则处理所有失败记录
     * @return 补偿处理的记录数
     */
    public int triggerManualRetry(Long recordId) {
        if (blockchainService == null || !blockchainService.isConnected()) {
            throw new IllegalStateException("区块链服务未连接");
        }

        logger.info("手动触发交易补偿任务: recordId={}", recordId);

        if (recordId != null) {
            BlockchainTransactionRecord record = transactionRecordMapper.selectById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("交易记录不存在: " + recordId);
            }
            if (record.getStatus() != BlockchainTransactionRecord.STATUS_FAILED) {
                throw new IllegalStateException("交易状态不是失败，无法补偿: " + record.getStatus());
            }

            boolean success = checkAndUpdateTransactionStatus(record);
            if (!success) {
                int newRetryCount = record.getRetryCount() + 1;
                record.setRetryCount(newRetryCount);
                record.setLastRetryTime(LocalDateTime.now());
                if (newRetryCount >= BlockchainTransactionRecord.MAX_RETRY_COUNT) {
                    record.setStatus(BlockchainTransactionRecord.STATUS_RETRY_EXHAUSTED);
                }
                transactionRecordMapper.updateById(record);
            }
            return 1;
        } else {
            // 处理所有失败记录
            LambdaQueryWrapper<BlockchainTransactionRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlockchainTransactionRecord::getStatus, BlockchainTransactionRecord.STATUS_FAILED);
            List<BlockchainTransactionRecord> records = transactionRecordMapper.selectList(wrapper);
            for (BlockchainTransactionRecord record : records) {
                checkAndUpdateTransactionStatus(record);
            }
            return records.size();
        }
    }
}