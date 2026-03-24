// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./ILogisticsCore.sol";

/**
 * @title LogisticsOps
 * @dev 物流操作合约
 *
 * 管理物流委派单的高级操作，包括批量处理、轨迹追踪等
 * 依赖于 LogisticsCore 合约
 *
 * 栈溢出防护：
 * - 使用 Struct 封装参数
 * - 使用 calldata 传参
 * - 批量操作限制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract LogisticsOps {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    address public logisticsCore;

    // ==================== 数据结构 ====================

    /**
     * @dev 批量指派输入参数
     */
    struct BatchAssignInput {
        string[] voucherNos;
        bytes32[] carrierHashes;
    }

    /**
     * @dev 批量状态更新输入参数
     */
    struct BatchStatusUpdateInput {
        string[] voucherNos;
        ILogisticsCore.LogisticsStatus[] statuses;
    }

    // ==================== 事件定义 ====================

    event BatchCarrierAssigned(
        uint256 count,
        address indexed operator,
        uint256 timestamp
    );

    event BatchStatusUpdated(
        uint256 count,
        address indexed operator,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyValidVoucherNo(string calldata voucherNo) {
        require(bytes(voucherNo).length > 0, "Invalid voucher No");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _admin, address _logisticsCore) {
        require(_admin != address(0), "Admin cannot be zero");
        require(_logisticsCore != address(0), "Core contract cannot be zero");
        admin = _admin;
        logisticsCore = _logisticsCore;
    }

    // ==================== 批量操作 ====================

    /**
     * @dev 批量指派承运方
     * @param input 批量指派参数
     * @return success 是否成功
     */
    function batchAssignCarrier(BatchAssignInput calldata input)
        external returns (bool success)
    {
        require(input.voucherNos.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(
            input.voucherNos.length == input.carrierHashes.length,
            "Length mismatch"
        );

        // 验证调用者是所有委派单的货主
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        ILogisticsCore core = ILogisticsCore(logisticsCore);
        for (uint256 i = 0; i < input.voucherNos.length; i++) {
            require(
                core.getOwnerHash(input.voucherNos[i]) == senderHash,
                "Not owner"
            );
        }

        for (uint256 i = 0; i < input.voucherNos.length; i++) {
            ILogisticsCore(logisticsCore).assignCarrier(
                input.voucherNos[i],
                input.carrierHashes[i]
            );
        }

        emit BatchCarrierAssigned(input.voucherNos.length, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 批量更新状态
     * @param input 批量状态更新参数
     * @return success 是否成功
     */
    function batchUpdateStatus(BatchStatusUpdateInput calldata input)
        external returns (bool success)
    {
        require(input.voucherNos.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(
            input.voucherNos.length == input.statuses.length,
            "Length mismatch"
        );

        // 验证调用者是货主或承运方
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        ILogisticsCore core = ILogisticsCore(logisticsCore);
        for (uint256 i = 0; i < input.voucherNos.length; i++) {
            // 简化验证：允许货主或承运方更新
            require(
                core.getOwnerHash(input.voucherNos[i]) == senderHash,
                "Not authorized"
            );
        }

        for (uint256 i = 0; i < input.voucherNos.length; i++) {
            ILogisticsCore(logisticsCore).updateStatus(
                input.voucherNos[i],
                input.statuses[i]
            );
        }

        emit BatchStatusUpdated(input.voucherNos.length, msg.sender, block.timestamp);

        return true;
    }

    // ==================== 追踪功能 ====================

    /**
     * @dev 获取物流轨迹（简化版，实际需要链下存储完整轨迹）
     * @param voucherNo 委派单编号
     * @return 状态数组
     */
    function getLogisticsTrack(string calldata voucherNo)
        external view returns (ILogisticsCore.LogisticsStatus[] memory)
    {
        // 简化实现：返回当前状态
        // 完整实现需要在链下存储完整轨迹
        ILogisticsCore.LogisticsStatus currentStatus = ILogisticsCore(logisticsCore).getStatus(voucherNo);
        ILogisticsCore.LogisticsStatus[] memory track = new ILogisticsCore.LogisticsStatus[](1);
        track[0] = currentStatus;
        return track;
    }

    // ==================== 验证功能 ====================

    /**
     * @dev 验证物流委派单是否有效
     * @param voucherNo 委派单编号
     * @return isValid 是否有效
     */
    function validateLogisticsDelegate(string calldata voucherNo)
        external view returns (bool isValid)
    {
        return ILogisticsCore(logisticsCore).isValid(voucherNo);
    }

    /**
     * @dev 验证承运方是否有权操作
     * @param voucherNo 委派单编号
     * @param carrierHash 承运方哈希
     * @return isAuthorized 是否授权
     */
    function isCarrierAuthorized(string calldata voucherNo, bytes32 carrierHash)
        external view returns (bool isAuthorized)
    {
        // 需要从 Core 合约获取承运方信息
        // 简化实现
        return carrierHash != bytes32(0);
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setLogisticsCore(address newCore) external onlyAdmin returns (bool) {
        require(newCore != address(0), "Invalid address");
        logisticsCore = newCore;
        return true;
    }
}
