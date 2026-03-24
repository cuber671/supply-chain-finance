// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title IBillCore
 * @dev 票据核心合约接口
 *
 * 用于跨合约调用，定义票据核心业务操作的接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
interface IBillCore {

    // ==================== 数据结构 ====================

    /**
     * @dev 票据状态枚举
     */
    enum BillStatus {
        None,       // 0-不存在
        Created,    // 1-已开立
        Accepted,   // 2-已承兑
        Paid,       // 3-已兑付
        Overdue,    // 4-已逾期
        Cancelled   // 5-已撤销
    }

    /**
     * @dev 底层资产类型枚举
     */
    enum UnderlyingType {
        None,        // 0-无
        WarehouseReceipt, // 1-仓单
        Receivable   // 2-应收款
    }

    // ==================== 核心操作 ====================

    /**
     * @dev 开立票据
     * @param billId 票据ID
     * @param amount 票据金额
     * @param dueDate 到期日
     * @param payerHash 开票人哈希
     * @param payeeHash 收票人哈希
     * @param underlyingType 底层资产类型
     * @param underlyingId 底层资产ID
     * @return success 是否成功
     */
    function issueBill(
        string calldata billId,
        uint256 amount,
        uint256 dueDate,
        bytes32 payerHash,
        bytes32 payeeHash,
        UnderlyingType underlyingType,
        string calldata underlyingId
    ) external returns (bool success);

    /**
     * @dev 承兑票据
     * @param billId 票据ID
     * @return success 是否成功
     */
    function acceptBill(string calldata billId) external returns (bool success);

    /**
     * @dev 兑付票据
     * @param billId 票据ID
     * @return success 是否成功
     */
    function payBill(string calldata billId) external returns (bool success);

    /**
     * @dev 撤销票据
     * @param billId 票据ID
     * @return success 是否成功
     */
    function cancelBill(string calldata billId) external returns (bool success);

    /**
     * @dev 背书转让
     * @param billId 票据ID
     * @param toHash 被背书人哈希
     * @return success 是否成功
     */
    function endorseBill(string calldata billId, bytes32 toHash) external returns (bool success);

    /**
     * @dev 确认背书
     * @param billId 票据ID
     * @return success 是否成功
     */
    function confirmEndorsement(string calldata billId) external returns (bool success);

    // ==================== 查询操作 ====================

    /**
     * @dev 获取票据状态
     * @param billId 票据ID
     * @return status 票据状态
     */
    function getStatus(string calldata billId) external view returns (BillStatus status);

    /**
     * @dev 检查票据是否存在
     * @param billId 票据ID
     * @return exists 是否存在
     */
    function exists(string calldata billId) external view returns (bool exists);

    /**
     * @dev 检查票据是否有效
     * @param billId 票据ID
     * @return isValid 是否有效
     */
    function isValid(string calldata billId) external view returns (bool isValid);

    /**
     * @dev 获取持票人哈希
     * @param billId 票据ID
     * @return holderHash 持票人哈希
     */
    function getHolderHash(string calldata billId) external view returns (bytes32 holderHash);

    /**
     * @dev 获取开票人哈希
     * @param billId 票据ID
     * @return payerHash 开票人哈希
     */
    function getPayerHash(string calldata billId) external view returns (bytes32 payerHash);

    // ==================== 拆分合并操作 ====================

    /**
     * @dev 拆分票据
     * @param billId 原票据ID
     * @param newBillId1 第一个新票据ID
     * @param amount1 第一个新票据金额
     * @param newBillId2 第二个新票据ID
     * @param amount2 第二个新票据金额
     * @return success 是否成功
     */
    function splitBill(
        string calldata billId,
        string calldata newBillId1,
        uint256 amount1,
        string calldata newBillId2,
        uint256 amount2
    ) external returns (bool success);

    /**
     * @dev 合并票据
     * @param billIds 源票据ID数组
     * @param newBillId 新票据ID
     * @param totalAmount 总金额
     * @return success 是否成功
     */
    function mergeBills(
        string[] calldata billIds,
        string calldata newBillId,
        uint256 totalAmount
    ) external returns (bool success);
}
