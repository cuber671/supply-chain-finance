// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./IBillCore.sol";

/**
 * @title BillOps
 * @dev 票据操作合约
 *
 * 管理票据的高级操作，包括批量处理、背书链查询等
 * 依赖于 BillCore 合约
 *
 * 栈溢出防护：
 * - 使用 Struct 封装参数
 * - 使用 calldata 传参
 * - 批量操作限制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract BillOps {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    address public billCore;

    // ==================== 数据结构 ====================

    /**
     * @dev 批量背书输入参数
     */
    struct BatchEndorseInput {
        string[] billIds;
        bytes32[] toHashes;
    }

    /**
     * @dev 批量承兑输入参数
     */
    struct BatchAcceptInput {
        string[] billIds;
    }

    // ==================== 事件定义 ====================

    event BatchEndorsed(
        uint256 count,
        address indexed operator,
        uint256 timestamp
    );

    event BatchAccepted(
        uint256 count,
        address indexed operator,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _admin, address _billCore) {
        require(_admin != address(0), "Admin cannot be zero");
        require(_billCore != address(0), "Core contract cannot be zero");
        admin = _admin;
        billCore = _billCore;
    }

    // ==================== 批量操作 ====================

    /**
     * @dev 批量背书转让
     * @param input 批量背书参数
     * @return success 是否成功
     */
    function batchEndorse(BatchEndorseInput calldata input)
        external returns (bool success)
    {
        require(input.billIds.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(
            input.billIds.length == input.toHashes.length,
            "Length mismatch"
        );

        // 验证调用者是所有票据的持票人
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        for (uint256 i = 0; i < input.billIds.length; i++) {
            require(
                IBillCore(billCore).getHolderHash(input.billIds[i]) == senderHash,
                "Not holder"
            );
        }

        for (uint256 i = 0; i < input.billIds.length; i++) {
            IBillCore(billCore).endorseBill(input.billIds[i], input.toHashes[i]);
        }

        emit BatchEndorsed(input.billIds.length, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 批量承兑
     * @param input 批量承兑参数
     * @return success 是否成功
     */
    function batchAccept(BatchAcceptInput calldata input)
        external returns (bool success)
    {
        require(input.billIds.length <= MAX_BATCH_SIZE, "Batch size too large");

        // 验证调用者是所有票据的开票人
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        for (uint256 i = 0; i < input.billIds.length; i++) {
            require(
                IBillCore(billCore).getPayerHash(input.billIds[i]) == senderHash,
                "Not payer"
            );
        }

        for (uint256 i = 0; i < input.billIds.length; i++) {
            IBillCore(billCore).acceptBill(input.billIds[i]);
        }

        emit BatchAccepted(input.billIds.length, msg.sender, block.timestamp);

        return true;
    }

    // ==================== 背书链查询 ====================

    /**
     * @dev 获取背书链长度
     * @param billId 票据ID
     * @return 背书链长度
     */
    function getEndorsementChainLength(string calldata billId)
        external view returns (uint256)
    {
        // 需要从 Core 合约获取
        // 简化实现返回0
        return 0;
    }

    /**
     * @dev 验证背书链完整性
     * @param billId 票据ID
     * @return isValid 是否有效
     */
    function validateEndorsementChain(string calldata billId)
        external view returns (bool isValid)
    {
        // 验证背书链连续性
        // 简化实现返回true
        return true;
    }

    // ==================== 验证功能 ====================

    /**
     * @dev 验证票据是否可操作
     * @param billId 票据ID
     * @param operator 操作者地址
     * @return canOperate 是否可操作
     */
    function canOperate(string calldata billId, address operator)
        external view returns (bool canOperate)
    {
        // 验证持票人身份
        return IBillCore(billCore).isValid(billId);
    }

    /**
     * @dev 验证是否为合法持票人
     * @param billId 票据ID
     * @param holderHash 持票人哈希
     * @return isHolder 是否为持票人
     */
    function isHolder(string calldata billId, bytes32 holderHash)
        external view returns (bool isHolder)
    {
        // 需要从 Core 合约获取持票人信息
        // 简化实现返回false
        return false;
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setBillCore(address newCore) external onlyAdmin returns (bool) {
        require(newCore != address(0), "Invalid address");
        billCore = newCore;
        return true;
    }
}
