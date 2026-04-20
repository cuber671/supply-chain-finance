// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptHistory
 * @dev 仓单历史记录合约
 *
 * 记录仓单的所有变更操作，确保数据可追溯
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract WarehouseReceiptHistory {

    // ==================== 常量定义 ====================

    uint256 constant MAX_HISTORY_PER_RECEIPT = 1000;
    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    uint256 public historyCount;

    // ==================== 数据结构 ====================

    /**
     * @dev 操作类型枚举
     */
    enum OperationType {
        Issued,           // 开立
        Transferred,       // 转移
        Locked,           // 锁定
        Unlocked,         // 解锁
        Split,            // 拆分
        Merged,           // 合并
        Endorsed,         // 背书
        Cancelled,        // 注销
        Delivered,        // 提货
        Frozen,           // 冻结
        Unfrozen,         // 解冻
        WeightAdjusted,   // 重量调整
        StatusChanged     // 状态变更
    }

    /**
     * @dev 变更记录结构
     */
    struct HistoryRecord {
        string receiptId;
        OperationType operationType;
        bytes32 operatorHash;
        uint256 timestamp;
        uint256 blockNumber;
        bytes32 txHash;
        string description;
    }

    /**
     * @dev 变更详情（扩展信息）
     */
    struct OperationDetail {
        string beforeValue;
        string afterValue;
        bytes32 relatedHash;
    }

    // ==================== 存储层 ====================

    // 仓单ID -> 历史记录数组
    mapping(string => HistoryRecord[]) private receiptHistory;
    // 仓单ID -> 记录数量
    mapping(string => uint256) private receiptHistoryCount;
    // 全局历史索引
    mapping(uint256 => HistoryRecord) private globalHistory;

    // ==================== 事件定义 ====================

    event HistoryRecorded(
        string indexed receiptId,
        uint8 indexed operationType,
        bytes32 operatorHash,
        uint256 timestamp,
        uint256 blockNumber
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

    constructor(address _admin) {
        require(_admin != address(0), "Admin cannot be zero");
        admin = _admin;
    }

    // ==================== 记录功能 ====================

    /**
     * @dev 记录仓单变更
     * @param receiptId 仓单ID
     * @param operationType 操作类型
     * @param operatorHash 操作者哈希
     * @param description 操作描述
     * @return recordId 记录ID
     */
    function recordHistory(
        string calldata receiptId,
        OperationType operationType,
        bytes32 operatorHash,
        string calldata description
    )
        external onlyValidReceiptId(receiptId) returns (uint256 recordId)
    {
        require(operatorHash != bytes32(0), "Invalid operator hash");

        // 检查记录数量限制
        require(
            receiptHistoryCount[receiptId] < MAX_HISTORY_PER_RECEIPT,
            "History limit reached"
        );

        uint256 timestamp = block.timestamp;
        uint256 blockNum = block.number;
        bytes32 txHash = blockhash(blockNum - 1);

        // 创建记录
        HistoryRecord memory record = HistoryRecord({
            receiptId: receiptId,
            operationType: operationType,
            operatorHash: operatorHash,
            timestamp: timestamp,
            blockNumber: blockNum,
            txHash: txHash,
            description: description
        });

        // 存储记录
        receiptHistory[receiptId].push(record);
        receiptHistoryCount[receiptId]++;

        // 存储全局索引
        recordId = historyCount;
        globalHistory[recordId] = record;
        historyCount++;

        emit HistoryRecorded(
            receiptId,
            uint8(operationType),
            operatorHash,
            timestamp,
            blockNum
        );

        return recordId;
    }

    /**
     * @dev 记录带详情的变更
     * @param receiptId 仓单ID
     * @param operationType 操作类型
     * @param operatorHash 操作者哈希
     * @param beforeValue 变更前值
     * @param afterValue 变更后值
     * @param relatedHash 关联哈希
     * @return recordId 记录ID
     */
    function recordHistoryWithDetail(
        string calldata receiptId,
        OperationType operationType,
        bytes32 operatorHash,
        string calldata beforeValue,
        string calldata afterValue,
        bytes32 relatedHash
    )
        external onlyValidReceiptId(receiptId) returns (uint256 recordId)
    {
        // 构建描述
        string memory description = string(
            abi.encodePacked(beforeValue, "->", afterValue)
        );

        return this.recordHistory(receiptId, operationType, operatorHash, description);
    }

    /**
     * @dev 批量记录变更
     * @param receiptIds 仓单ID数组
     * @param operationTypes 操作类型数组
     * @param operatorHashes 操作者哈希数组
     * @param descriptions 描述数组
     * @return success 是否成功
     */
    function batchRecordHistory(
        string[] calldata receiptIds,
        OperationType[] calldata operationTypes,
        bytes32[] calldata operatorHashes,
        string[] calldata descriptions
    )
        external returns (bool success)
    {
        require(receiptIds.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(
            receiptIds.length == operationTypes.length &&
            receiptIds.length == operatorHashes.length &&
            receiptIds.length == descriptions.length,
            "Length mismatch"
        );

        for (uint256 i = 0; i < receiptIds.length; i++) {
            this.recordHistory(
                receiptIds[i],
                operationTypes[i],
                operatorHashes[i],
                descriptions[i]
            );
        }

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取仓单历史记录数量
     * @param receiptId 仓单ID
     * @return 记录数量
     */
    function getHistoryCount(string calldata receiptId)
        external view returns (uint256)
    {
        return receiptHistoryCount[receiptId];
    }

    /**
     * @dev 获取仓单历史记录（分页）
     * @param receiptId 仓单ID
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 历史记录数组
     */
    function getHistory(string calldata receiptId, uint256 offset, uint256 limit)
        external view returns (HistoryRecord[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");

        uint256 total = receiptHistoryCount[receiptId];
        if (offset >= total) {
            return new HistoryRecord[](0);
        }

        uint256 resultLength = total - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        HistoryRecord[] memory result = new HistoryRecord[](resultLength);
        HistoryRecord[] storage history = receiptHistory[receiptId];

        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = history[offset + i];
        }

        return result;
    }

    /**
     * @dev 获取单条历史记录
     * @param receiptId 仓单ID
     * @param index 记录索引
     */
    function getHistoryByIndex(string calldata receiptId, uint256 index)
        external view returns (
            string memory _receiptId,
            uint8 operationType,
            bytes32 operatorHash,
            uint256 timestamp,
            uint256 blockNumber,
            bytes32 txHash,
            string memory description
        )
    {
        require(index < receiptHistoryCount[receiptId], "Index out of bounds");
        HistoryRecord storage record = receiptHistory[receiptId][index];

        return (
            record.receiptId,
            uint8(record.operationType),
            record.operatorHash,
            record.timestamp,
            record.blockNumber,
            record.txHash,
            record.description
        );
    }

    /**
     * @dev 获取最新历史记录
     * @param receiptId 仓单ID
     */
    function getLatestHistory(string calldata receiptId)
        external view returns (
            string memory _receiptId,
            uint8 operationType,
            bytes32 operatorHash,
            uint256 timestamp,
            uint256 blockNumber,
            bytes32 txHash,
            string memory description
        )
    {
        uint256 count = receiptHistoryCount[receiptId];
        require(count > 0, "No history");

        HistoryRecord storage record = receiptHistory[receiptId][count - 1];

        return (
            record.receiptId,
            uint8(record.operationType),
            record.operatorHash,
            record.timestamp,
            record.blockNumber,
            record.txHash,
            record.description
        );
    }

    /**
     * @dev 根据操作类型查询历史
     * @param receiptId 仓单ID
     * @param operationType 操作类型
     * @return 符合条件的历史记录
     */
    function getHistoryByOperationType(
        string calldata receiptId,
        OperationType operationType
    )
        external view returns (HistoryRecord[] memory)
    {
        uint256 total = receiptHistoryCount[receiptId];
        if (total == 0) {
            return new HistoryRecord[](0);
        }

        // 统计符合条件记录数
        uint256 matchCount = 0;
        HistoryRecord[] storage history = receiptHistory[receiptId];
        for (uint256 i = 0; i < total; i++) {
            if (history[i].operationType == operationType) {
                matchCount++;
            }
        }

        // 构建结果
        HistoryRecord[] memory result = new HistoryRecord[](matchCount);
        uint256 index = 0;
        for (uint256 i = 0; i < total; i++) {
            if (history[i].operationType == operationType) {
                result[index] = history[i];
                index++;
            }
        }

        return result;
    }

    /**
     * @dev 获取全局历史记录
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 历史记录数组
     */
    function getGlobalHistory(uint256 offset, uint256 limit)
        external view returns (HistoryRecord[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");

        uint256 total = historyCount;
        if (offset >= total) {
            return new HistoryRecord[](0);
        }

        uint256 resultLength = total - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        HistoryRecord[] memory result = new HistoryRecord[](resultLength);
        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = globalHistory[offset + i];
        }

        return result;
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }
}
