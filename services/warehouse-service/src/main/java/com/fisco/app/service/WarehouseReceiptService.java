package com.fisco.app.service;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fisco.app.entity.ReceiptEndorsement;
import com.fisco.app.entity.ReceiptOperationLog;
import com.fisco.app.entity.StockOrder;
import com.fisco.app.entity.Warehouse;
import com.fisco.app.entity.WarehouseReceipt;

/**
 * 仓单业务服务接口
 *
 * 提供仓单全生命周期管理，包括入库、签发、背书、拆分合并、质押、核销等功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface WarehouseReceiptService {

    // ==================== 入库单管理 ====================

    /**
     * 申请入库
     */
    Long applyStockIn(Long warehouseId, Long entId, Long userId, String goodsName,
            BigDecimal weight, String unit, String attachmentUrl);

    /**
     * 确认入库单
     */
    boolean confirmStockOrder(Long stockOrderId);

    /**
     * 取消入库单
     */
    boolean cancelStockOrder(Long stockOrderId);

    /**
     * 查询入库单
     */
    StockOrder getStockOrderById(Long stockOrderId);

    /**
     * 根据入库单号查询入库单
     */
    StockOrder getStockOrderByStockNo(String stockNo);

    /**
     * 查询企业入库单列表
     */
    List<StockOrder> getStockOrdersByEntId(Long entId);

    /**
     * 分页查询企业入库单列表
     */
    IPage<StockOrder> getStockOrdersByEntIdPaginated(Long entId, int pageNum, int pageSize);

    // ==================== 仓单签发 ====================

    /**
     * 签发仓单
     */
    Long mintReceipt(Long stockOrderId, Long warehouseUserId, String onChainId);

    /**
     * 查询仓单
     */
    WarehouseReceipt getReceiptById(Long receiptId);

    /**
     * 根据链上ID查询仓单
     */
    WarehouseReceipt getReceiptByOnChainId(String onChainId);

    /**
     * 查询企业仓单列表
     */
    List<WarehouseReceipt> getReceiptsByEntId(Long entId);

    /**
     * 分页查询企业仓单列表
     */
    IPage<WarehouseReceipt> getReceiptsByEntIdPaginated(Long entId, int pageNum, int pageSize);

    /**
     * 查询企业在库仓单
     */
    List<WarehouseReceipt> getInStockReceipts(Long entId);

    /**
     * 分页查询企业在库仓单
     */
    IPage<WarehouseReceipt> getInStockReceiptsPaginated(Long entId, int pageNum, int pageSize);

    /**
     * 校验仓单所有权
     * @param receiptId 仓单ID
     * @param entId 企业ID
     * @return 仓单状态、所属企业信息、ownerUserId
     */
    java.util.Map<String, Object> validateReceiptOwnership(Long receiptId, Long entId);

    // ==================== 背书转让 ====================

    /**
     * 发起背书转让
     */
    Long launchEndorsement(Long receiptId, Long transferorUserId, Long transfereeEntId,
            String signatureHash);

    /**
     * 确认/拒绝背书
     */
    boolean confirmEndorsement(Long endorsementId, Long transfereeUserId, boolean accept);

    /**
     * 撤回背书
     */
    boolean revokeEndorsement(Long endorsementId);

    /**
     * 查询背书记录
     */
    List<ReceiptEndorsement> getEndorsementsByReceiptId(Long receiptId);

    /**
     * 校验被背书目标企业权限
     */
    void checkEndorsementTargetPermission(Long endorsementId);

    /**
     * 校验背书发起方权限
     */
    void checkEndorsementInitiatorPermission(Long endorsementId);

    // ==================== 拆分/合并 ====================

    /**
     * 发起拆分申请
     */
    Long applySplit(Long receiptId, Long applyUserId, BigDecimal[] targetWeights);

    /**
     * 发起合并申请
     */
    Long applyMerge(List<Long> receiptIds, Long applyUserId);

    /**
     * 执行/驳回拆分合并
     */
    boolean executeSplitMerge(Long opLogId, Long executeUserId, boolean execute);

    /**
     * 查询操作记录
     */
    ReceiptOperationLog getOperationLogById(Long opLogId);

    // ==================== 质押/解押 ====================

    /**
     * 质押锁定仓单
     */
    boolean lockReceipt(Long receiptId, String loanId);

    /**
     * 还款解押仓单
     */
    boolean unlockReceipt(Long receiptId);

    /**
     * 管理员强制解锁仓单（用于异常情况下的手动干预）
     * 跳过部分校验，直接解锁仓单
     */
    boolean forceUnlockReceipt(Long receiptId, String reason);

    /**
     * 作废仓单（撤销签发）
     * 仅在库、未锁定、状态为IN_STOCK的仓单可作废
     * 作废后仓单不可再进行任何操作
     */
    boolean voidReceipt(Long receiptId, Long operatorUserId, String reason);

    /**
     * 撤销拆分合并申请（申请人主动撤销）
     * 仅申请人可在状态为PENDING时撤销
     */
    boolean cancelSplitMerge(Long opLogId, Long applyUserId);

    // ==================== 核销出库 ====================

    /**
     * 申请核销出库
     */
    Long applyBurn(Long receiptId, Long applyUserId, String signatureHash);

    /**
     * 确认核销出库
     */
    boolean confirmBurn(Long stockOrderId, Long warehouseUserId);

    // ==================== 仓库管理 ====================

    /**
     * 创建仓库
     */
    Long createWarehouse(Long entId, String name, String address, String contactUser,
            String contactPhone);

    /**
     * 查询仓库列表
     */
    List<Warehouse> getWarehousesByEntId(Long entId);

    /**
     * 分页查询仓库列表
     */
    IPage<Warehouse> getWarehousesByEntIdPaginated(Long entId, int pageNum, int pageSize);

    // ==================== 溯源查询 ====================

    /**
     * 全路径溯源查询
     */
    TraceInfo traceReceipt(Long receiptId);

    /**
     * 根据仓库ID查询仓库
     */
    Warehouse getWarehouseById(Long warehouseId);

    /**
     * 溯源信息
     */
    class TraceInfo {
        private WarehouseReceipt currentReceipt;
        private List<WarehouseReceipt> historyReceipts;
        private List<ReceiptEndorsement> endorsementHistory;
        private List<ReceiptOperationLog> operationHistory;

        public WarehouseReceipt getCurrentReceipt() { return currentReceipt; }
        public void setCurrentReceipt(WarehouseReceipt currentReceipt) { this.currentReceipt = currentReceipt; }
        public List<WarehouseReceipt> getHistoryReceipts() { return historyReceipts; }
        public void setHistoryReceipts(List<WarehouseReceipt> historyReceipts) { this.historyReceipts = historyReceipts; }
        public List<ReceiptEndorsement> getEndorsementHistory() { return endorsementHistory; }
        public void setEndorsementHistory(List<ReceiptEndorsement> endorsementHistory) { this.endorsementHistory = endorsementHistory; }
        public List<ReceiptOperationLog> getOperationHistory() { return operationHistory; }
        public void setOperationHistory(List<ReceiptOperationLog> operationHistory) { this.operationHistory = operationHistory; }
    }
}
