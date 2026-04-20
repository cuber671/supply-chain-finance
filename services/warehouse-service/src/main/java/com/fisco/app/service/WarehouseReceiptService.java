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
     *
     * 货主企业发起入库申请，指定仓储公司（warehouseId为仓储公司entId）。
     *
     * @param warehouseEntId 仓储公司ID（entId）
     * @param entId 申请企业ID
     * @param userId 申请人用户ID
     * @param goodsName 货物名称
     * @param weight 货物重量
     * @param unit 重量单位
     * @param attachmentUrl 附件URL（货权凭证等）
     * @return 创建的入库单ID
     * @throws IllegalArgumentException 参数不合法
     */
    Long applyStockIn(Long warehouseEntId, Long entId, Long userId, String goodsName,
            BigDecimal weight, String unit, String attachmentUrl);

    /**
     * 确认入库单（仓库方操作）
     *
     * 仓库管理人员核实货物到库后，确认入库单生效，并指定具体仓库。
     * 确认后自动签发仓单。
     *
     * @param stockOrderId 入库单ID
     * @param actualWarehouseId 实际入库的仓库ID（Warehouse.id）
     * @return 新签发的仓单ID
     * @throws IllegalArgumentException 入库单不存在或状态不允许确认
     */
    Long confirmStockOrder(Long stockOrderId, Long actualWarehouseId);

    /**
     * 取消入库单
     *
     * 申请人可在入库单未确认前取消。
     *
     * @param stockOrderId 入库单ID
     * @return 取消是否成功
     * @throws IllegalArgumentException 入库单不存在或状态不允许取消
     */
    boolean cancelStockOrder(Long stockOrderId);

    /**
     * 快速入库确认（合并申请和确认步骤）
     *
     * 仓储公司在收到货物后，直接确认入库并签发仓单，跳过待确认状态。
     * 适用于物流直接移库场景，仓库已确认收到货物。
     *
     * @param warehouseEntId 仓储公司ID
     * @param entId 货主企业ID
     * @param userId 货主用户ID
     * @param goodsName 货物名称
     * @param weight 货物重量
     * @param unit 计量单位
     * @param attachmentUrl 附件URL（可选）
     * @param actualWarehouseId 实际仓库ID
     * @return 新创建的仓单ID
     * @throws IllegalArgumentException 参数不合法或仓库不存在
     */
    Long applyStockInAndConfirm(Long warehouseEntId, Long entId, Long userId, String goodsName,
            BigDecimal weight, String unit, String attachmentUrl, Long actualWarehouseId);

    /**
     * 创建已确认状态的入库单（不签发仓单）
     *
     * 适用于物流直接移库场景：arrive到货后直接创建已确认的入库单，
     * 仓单签发由后续confirmDelivery或仓储方手动操作完成。
     *
     * @param warehouseEntId 仓储公司ID
     * @param entId 货主企业ID（可为null）
     * @param goodsName 货物名称
     * @param weight 货物重量
     * @param unit 计量单位
     * @param actualWarehouseId 实际仓库ID
     * @return 新创建的入库单ID
     * @throws IllegalArgumentException 参数不合法或仓库不存在
     */
    Long createStockInConfirmed(Long warehouseEntId, Long entId, String goodsName,
            BigDecimal weight, String unit, Long actualWarehouseId);

    /**
     * 查询入库单
     *
     * @param stockOrderId 入库单ID
     * @return 入库单记录，不存在返回null
     */
    StockOrder getStockOrderById(Long stockOrderId);

    /**
     * 更新入库单状态
     *
     * @param stockOrderId 入库单ID
     * @param status 新状态
     * @param remark 备注（可选）
     * @return 更新是否成功
     */
    boolean updateStockOrderStatus(Long stockOrderId, Integer status, String remark);

    /**
     * 查询企业的入库单列表
     *
     * @param entId 企业ID
     * @return 入库单列表
     */
    List<StockOrder> getStockOrdersByEntId(Long entId);

    /**
     * 分页查询企业的入库单列表
     *
     * @param entId 企业ID
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    IPage<StockOrder> getStockOrdersByEntIdPaginated(Long entId, int pageNum, int pageSize);

    // ==================== 仓单签发 ====================

    /**
     * 签发仓单
     *
     * 仓库方根据已确认的入库单签发仓单，仓单代表货物的所有权。
     * 签发后仓单上链，记录链上ID。
     *
     * @param stockOrderId 入库单ID
     * @param warehouseUserId 仓库操作员用户ID
     * @param onChainId 区块链上该仓单的ID
     * @return 创建的仓单ID
     * @throws IllegalArgumentException 入库单不存在或状态不允许签发
     */
    Long mintReceipt(Long stockOrderId, Long warehouseUserId, String onChainId);

    /**
     * 物流直接入库签发仓单
     *
     * @param warehouseId 仓库ID
     * @param goodsName 货物名称
     * @param weight 重量
     * @param unit 单位
     * @param ownerEntId 货主企业ID
     * @param warehouseUserId 仓库用户ID
     * @param warehouseEntId 仓库企业ID
     * @param logisticsVoucherNo 物流委托单号
     * @return 仓单ID
     */
    Long mintDirectReceipt(Long warehouseId, String goodsName, BigDecimal weight, String unit,
                           Long ownerEntId, Long warehouseUserId, Long warehouseEntId, String logisticsVoucherNo);

    /**
     * 查询仓单
     *
     * @param receiptId 仓单ID
     * @return 仓单记录，不存在返回null
     */
    WarehouseReceipt getReceiptById(Long receiptId);

    /**
     * 根据链上ID查询仓单
     *
     * @param onChainId 区块链上的仓单ID
     * @return 仓单记录，不存在返回null
     */
    WarehouseReceipt getReceiptByOnChainId(String onChainId);

    /**
     * 查询企业的仓单列表
     *
     * @param entId 企业ID
     * @return 仓单列表
     */
    List<WarehouseReceipt> getReceiptsByEntId(Long entId);

    /**
     * 分页查询企业的仓单列表
     *
     * @param entId 企业ID
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    IPage<WarehouseReceipt> getReceiptsByEntIdPaginated(Long entId, int pageNum, int pageSize);

    /**
     * 查询企业在库仓单列表
     *
     * @param entId 企业ID
     * @return 在库状态的仓单列表
     */
    List<WarehouseReceipt> getInStockReceipts(Long entId);

    /**
     * 分页查询企业在库仓单列表
     *
     * @param entId 企业ID
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    IPage<WarehouseReceipt> getInStockReceiptsPaginated(Long entId, int pageNum, int pageSize);

    /**
     * 校验仓单所有权
     *
     * 验证仓单是否属于指定企业，返回仓单状态和所有权信息。
     *
     * @param receiptId 仓单ID
     * @param entId 企业ID
     * @return 包含仓单状态、所属企业信息、ownerUserId的Map
     * @throws IllegalArgumentException 仓单不存在
     */
    java.util.Map<String, Object> validateReceiptOwnership(Long receiptId, Long entId);

    // ==================== 背书转让 ====================

    /**
     * 发起背书转让
     *
     * 仓单持有人发起背书转让，将仓单转让给目标企业。
     * 转让需目标企业确认后生效。
     *
     * @param receiptId 仓单ID
     * @param transferorUserId 转让人用户ID
     * @param transfereeEntId 受让人企业ID
     * @param signatureHash 转让签名哈希
     * @return 创建的背书记录ID
     * @throws IllegalArgumentException 仓单不存在或状态不允许背书转让
     */
    Long launchEndorsement(Long receiptId, Long transferorUserId, Long transfereeEntId,
            String signatureHash);

    /**
     * 确认/拒绝背书
     *
     * 被背书企业确认或拒绝背书转让。
     *
     * @param endorsementId 背书记录ID
     * @param transfereeUserId 被背书企业确认操作的用户ID
     * @param accept true确认/false拒绝
     * @return 操作是否成功
     * @throws IllegalArgumentException 背书记录不存在或状态不允许操作
     */
    boolean confirmEndorsement(Long endorsementId, Long transfereeUserId, boolean accept);

    /**
     * 撤回背书
     *
     * 转让人在背书被确认前撤回转让申请。
     *
     * @param endorsementId 背书记录ID
     * @return 撤回是否成功
     * @throws IllegalArgumentException 背书记录不存在或状态不允许撤回
     */
    boolean revokeEndorsement(Long endorsementId);

    /**
     * 查询仓单的背书记录
     *
     * @param receiptId 仓单ID
     * @return 背书记录列表（按时间倒序）
     */
    List<ReceiptEndorsement> getEndorsementsByReceiptId(Long receiptId);

    /**
     * 校验被背书目标企业权限
     *
     * 验证当前企业是否有权作为背书的目标方。
     *
     * @param endorsementId 背书记录ID
     * @throws IllegalArgumentException 无权操作
     */
    void checkEndorsementTargetPermission(Long endorsementId);

    /**
     * 校验背书发起方权限
     *
     * 验证当前企业/用户是否有权发起背书转让。
     *
     * @param endorsementId 背书记录ID
     * @throws IllegalArgumentException 无权操作
     */
    void checkEndorsementInitiatorPermission(Long endorsementId);

    // ==================== 拆分/合并 ====================

    /**
     * 发起拆分申请
     *
     * 将一个仓单拆分为多个子仓单，每个子仓单重量之和等于原仓单。
     *
     * @param receiptId 原仓单ID
     * @param applyUserId 申请人用户ID
     * @param targetWeights 目标各子仓单重量数组
     * @param warehouseIds 目标各子仓单所属仓库ID数组（可为null，为null时使用原仓单仓库）
     * @return 创建的操作记录ID
     * @throws IllegalArgumentException 仓单不存在或重量分配不合法
     */
    Long applySplit(Long receiptId, Long applyUserId, BigDecimal[] targetWeights, Long[] warehouseIds);

    /**
     * 发起合并申请
     *
     * 将多个仓单合并为一个新仓单。
     *
     * @param receiptIds 待合并的仓单ID列表
     * @param applyUserId 申请人用户ID
     * @return 创建的操作记录ID
     * @throws IllegalArgumentException 仓单不存在或状态不允许合并
     */
    Long applyMerge(List<Long> receiptIds, Long applyUserId);

    /**
     * 执行或驳回拆分合并
     *
     * @param opLogId 操作记录ID
     * @param executeUserId 执行操作的用户ID
     * @param execute true执行/false驳回
     * @return 操作是否成功
     * @throws IllegalArgumentException 记录不存在或状态不允许操作
     */
    boolean executeSplitMerge(Long opLogId, Long executeUserId, boolean execute);

    /**
     * 查询拆分合并操作记录
     *
     * @param opLogId 操作记录ID
     * @return 操作记录，不存在返回null
     */
    ReceiptOperationLog getOperationLogById(Long opLogId);

    // ==================== 质押/解押 ====================

    /**
     * 质押锁定仓单
     *
     * 仓单作为贷款质押物时锁定，锁定后不可进行背书转让等操作。
     *
     * @param receiptId 仓单ID
     * @param loanId 关联的贷款ID
     * @return 锁定是否成功
     * @throws IllegalArgumentException 仓单不存在或状态不允许锁定
     */
    boolean lockReceipt(Long receiptId, String loanId);

    /**
     * 还款解押仓单
     *
     * 贷款还款后解除仓单质押锁定。
     *
     * @param receiptId 仓单ID
     * @return 解锁是否成功
     * @throws IllegalArgumentException 仓单不存在或未被锁定
     */
    boolean unlockReceipt(Long receiptId);

    /**
     * 管理员强制解锁仓单
     *
     * 用于异常情况下的手动干预，跳过部分校验直接解锁。
     *
     * @param receiptId 仓单ID
     * @param reason 强制解锁原因
     * @return 解锁是否成功
     */
    boolean forceUnlockReceipt(Long receiptId, String reason);

    /**
     * 设置仓单为物流转运中状态（物流提货确认时调用）
     *
     * 物流服务调用此方法将仓单区块链状态设为InTransit(5)。
     *
     * @param receiptId 仓单ID
     * @return 操作是否成功
     */
    boolean setInTransit(Long receiptId);

    /**
     * 从物流转运中恢复到在库状态（部分交付确认时调用）
     *
     * @param receiptId 仓单ID
     * @return 操作是否成功
     */
    boolean restoreFromTransit(Long receiptId);

    /**
     * 标记仓单为待物流状态
     *
     * 物流服务创建委派单时调用，防止仓单在物流操作期间被拆分/转让/再次创建物流。
     *
     * @param receiptId 仓单ID
     * @param voucherNo 物流委托单号
     * @return 操作是否成功
     * @throws IllegalStateException 仓单状态不允许（如已锁定/已作废/已有待物流）
     */
    boolean markWaitLogistics(Long receiptId, String voucherNo);

    /**
     * 清除仓单的待物流状态
     *
     * 物流委派单交付确认或取消时调用，恢复仓单可操作状态。
     *
     * @param receiptId 仓单ID
     * @return 操作是否成功
     * @throws IllegalStateException 仓单不在待物流状态
     */
    boolean clearWaitLogistics(Long receiptId);

    /**
     * 更新仓单备注
     *
     * 物流服务在指派司机后调用，记录承运信息到仓单备注。
     *
     * @param receiptId 仓单ID
     * @param remark 备注内容
     * @return 更新是否成功
     * @throws IllegalArgumentException 仓单不存在
     */
    boolean updateReceiptRemark(Long receiptId, String remark);

    /**
     * 作废仓单
     *
     * 仅在库、未锁定、状态为IN_STOCK的仓单可作废。
     * 作废后仓单不可再进行任何操作。
     *
     * @param receiptId 仓单ID
     * @param operatorUserId 操作人用户ID
     * @param reason 作废原因
     * @return 作废是否成功
     * @throws IllegalArgumentException 仓单不存在或状态不允许作废
     */
    boolean voidReceipt(Long receiptId, Long operatorUserId, String reason);

    /**
     * 撤销拆分合并申请
     *
     * 申请人可在状态为PENDING时主动撤销。
     *
     * @param opLogId 操作记录ID
     * @param applyUserId 申请人用户ID
     * @return 撤销是否成功
     * @throws IllegalArgumentException 记录不存在或状态不允许撤销
     */
    boolean cancelSplitMerge(Long opLogId, Long applyUserId);

    // ==================== 核销出库 ====================

    /**
     * 申请核销出库
     *
     * 仓单持有人申请将仓单核销并出库。
     *
     * @param receiptId 仓单ID
     * @param applyUserId 申请人用户ID
     * @param signatureHash 核销签名哈希
     * @return 创建的操作记录ID
     * @throws IllegalArgumentException 仓单不存在或状态不允许核销
     */
    Long applyBurn(Long receiptId, Long applyUserId, String signatureHash);

    /**
     * 确认核销出库
     *
     * 仓库方确认货物已出库，完成核销流程。
     *
     * @param stockOrderId 原入库单ID
     * @param warehouseUserId 仓库操作员用户ID
     * @return 确认是否成功
     * @throws IllegalArgumentException 入库单不存在或状态不允许确认
     */
    boolean confirmBurn(Long stockOrderId, Long warehouseUserId);

    // ==================== 仓库管理 ====================

    /**
     * 创建仓库
     *
     * @param entId 所属企业ID
     * @param name 仓库名称
     * @param address 仓库地址
     * @param contactUser 联系人
     * @param contactPhone 联系电话
     * @return 创建的仓库ID
     */
    Long createWarehouse(Long entId, String name, String address, String contactUser,
            String contactPhone);

    /**
     * 查询企业的仓库列表
     *
     * @param entId 企业ID
     * @return 仓库列表
     */
    List<Warehouse> getWarehousesByEntId(Long entId);

    /**
     * 分页查询企业的仓库列表
     *
     * @param entId 企业ID
     * @param pageNum 页码，从1开始
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    IPage<Warehouse> getWarehousesByEntIdPaginated(Long entId, int pageNum, int pageSize);

    // ==================== 溯源查询 ====================

    /**
     * 全路径溯源查询
     *
     * 查询仓单的完整生命周期，包括当前状态、历史记录、背书历史、操作历史。
     *
     * @param receiptId 仓单ID
     * @return 溯源信息封装对象
     */
    TraceInfo traceReceipt(Long receiptId);

    /**
     * 根据仓库ID查询仓库
     *
     * @param warehouseId 仓库ID
     * @return 仓库记录，不存在返回null
     */
    Warehouse getWarehouseById(Long warehouseId);

    /**
     * 溯源信息封装类
     *
     * 包含仓单溯源的完整信息：当前仓单、历史仓单、背书历史、操作历史
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