// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title IWarehouseReceiptCore
 * @dev 仓单核心合约接口
 *
 * 用于跨合约调用，定义仓单核心业务操作的接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
interface IWarehouseReceiptCore {

    // ==================== 数据结构 ====================

    /**
     * @dev 仓单拆分输入参数
     */
    struct SplitInput {
        string originalReceiptId;
        string[] newReceiptIds;
        uint256[] weights;
        bytes32[] ownerHashes;
        string unit;
    }

    /**
     * @dev 仓单合并输入参数
     */
    struct MergeInput {
        string[] sourceReceiptIds;
        string targetReceiptId;
        bytes32 targetOwnerHash;
        string unit;
        uint256 totalWeight;
    }

    // ==================== 拆分合并操作 ====================

    /**
     * @dev 拆分仓单
     * @param input 拆分参数
     * @return success 是否成功
     */
    function splitReceipt(SplitInput calldata input) external returns (bool success);

    /**
     * @dev 合并仓单
     * @param input 合并参数
     * @return success 是否成功
     */
    function mergeReceipts(MergeInput calldata input) external returns (bool success);

    // ==================== 状态操作 ====================

    /**
     * @dev 更新仓单所有者
     * @param receiptId 仓单ID
     * @param newOwnerHash 新所有者哈希
     * @return success 是否成功
     */
    function updateOwner(string calldata receiptId, bytes32 newOwnerHash) external returns (bool success);

    /**
     * @dev 更新仓单状态
     * @param receiptId 仓单ID
     * @param newStatus 新状态
     * @return success 是否成功
     */
    function updateReceiptStatus(string calldata receiptId, uint8 newStatus) external returns (bool success);

    // ==================== 查询操作 ====================

    /**
     * @dev 获取仓单重量
     * @param receiptId 仓单ID
     * @return weight 仓单重量
     */
    function getReceiptWeight(string calldata receiptId) external view returns (uint256 weight);

    /**
     * @dev 检查仓单是否可以操作
     * @param receiptId 仓单ID
     * @return canOperate 是否可以操作
     */
    function canOperate(string calldata receiptId) external view returns (bool canOperate);

    /**
     * @dev 检查仓单是否存在
     * @param receiptId 仓单ID
     * @return exists 是否存在
     */
    function exists(string calldata receiptId) external view returns (bool exists);

    /**
     * @dev 获取仓单所有者哈希
     * @param receiptId 仓单ID
     * @return ownerHash 仓单所有者哈希
     */
    function getOwnerHash(string calldata receiptId) external view returns (bytes32 ownerHash);
}
