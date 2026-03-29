// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title ILogisticsCore
 * @dev 物流核心合约接口
 *
 * 用于跨合约调用，定义物流委派单核心业务操作的接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
interface ILogisticsCore {

    // ==================== 数据结构 ====================

    /**
     * @dev 物流委派单状态枚举
     */
    enum LogisticsStatus {
        None,       // 0-不存在
        Pending,    // 1-待指派
        Assigned,   // 2-已调度
        InTransit,  // 3-运输中
        Delivered,  // 4-已交付
        Invalid     // 5-已失效
    }

    /**
     * @dev 业务场景枚举
     */
    enum BusinessScene {
        None,           // 0-不存在
        DirectTransfer, // 1-直接移库
        TransferAfterSale, // 2-转让后移库
        ShipToWarehouse // 3-发往指定仓库
    }

    /**
     * @dev 到货处理动作枚举
     */
    enum ArrivalAction {
        None,           // 0-无
        CreateNew,      // 1-生成新仓单
        AddExisting     // 2-并入已有仓单
    }

    // ==================== 核心操作 ====================

    /**
     * @dev 创建物流委派单（数组参数解决Stack too deep）
     * @param stringParams [0]=voucherNo, [1]=receiptId, [2]=unit
     * @param uintParams [0]=businessScene, [1]=transportQuantity, [2]=validUntil
     * @param bytesParams [0]=ownerHash, [1]=carrierHash, [2]=sourceWhHash, [3]=targetWhHash
     * @return success 是否成功
     */
    function createLogisticsDelegate(
        string[] calldata stringParams,
        uint256[] calldata uintParams,
        bytes32[] calldata bytesParams
    ) external returns (bool success);

    /**
     * @dev 指派承运方
     * @param voucherNo 委派单编号
     * @param carrierHash 承运方哈希
     * @return success 是否成功
     */
    function assignCarrier(string calldata voucherNo, bytes32 carrierHash)
        external returns (bool success);

    /**
     * @dev 更新物流状态
     * @param voucherNo 委派单编号
     * @param newStatus 新状态
     * @return success 是否成功
     */
    function updateStatus(string calldata voucherNo, LogisticsStatus newStatus)
        external returns (bool success);

    /**
     * @dev 确认交付
     * @param voucherNo 委派单编号
     * @param action 到货处理动作
     * @param targetReceiptId 目标仓单ID（增量入库时使用）
     * @return success 是否成功
     */
    function confirmDelivery(
        string calldata voucherNo,
        ArrivalAction action,
        string calldata targetReceiptId
    ) external returns (bool success);

    /**
     * @dev 使委派单失效
     * @param voucherNo 委派单编号
     * @return success 是否成功
     */
    function invalidate(string calldata voucherNo) external returns (bool success);

    // ==================== 查询操作 ====================

    /**
     * @dev 获取委派单状态
     * @param voucherNo 委派单编号
     * @return status 委派单状态
     */
    function getStatus(string calldata voucherNo) external view returns (LogisticsStatus status);

    /**
     * @dev 检查委派单是否存在
     * @param voucherNo 委派单编号
     * @return exists 是否存在
     */
    function exists(string calldata voucherNo) external view returns (bool exists);

    /**
     * @dev 检查委派单是否有效
     * @param voucherNo 委派单编号
     * @return isValid 是否有效
     */
    function isValid(string calldata voucherNo) external view returns (bool isValid);

    /**
     * @dev 获取货主哈希
     * @param voucherNo 委派单编号
     * @return ownerHash 货主哈希
     */
    function getOwnerHash(string calldata voucherNo) external view returns (bytes32 ownerHash);

    // ==================== 仓单联动操作 ====================

    /**
     * @dev 提货时扣减仓单重量
     * @param voucherNo 委派单编号
     * @param quantity 扣减数量
     * @return success 是否成功
     */
    function pickup(string calldata voucherNo, uint256 quantity)
        external returns (bool success);

    /**
     * @dev 到货时生成新仓单
     * @param voucherNo 委派单编号
     * @param newReceiptId 新仓单ID
     * @param weight 新仓单重量
     * @param unit 计量单位
     * @param ownerHash 货主哈希
     * @param warehouseHash 仓库哈希
     * @return success 是否成功
     */
    function arriveAndCreateReceipt(
        string calldata voucherNo,
        string calldata newReceiptId,
        uint256 weight,
        string calldata unit,
        bytes32 ownerHash,
        bytes32 warehouseHash
    ) external returns (bool success);

    /**
     * @dev 到货时增量入库
     * @param voucherNo 委派单编号
     * @param targetReceiptId 目标仓单ID
     * @param quantity 增量数量
     * @return success 是否成功
     */
    function arriveAndAddQuantity(
        string calldata voucherNo,
        string calldata targetReceiptId,
        uint256 quantity
    ) external returns (bool success);
}
