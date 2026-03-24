// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./IWarehouseReceiptCore.sol";

/**
 * @title WarehouseReceiptOps
 * @dev 仓单操作合约
 *
 * 管理仓单的高级操作，包括拆分、合并、背书等
 * 依赖于 WarehouseReceiptCore 合约
 *
 * 栈溢出防护：
 * - 使用 Struct 封装参数
 * - 使用 calldata 传参
 * - 批量操作限制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract WarehouseReceiptOps {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;
    uint256 constant MAX_SPLIT_COUNT = 10;

    // ==================== 状态变量 ====================

    address public admin;
    address public warehouseReceiptCore;

    // ==================== 数据结构 ====================

    /**
     * @dev 拆分输入参数
     */
    struct SplitInput {
        string originalReceiptId;
        string[] newReceiptIds;
        uint256[] weights;
        bytes32[] ownerHashes;
        string unit;
    }

    /**
     * @dev 合并输入参数
     */
    struct MergeInput {
        string[] sourceReceiptIds;
        string targetReceiptId;
        bytes32 targetOwnerHash;
        string unit;
    }

    /**
     * @dev 背书输入参数
     */
    struct EndorsementInput {
        string receiptId;
        bytes32 fromHash;
        bytes32 toHash;
        string endorsementType;
    }

    // ==================== 事件定义 ====================

    event ReceiptSplit(
        string indexed originalReceiptId,
        string[] newReceiptIds,
        uint256[] weights,
        uint256 timestamp
    );

    event ReceiptMerged(
        string[] indexed sourceReceiptIds,
        string indexed targetReceiptId,
        uint256 totalWeight,
        uint256 timestamp
    );

    event EndorsementLaunched(
        string indexed receiptId,
        bytes32 indexed fromHash,
        bytes32 toHash,
        string endorsementType,
        address indexed endorser,
        uint256 timestamp
    );

    event EndorsementConfirmed(
        string indexed receiptId,
        bytes32 indexed fromHash,
        bytes32 toHash,
        address indexed newOwner,
        uint256 timestamp
    );

    event EndorsementRejected(
        string indexed receiptId,
        bytes32 indexed fromHash,
        address indexed rejector,
        string reason,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyValidReceiptId(string calldata receiptId) {
        require(bytes(receiptId).length > 0, "Invalid receipt ID");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _admin, address _warehouseReceiptCore) {
        require(_admin != address(0), "Admin cannot be zero");
        require(_warehouseReceiptCore != address(0), "Core contract cannot be zero");
        admin = _admin;
        warehouseReceiptCore = _warehouseReceiptCore;
    }

    // ==================== 拆分功能 ====================

    /**
     * @dev 拆分仓单
     * @param input 拆分参数
     * @return success 是否成功
     */
    function splitReceipt(SplitInput calldata input)
        external onlyValidReceiptId(input.originalReceiptId) returns (bool success)
    {
        // 验证拆分数量
        require(input.newReceiptIds.length > 0 && input.newReceiptIds.length <= MAX_SPLIT_COUNT, "Invalid split count");
        require(input.newReceiptIds.length == input.weights.length, "Length mismatch");
        require(input.newReceiptIds.length == input.ownerHashes.length, "Length mismatch");
        require(bytes(input.unit).length > 0, "Invalid unit");

        // 验证权重总和
        uint256 totalWeight = 0;
        for (uint256 i = 0; i < input.weights.length; i++) {
            require(input.weights[i] > 0, "Invalid weight");
            require(input.ownerHashes[i] != bytes32(0), "Invalid owner hash");
            totalWeight += input.weights[i];
        }

        // 调用 Core 合约执行拆分
        IWarehouseReceiptCore.SplitInput memory coreInput = IWarehouseReceiptCore.SplitInput({
            originalReceiptId: input.originalReceiptId,
            newReceiptIds: input.newReceiptIds,
            weights: input.weights,
            ownerHashes: input.ownerHashes,
            unit: input.unit
        });

        IWarehouseReceiptCore(warehouseReceiptCore).splitReceipt(coreInput);

        emit ReceiptSplit(
            input.originalReceiptId,
            input.newReceiptIds,
            input.weights,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 批量拆分仓单
     * @param inputs 拆分参数数组
     * @return success 是否成功
     */
    function batchSplitReceipt(SplitInput[] calldata inputs)
        external returns (bool success)
    {
        require(inputs.length <= MAX_BATCH_SIZE, "Batch size too large");

        // 验证调用者是所有原仓单的持有人
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        IWarehouseReceiptCore core = IWarehouseReceiptCore(warehouseReceiptCore);
        for (uint256 i = 0; i < inputs.length; i++) {
            require(
                core.getOwnerHash(inputs[i].originalReceiptId) == senderHash,
                "Not owner"
            );
        }

        for (uint256 i = 0; i < inputs.length; i++) {
            IWarehouseReceiptCore.SplitInput memory coreInput = IWarehouseReceiptCore.SplitInput({
                originalReceiptId: inputs[i].originalReceiptId,
                newReceiptIds: inputs[i].newReceiptIds,
                weights: inputs[i].weights,
                ownerHashes: inputs[i].ownerHashes,
                unit: inputs[i].unit
            });
            core.splitReceipt(coreInput);
        }

        return true;
    }

    // ==================== 合并功能 ====================

    /**
     * @dev 合并仓单
     * @param input 合并参数
     * @return success 是否成功
     */
    function mergeReceipts(MergeInput calldata input)
        external returns (bool success)
    {
        // 验证调用者是所有源仓单的持有人
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        IWarehouseReceiptCore core = IWarehouseReceiptCore(warehouseReceiptCore);
        for (uint256 i = 0; i < input.sourceReceiptIds.length; i++) {
            require(
                core.getOwnerHash(input.sourceReceiptIds[i]) == senderHash,
                "Not owner"
            );
        }

        // 验证合并数量
        require(input.sourceReceiptIds.length > 1 && input.sourceReceiptIds.length <= MAX_SPLIT_COUNT, "Invalid merge count");
        require(bytes(input.targetReceiptId).length > 0, "Invalid target receipt ID");
        require(input.targetOwnerHash != bytes32(0), "Invalid owner hash");
        require(bytes(input.unit).length > 0, "Invalid unit");

        // 从 Core 合约获取各仓单重量并累加
        uint256 totalWeight = 0;
        for (uint256 i = 0; i < input.sourceReceiptIds.length; i++) {
            totalWeight += core.getReceiptWeight(input.sourceReceiptIds[i]);
        }

        // 调用 Core 合约执行合并
        IWarehouseReceiptCore.MergeInput memory coreInput = IWarehouseReceiptCore.MergeInput({
            sourceReceiptIds: input.sourceReceiptIds,
            targetReceiptId: input.targetReceiptId,
            targetOwnerHash: input.targetOwnerHash,
            unit: input.unit,
            totalWeight: totalWeight
        });

        core.mergeReceipts(coreInput);

        emit ReceiptMerged(
            input.sourceReceiptIds,
            input.targetReceiptId,
            totalWeight,
            block.timestamp
        );

        return true;
    }

    // ==================== 背书功能 ====================

    /**
     * @dev 发起背书
     * @param input 背书参数
     * @return success 是否成功
     */
    function launchEndorsement(EndorsementInput calldata input)
        external onlyValidReceiptId(input.receiptId) returns (bool success)
    {
        // 验证背书类型
        require(
            keccak256(abi.encodePacked(input.endorsementType)) == keccak256(abi.encodePacked("transfer")) ||
            keccak256(abi.encodePacked(input.endorsementType)) == keccak256(abi.encodePacked("pledge")),
            "Invalid endorsement type"
        );

        // 验证哈希
        require(input.fromHash != bytes32(0), "Invalid from hash");
        require(input.toHash != bytes32(0), "Invalid to hash");
        require(input.fromHash != input.toHash, "Same owner");

        emit EndorsementLaunched(
            input.receiptId,
            input.fromHash,
            input.toHash,
            input.endorsementType,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 确认背书
     * @param receiptId 仓单ID
     * @param fromHash 原持有人哈希
     * @param toHash 新持有人哈希
     * @return success 是否成功
     */
    function confirmEndorsement(
        string calldata receiptId,
        bytes32 fromHash,
        bytes32 toHash
    )
        external onlyValidReceiptId(receiptId) returns (bool success)
    {
        require(fromHash != bytes32(0), "Invalid from hash");
        require(toHash != bytes32(0), "Invalid to hash");

        // 调用 Core 合约更新仓单所有者
        IWarehouseReceiptCore(warehouseReceiptCore).updateOwner(receiptId, toHash);

        emit EndorsementConfirmed(
            receiptId,
            fromHash,
            toHash,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 拒绝背书
     * @param receiptId 仓单ID
     * @param fromHash 原持有人哈希
     * @param reason 拒绝原因
     * @return success 是否成功
     */
    function rejectEndorsement(
        string calldata receiptId,
        bytes32 fromHash,
        string calldata reason
    )
        external onlyValidReceiptId(receiptId) returns (bool success)
    {
        require(fromHash != bytes32(0), "Invalid from hash");

        emit EndorsementRejected(
            receiptId,
            fromHash,
            msg.sender,
            reason,
            block.timestamp
        );

        return true;
    }

    // ==================== 批量操作 ====================

    /**
     * @dev 批量发起背书
     * @param inputs 背书参数数组
     * @return success 是否成功
     */
    function batchLaunchEndorsement(EndorsementInput[] calldata inputs)
        external returns (bool success)
    {
        require(inputs.length <= MAX_BATCH_SIZE, "Batch size too large");

        // 验证调用者是所有仓单的持有人
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        IWarehouseReceiptCore core = IWarehouseReceiptCore(warehouseReceiptCore);
        for (uint256 i = 0; i < inputs.length; i++) {
            require(
                core.getOwnerHash(inputs[i].receiptId) == senderHash,
                "Not owner"
            );
        }

        for (uint256 i = 0; i < inputs.length; i++) {
            this.launchEndorsement(inputs[i]);
        }

        return true;
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setWarehouseReceiptCore(address newCore) external onlyAdmin returns (bool) {
        require(newCore != address(0), "Invalid address");
        warehouseReceiptCore = newCore;
        return true;
    }
}
