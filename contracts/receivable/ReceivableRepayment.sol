// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./IReceivableCore.sol";

/**
 * @title ReceivableRepayment
 * @dev 应收款还款管理合约
 *
 * 管理应收款的还款操作，包括还款记录、债务抵消等
 * 依赖于 ReceivableCore 合约
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract ReceivableRepayment {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;
    uint256 constant MAX_REPAYMENT_RECORDS = 1000;

    // ==================== 状态变量 ====================

    address public admin;
    address public receivableCore;
    uint256 public repaymentRecordCount;

    // ==================== 数据结构 ====================

    /**
     * @dev 还款记录结构
     */
    struct RepaymentRecord {
        string receivableId;
        uint256 amount;
        uint256 timestamp;
        address payer;
        string paymentMethod;
        bytes32 txHash;
    }

    /**
     * @dev 债务抵消记录结构
     */
    struct OffsetRecord {
        string sourceReceivableId;
        string targetReceivableId;
        uint256 offsetAmount;
        uint256 timestamp;
        address operator;
        string reason;
    }

    // ==================== 存储层 ====================

    // 应收款ID -> 还款记录数组
    mapping(string => RepaymentRecord[]) private repaymentRecords;
    // 应收款ID -> 还款记录数量
    mapping(string => uint256) private repaymentRecordCountByReceivable;
    // 全局抵消记录
    mapping(uint256 => OffsetRecord) private offsetRecords;
    uint256 private offsetRecordCount;

    // ==================== 事件定义 ====================

    event RepaymentRecorded(
        string indexed receivableId,
        uint256 amount,
        address indexed payer,
        string paymentMethod,
        uint256 timestamp
    );

    event DebtOffset(
        string indexed sourceReceivableId,
        string indexed targetReceivableId,
        uint256 offsetAmount,
        address indexed operator,
        uint256 timestamp
    );

    event ReceivableSettled(
        string indexed receivableId,
        uint256 totalRepaid,
        uint256 timestamp
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyValidReceivableId(string calldata receivableId) {
        require(bytes(receivableId).length > 0, "Invalid receivable ID");
        _;
    }

    /**
     * @dev 调用Core合约更新余额（辅助函数，用于减少栈深度）
     */
    function _updateReceivableBalance(string memory receivableId, uint256 amount, bool isFull) internal {
        IReceivableCore(receivableCore).updateBalance(receivableId, amount, isFull);
    }

    // ==================== 构造函数 ====================

    constructor(address _admin, address _receivableCore) {
        require(_admin != address(0), "Admin cannot be zero");
        require(_receivableCore != address(0), "Core contract cannot be zero");
        admin = _admin;
        receivableCore = _receivableCore;
    }

    // ==================== 还款功能 ====================

    /**
     * @dev 记录还款
     * @param receivableId 应收款ID
     * @param amount 还款金额
     * @param paymentMethod 还款方式
     * @return success 是否成功
     */
    function recordRepayment(
        string calldata receivableId,
        uint256 amount,
        string calldata paymentMethod
    )
        external onlyValidReceivableId(receivableId) returns (bool success)
    {
        require(amount > 0, "Invalid amount");
        require(bytes(paymentMethod).length > 0, "Invalid payment method");

        // 获取当前余额并判断是否结清
        IReceivableCore core = IReceivableCore(receivableCore);
        uint256 currentBalance = core.getBalanceUnpaid(receivableId);
        bool isFull = (amount >= currentBalance);

        // 调用 Core 合约更新余额
        core.updateBalance(receivableId, amount, isFull);

        // 记录还款
        RepaymentRecord memory record = RepaymentRecord({
            receivableId: receivableId,
            amount: amount,
            timestamp: block.timestamp,
            payer: msg.sender,
            paymentMethod: paymentMethod,
            txHash: blockhash(block.number - 1)
        });

        repaymentRecords[receivableId].push(record);
        repaymentRecordCountByReceivable[receivableId]++;
        repaymentRecordCount++;

        emit RepaymentRecorded(
            receivableId,
            amount,
            msg.sender,
            paymentMethod,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 批量记录还款
     * @param receivableIds 应收款ID数组
     * @param amounts 还款金额数组
     * @param paymentMethods 还款方式数组
     * @return success 是否成功
     */
    function batchRecordRepayment(
        string[] calldata receivableIds,
        uint256[] calldata amounts,
        string[] calldata paymentMethods
    )
        external returns (bool success)
    {
        require(receivableIds.length <= MAX_BATCH_SIZE, "Batch size too large");
        require(
            receivableIds.length == amounts.length &&
            receivableIds.length == paymentMethods.length,
            "Length mismatch"
        );

        // 验证调用者是所有应收款的债务人
        bytes32 senderHash = bytes32(uint256(uint160(msg.sender)));
        IReceivableCore core = IReceivableCore(receivableCore);
        for (uint256 i = 0; i < receivableIds.length; i++) {
            require(
                core.getDebtorHash(receivableIds[i]) == senderHash,
                "Not debtor"
            );
        }

        for (uint256 i = 0; i < receivableIds.length; i++) {
            this.recordRepayment(receivableIds[i], amounts[i], paymentMethods[i]);
        }

        return true;
    }

    /**
     * @dev 记录现金还款（直接结清）
     * @param receivableId 应收款ID
     * @param amount 还款金额
     * @return success 是否成功
     */
    function recordFullRepayment(string calldata receivableId, uint256 amount)
        external onlyValidReceivableId(receivableId) returns (bool success)
    {
        require(amount > 0, "Invalid amount");

        // 记录还款
        RepaymentRecord memory record = RepaymentRecord({
            receivableId: receivableId,
            amount: amount,
            timestamp: block.timestamp,
            payer: msg.sender,
            paymentMethod: "CASH",
            txHash: blockhash(block.number - 1)
        });

        repaymentRecords[receivableId].push(record);
        repaymentRecordCountByReceivable[receivableId]++;
        repaymentRecordCount++;

        emit RepaymentRecorded(
            receivableId,
            amount,
            msg.sender,
            "CASH",
            block.timestamp
        );

        // 调用 Core 合约更新余额（标记为结清）
        IReceivableCore(receivableCore).updateBalance(receivableId, amount, true);

        return true;
    }

    // ==================== 债务抵消功能 ====================

    /**
     * @dev 债务抵消
     * @param sourceReceivableId 源应收款ID（付款方）
     * @param targetReceivableId 目标应收款ID（收款方）
     * @param offsetAmount 抵消金额
     * @param reason 抵消原因
     * @return success 是否成功
     */
    function offsetDebtWithCollateral(
        string calldata sourceReceivableId,
        string calldata targetReceivableId,
        uint256 offsetAmount,
        string calldata reason
    )
        external onlyValidReceivableId(sourceReceivableId)
        onlyValidReceivableId(targetReceivableId) returns (bool success)
    {
        require(offsetAmount > 0, "Invalid amount");
        require(bytes(reason).length > 0, "Invalid reason");
        require(
            keccak256(abi.encodePacked(sourceReceivableId)) !=
            keccak256(abi.encodePacked(targetReceivableId)),
            "Same receivable"
        );

        // 记录抵消
        OffsetRecord memory record = OffsetRecord({
            sourceReceivableId: sourceReceivableId,
            targetReceivableId: targetReceivableId,
            offsetAmount: offsetAmount,
            timestamp: block.timestamp,
            operator: msg.sender,
            reason: reason
        });

        offsetRecords[offsetRecordCount] = record;
        offsetRecordCount++;

        // 调用 Core 合约更新源应收款余额（减少）- 使用子作用域
        {
            uint256 sourceBalance = IReceivableCore(receivableCore).getBalanceUnpaid(sourceReceivableId);
            bool isFullSource = (offsetAmount >= sourceBalance);
            _updateReceivableBalance(sourceReceivableId, offsetAmount, isFullSource);
        }

        emit DebtOffset(
            sourceReceivableId,
            targetReceivableId,
            offsetAmount,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取还款记录数量
     * @param receivableId 应收款ID
     * @return 记录数量
     */
    function getRepaymentRecordCount(string calldata receivableId)
        external view returns (uint256)
    {
        return repaymentRecordCountByReceivable[receivableId];
    }

    /**
     * @dev 获取还款记录（分页）
     * @param receivableId 应收款ID
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 还款记录数组
     */
    function getRepaymentRecords(string calldata receivableId, uint256 offset, uint256 limit)
        external view returns (RepaymentRecord[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");

        uint256 total = repaymentRecordCountByReceivable[receivableId];
        if (offset >= total) {
            return new RepaymentRecord[](0);
        }

        uint256 resultLength = total - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        RepaymentRecord[] memory result = new RepaymentRecord[](resultLength);
        RepaymentRecord[] storage records = repaymentRecords[receivableId];

        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = records[offset + i];
        }

        return result;
    }

    /**
     * @dev 获取总还款额
     * @param receivableId 应收款ID
     */
    function getTotalRepaid(string calldata receivableId)
        public view returns (uint256 totalRepaid)
    {
        RepaymentRecord[] storage records = repaymentRecords[receivableId];
        for (uint256 i = 0; i < records.length; i++) {
            totalRepaid += records[i].amount;
        }
    }

    /**
     * @dev 获取最新还款记录
     * @param receivableId 应收款ID
     */
    function getLatestRepayment(string calldata receivableId)
        external view returns (
            string memory _receivableId,
            uint256 amount,
            address payer,
            string memory paymentMethod,
            uint256 timestamp
        )
    {
        uint256 count = repaymentRecordCountByReceivable[receivableId];
        require(count > 0, "No records");

        RepaymentRecord storage record = repaymentRecords[receivableId][count - 1];
        return (
            record.receivableId,
            record.amount,
            record.payer,
            record.paymentMethod,
            record.timestamp
        );
    }

    /**
     * @dev 获取抵消记录数量
     * @return 记录数量
     */
    function getOffsetRecordCount() external view returns (uint256) {
        return offsetRecordCount;
    }

    /**
     * @dev 获取抵消记录
     * @param offset 起始索引
     * @param limit 数量限制
     * @return 抵消记录数组
     */
    function getOffsetRecords(uint256 offset, uint256 limit)
        external view returns (OffsetRecord[] memory)
    {
        require(limit <= MAX_BATCH_SIZE, "Limit too large");

        uint256 total = offsetRecordCount;
        if (offset >= total) {
            return new OffsetRecord[](0);
        }

        uint256 resultLength = total - offset;
        if (resultLength > limit) {
            resultLength = limit;
        }

        OffsetRecord[] memory result = new OffsetRecord[](resultLength);
        for (uint256 i = 0; i < resultLength; i++) {
            result[i] = offsetRecords[offset + i];
        }

        return result;
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setReceivableCore(address newCore) external onlyAdmin returns (bool) {
        require(newCore != address(0), "Invalid address");
        receivableCore = newCore;
        return true;
    }
}
