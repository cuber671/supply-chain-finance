package com.fisco.app.service;

import java.util.List;
import java.util.Map;

/**
 * 区块链事件监听服务接口
 *
 * 【QS-07修复】实现区块链事件监听机制
 *
 * 功能：
 * 1. 订阅链上事件（LoanCreated, ReceiptIssued, StatusChanged等）
 * 2. 解析事件参数并触发业务逻辑
 * 3. 处理事件回调，更新本地数据库
 *
 * 使用方式：
 * 1. 服务启动时注册事件监听器
 * 2. 事件发生时自动触发回调处理
 * 3. 回调中可更新DB、执行补偿逻辑等
 *
 * 注意：
 * - FISCO SDK v3 支持通过 Client.subscribeEvent() 订阅事件
 * - 需要配置事件过滤器（fromBlock, toBlock, addresses, events）
 * - 事件处理应幂等，防止重复处理
 *
 * TODO: 实现完整的事件监听机制
 * 1. 创建事件订阅配置类
 * 2. 实现事件解析器（将原始事件转换为业务对象）
 * 3. 实现事件处理器（处理业务逻辑）
 * 4. 配置事件重试机制（网络异常时重试）
 * 5. 添加事件审计日志
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface BlockchainEventService {

    /**
     * 启动事件监听
     * 应在服务启动时调用
     */
    void startListening();

    /**
     * 停止事件监听
     * 应在服务关闭时调用
     */
    void stopListening();

    /**
     * 处理贷款创建事件
     */
    void handleLoanCreatedEvent(String loanNo, String borrowerHash, String financeEntHash,
                                String receiptId, long loanAmount, long interestRate,
                                long loanDays, long timestamp);

    /**
     * 处理仓单发行事件
     */
    void handleReceiptIssuedEvent(String receiptId, String ownerHash, String warehouseHash,
                                  String goodsDetailHash, long weight, String unit,
                                  long timestamp);

    /**
     * 处理仓单状态变更事件
     */
    void handleReceiptStatusChangedEvent(String receiptId, int oldStatus, int newStatus,
                                         String operator, long timestamp);

    /**
     * 处理还款事件
     */
    void handleRepaymentEvent(String loanNo, long principal, long interest, long penalty,
                              String repaymentType, long timestamp);

    /**
     * 获取最近的事件记录（用于调试）
     */
    List<Map<String, Object>> getRecentEvents(int limit);
}
