// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LoanRepayment
 * @dev 贷款还款合约
 *
 * 处理质押贷款的各种还款方式，包括：
 * - 现金还款 (Cash)
 * - 仓单抵债 (ReceiptOffset)
 * - 应收账款抵债 (ReceivableOffset)
 * - 质押物处置 (CollateralDisposal)
 * - 部分还款 (Partial)
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract LoanRepayment {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    uint256 public repaymentCount;
    uint256 public constant VERSION = 1;

    // ==================== 数据结构 ====================

    /**
     * @dev 还款类型枚举
     */
    enum RepaymentType {
        None,               // 0 - 不存在
        Cash,               // 1 - 现金还款
        ReceiptOffset,      // 2 - 仓单抵债
        ReceivableOffset,   // 3 - 应收账款抵债
        CollateralDisposal, // 4 - 质押物处置
        Partial             // 5 - 部分还款
    }

    /**
     * @dev 还款状态枚举
     */
    enum RepaymentStatus {
        Pending,    // 1 - 待确认
        Confirmed,  // 2 - 已确认
        Rejected    // 3 - 已拒绝
    }

    /**
     * @dev 还款记录结构
     */
    struct RepaymentRecord {
        string repaymentNo;              // 还款编号
        string loanNo;                   // 关联贷款编号
        uint256 principalAmount;        // 本金金额
        uint256 interestAmount;         // 利息金额
        uint256 penaltyAmount;           // 罚息金额
        uint256 totalAmount;             // 还款总金额
        RepaymentType repaymentType;    // 还款类型
        RepaymentStatus status;          // 还款状态
        uint256 repaymentTime;           // 实际还款时间
        address payer;                   // 还款人地址
        bytes32 dataHash;               // 数据哈希
    }

    // 使用独立mapping存储额外字段
    mapping(string => RepaymentRecord) private repaymentRecords;
    mapping(string => bool) private repaymentExists;
    mapping(string => string[]) private loanRepayments;
    mapping(string => uint256) private repaymentCreatedAt;

    // 仓单抵债记录
    mapping(string => ReceiptOffsetRecord) private receiptOffsetRecords;
    mapping(string => bool) private receiptOffsetExists;

    // 应收账款抵债记录
    mapping(string => ReceivableOffsetRecord) private receivableOffsetRecords;
    mapping(string => bool) private receivableOffsetExists;

    /**
     * @dev 仓单抵债记录
     */
    struct ReceiptOffsetRecord {
        string repaymentNo;
        string sourceLoanNo;
        string offsetReceiptId;
        uint256 offsetAmount;
        uint256 offsetTime;
    }

    /**
     * @dev 应收账款抵债记录
     */
    struct ReceivableOffsetRecord {
        string repaymentNo;
        string sourceLoanNo;
        string offsetReceivableId;
        uint256 offsetAmount;
        uint256 offsetTime;
    }

    /**
     * @dev 现金还款输入参数
     */
    struct CashRepaymentInput {
        string repaymentNo;
        string loanNo;
        uint256 principalAmount;
        uint256 interestAmount;
        uint256 penaltyAmount;
        bytes32 dataHash;
    }

    /**
     * @dev 仓单抵债输入参数
     */
    struct ReceiptOffsetInput {
        string repaymentNo;
        string sourceLoanNo;
        string offsetReceiptId;
        uint256 offsetAmount;
        bytes32 dataHash;
    }

    /**
     * @dev 应收账款抵债输入参数
     */
    struct ReceivableOffsetInput {
        string repaymentNo;
        string sourceLoanNo;
        string offsetReceivableId;
        uint256 offsetAmount;
        bytes32 dataHash;
    }

    // ==================== 存储层 ====================

    mapping(string => RepaymentRecord) public repaymentInfos;

    // ==================== 事件定义 ====================

    event RepaymentCreated(
        string indexed repaymentNo,
        string indexed loanNo,
        uint256 principalAmount,
        uint256 interestAmount,
        uint256 totalAmount,
        RepaymentType repaymentType,
        uint256 createTime
    );

    event RepaymentConfirmed(
        string indexed repaymentNo,
        uint256 confirmedAmount,
        uint256 confirmTime
    );

    event RepaymentRejected(
        string indexed repaymentNo,
        string rejectReason,
        uint256 rejectTime
    );

    event ReceiptOffsetRecorded(
        string indexed repaymentNo,
        string indexed sourceLoanNo,
        string offsetReceiptId,
        uint256 offsetAmount
    );

    event ReceivableOffsetRecorded(
        string indexed repaymentNo,
        string indexed sourceLoanNo,
        string offsetReceivableId,
        uint256 offsetAmount
    );

    event CollateralDisposalRecorded(
        string indexed repaymentNo,
        string indexed loanNo,
        string disposalMethod,
        uint256 disposalAmount
    );

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Only Java backend");
        _;
    }

    modifier onlyValidRepayment(string memory repaymentNo) {
        require(repaymentExists[repaymentNo], "Repayment not found");
        _;
    }

    modifier onlyPending(string memory repaymentNo) {
        require(
            repaymentRecords[repaymentNo].status == RepaymentStatus.Pending,
            "Repayment not pending"
        );
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
        javaBackend = _initialAdmin;
    }

    // ==================== 现金还款 ====================

    /**
     * @dev 记录现金还款
     * @param input 现金还款参数
     * @return success 是否成功
     */
    function recordCashRepayment(CashRepaymentInput calldata input)
        external onlyJavaBackend returns (bool success)
    {
        // 允许重复创建（幂等性）
        if (repaymentExists[input.repaymentNo]) {
            return true;
        }

        require(bytes(input.repaymentNo).length > 0, "repaymentNo is empty");
        require(bytes(input.loanNo).length > 0, "loanNo is empty");

        uint256 totalAmount = input.principalAmount + input.interestAmount + input.penaltyAmount;
        uint256 timestamp = block.timestamp;

        repaymentRecords[input.repaymentNo] = RepaymentRecord({
            repaymentNo: input.repaymentNo,
            loanNo: input.loanNo,
            principalAmount: input.principalAmount,
            interestAmount: input.interestAmount,
            penaltyAmount: input.penaltyAmount,
            totalAmount: totalAmount,
            repaymentType: RepaymentType.Cash,
            status: RepaymentStatus.Pending,
            repaymentTime: 0,
            payer: msg.sender,
            dataHash: input.dataHash
        });

        repaymentExists[input.repaymentNo] = true;
        loanRepayments[input.loanNo].push(input.repaymentNo);
        repaymentCreatedAt[input.repaymentNo] = timestamp;
        repaymentCount++;

        emit RepaymentCreated(
            input.repaymentNo,
            input.loanNo,
            input.principalAmount,
            input.interestAmount,
            totalAmount,
            RepaymentType.Cash,
            timestamp
        );

        return true;
    }

    // ==================== 仓单抵债 ====================

    /**
     * @dev 记录仓单抵债
     * @param input 仓单抵债参数
     * @return success 是否成功
     */
    function recordReceiptOffset(ReceiptOffsetInput calldata input)
        external onlyJavaBackend returns (bool success)
    {
        require(bytes(input.repaymentNo).length > 0, "repaymentNo is empty");
        require(bytes(input.sourceLoanNo).length > 0, "sourceLoanNo is empty");
        require(bytes(input.offsetReceiptId).length > 0, "offsetReceiptId is empty");
        require(input.offsetAmount > 0, "Invalid offset amount");

        uint256 timestamp = block.timestamp;

        receiptOffsetRecords[input.repaymentNo] = ReceiptOffsetRecord({
            repaymentNo: input.repaymentNo,
            sourceLoanNo: input.sourceLoanNo,
            offsetReceiptId: input.offsetReceiptId,
            offsetAmount: input.offsetAmount,
            offsetTime: timestamp
        });

        receiptOffsetExists[input.repaymentNo] = true;

        // 同时创建还款记录
        repaymentRecords[input.repaymentNo] = RepaymentRecord({
            repaymentNo: input.repaymentNo,
            loanNo: input.sourceLoanNo,
            principalAmount: input.offsetAmount,
            interestAmount: 0,
            penaltyAmount: 0,
            totalAmount: input.offsetAmount,
            repaymentType: RepaymentType.ReceiptOffset,
            status: RepaymentStatus.Pending,
            repaymentTime: 0,
            payer: msg.sender,
            dataHash: input.dataHash
        });

        repaymentExists[input.repaymentNo] = true;
        loanRepayments[input.sourceLoanNo].push(input.repaymentNo);
        repaymentCreatedAt[input.repaymentNo] = timestamp;
        repaymentCount++;

        emit ReceiptOffsetRecorded(
            input.repaymentNo,
            input.sourceLoanNo,
            input.offsetReceiptId,
            input.offsetAmount
        );

        emit RepaymentCreated(
            input.repaymentNo,
            input.sourceLoanNo,
            input.offsetAmount,
            0,
            input.offsetAmount,
            RepaymentType.ReceiptOffset,
            timestamp
        );

        return true;
    }

    // ==================== 应收账款抵债 ====================

    /**
     * @dev 记录应收账款抵债
     * @param input 应收账款抵债参数
     * @return success 是否成功
     */
    function recordReceivableOffset(ReceivableOffsetInput calldata input)
        external onlyJavaBackend returns (bool success)
    {
        require(bytes(input.repaymentNo).length > 0, "repaymentNo is empty");
        require(bytes(input.sourceLoanNo).length > 0, "sourceLoanNo is empty");
        require(bytes(input.offsetReceivableId).length > 0, "offsetReceivableId is empty");
        require(input.offsetAmount > 0, "Invalid offset amount");

        uint256 timestamp = block.timestamp;

        receivableOffsetRecords[input.repaymentNo] = ReceivableOffsetRecord({
            repaymentNo: input.repaymentNo,
            sourceLoanNo: input.sourceLoanNo,
            offsetReceivableId: input.offsetReceivableId,
            offsetAmount: input.offsetAmount,
            offsetTime: timestamp
        });

        receivableOffsetExists[input.repaymentNo] = true;

        // 同时创建还款记录
        repaymentRecords[input.repaymentNo] = RepaymentRecord({
            repaymentNo: input.repaymentNo,
            loanNo: input.sourceLoanNo,
            principalAmount: input.offsetAmount,
            interestAmount: 0,
            penaltyAmount: 0,
            totalAmount: input.offsetAmount,
            repaymentType: RepaymentType.ReceivableOffset,
            status: RepaymentStatus.Pending,
            repaymentTime: 0,
            payer: msg.sender,
            dataHash: input.dataHash
        });

        repaymentExists[input.repaymentNo] = true;
        loanRepayments[input.sourceLoanNo].push(input.repaymentNo);
        repaymentCreatedAt[input.repaymentNo] = timestamp;
        repaymentCount++;

        emit ReceivableOffsetRecorded(
            input.repaymentNo,
            input.sourceLoanNo,
            input.offsetReceivableId,
            input.offsetAmount
        );

        emit RepaymentCreated(
            input.repaymentNo,
            input.sourceLoanNo,
            input.offsetAmount,
            0,
            input.offsetAmount,
            RepaymentType.ReceivableOffset,
            timestamp
        );

        return true;
    }

    // ==================== 质押物处置 ====================

    /**
     * @dev 记录质押物处置
     * @param repaymentNo 还款编号
     * @param loanNo 贷款编号
     * @param disposalMethod 处置方式
     * @param disposalAmount 处置金额
     * @return success 是否成功
     */
    function recordCollateralDisposal(
        string calldata repaymentNo,
        string calldata loanNo,
        string calldata disposalMethod,
        uint256 disposalAmount
    ) external onlyJavaBackend returns (bool success)
    {
        require(bytes(repaymentNo).length > 0, "repaymentNo is empty");
        require(bytes(loanNo).length > 0, "loanNo is empty");
        require(disposalAmount > 0, "Invalid disposal amount");

        uint256 timestamp = block.timestamp;

        // 创建还款记录
        repaymentRecords[repaymentNo] = RepaymentRecord({
            repaymentNo: repaymentNo,
            loanNo: loanNo,
            principalAmount: 0,
            interestAmount: 0,
            penaltyAmount: disposalAmount,
            totalAmount: disposalAmount,
            repaymentType: RepaymentType.CollateralDisposal,
            status: RepaymentStatus.Pending,
            repaymentTime: 0,
            payer: msg.sender,
            dataHash: bytes32(0)
        });

        repaymentExists[repaymentNo] = true;
        loanRepayments[loanNo].push(repaymentNo);
        repaymentCreatedAt[repaymentNo] = timestamp;
        repaymentCount++;

        emit CollateralDisposalRecorded(repaymentNo, loanNo, disposalMethod, disposalAmount);

        emit RepaymentCreated(
            repaymentNo,
            loanNo,
            0,
            0,
            disposalAmount,
            RepaymentType.CollateralDisposal,
            timestamp
        );

        return true;
    }

    // ==================== 部分还款 ====================

    /**
     * @dev 记录部分还款
     * @param input 部分还款参数
     * @return success 是否成功
     */
    function recordPartialRepayment(CashRepaymentInput calldata input)
        external onlyJavaBackend returns (bool success)
    {
        // 允许重复创建（幂等性）
        if (repaymentExists[input.repaymentNo]) {
            return true;
        }

        require(bytes(input.repaymentNo).length > 0, "repaymentNo is empty");
        require(bytes(input.loanNo).length > 0, "loanNo is empty");
        require(input.principalAmount > 0 || input.interestAmount > 0, "Invalid amount");

        uint256 totalAmount = input.principalAmount + input.interestAmount + input.penaltyAmount;
        uint256 timestamp = block.timestamp;

        repaymentRecords[input.repaymentNo] = RepaymentRecord({
            repaymentNo: input.repaymentNo,
            loanNo: input.loanNo,
            principalAmount: input.principalAmount,
            interestAmount: input.interestAmount,
            penaltyAmount: input.penaltyAmount,
            totalAmount: totalAmount,
            repaymentType: RepaymentType.Partial,
            status: RepaymentStatus.Pending,
            repaymentTime: 0,
            payer: msg.sender,
            dataHash: input.dataHash
        });

        repaymentExists[input.repaymentNo] = true;
        loanRepayments[input.loanNo].push(input.repaymentNo);
        repaymentCreatedAt[input.repaymentNo] = timestamp;
        repaymentCount++;

        emit RepaymentCreated(
            input.repaymentNo,
            input.loanNo,
            input.principalAmount,
            input.interestAmount,
            totalAmount,
            RepaymentType.Partial,
            timestamp
        );

        return true;
    }

    // ==================== 还款确认/拒绝 ====================

    /**
     * @dev 确认还款
     * @param repaymentNo 还款编号
     * @param confirmedAmount 确认金额
     * @return success 是否成功
     */
    function confirmRepayment(string calldata repaymentNo, uint256 confirmedAmount)
        external onlyJavaBackend onlyValidRepayment(repaymentNo) onlyPending(repaymentNo)
        returns (bool success)
    {
        repaymentRecords[repaymentNo].status = RepaymentStatus.Confirmed;
        repaymentRecords[repaymentNo].repaymentTime = block.timestamp;

        emit RepaymentConfirmed(repaymentNo, confirmedAmount, block.timestamp);

        return true;
    }

    /**
     * @dev 拒绝还款
     * @param repaymentNo 还款编号
     * @param rejectReason 拒绝原因
     * @return success 是否成功
     */
    function rejectRepayment(string calldata repaymentNo, string calldata rejectReason)
        external onlyJavaBackend onlyValidRepayment(repaymentNo) onlyPending(repaymentNo)
        returns (bool success)
    {
        repaymentRecords[repaymentNo].status = RepaymentStatus.Rejected;

        emit RepaymentRejected(repaymentNo, rejectReason, block.timestamp);

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取还款记录
     * @param repaymentNo 还款编号
     * @return record 还款记录
     */
    function getRepayment(string calldata repaymentNo)
        external view onlyValidRepayment(repaymentNo) returns (RepaymentRecord memory record)
    {
        return repaymentRecords[repaymentNo];
    }

    /**
     * @dev 获取贷款的所有还款记录
     * @param loanNo 贷款编号
     * @return repaymentNos 还款编号列表
     */
    function getRepaymentsByLoan(string calldata loanNo)
        external view returns (string[] memory repaymentNos)
    {
        return loanRepayments[loanNo];
    }

    /**
     * @dev 获取还款记录数量
     * @param loanNo 贷款编号
     * @return count 还款记录数量
     */
    function getRepaymentCount(string calldata loanNo) external view returns (uint256 count) {
        return loanRepayments[loanNo].length;
    }

    /**
     * @dev 获取仓单抵债记录
     * @param repaymentNo 还款编号
     * @return record 仓单抵债记录
     */
    function getReceiptOffset(string calldata repaymentNo)
        external view returns (ReceiptOffsetRecord memory record)
    {
        require(receiptOffsetExists[repaymentNo], "Receipt offset not found");
        return receiptOffsetRecords[repaymentNo];
    }

    /**
     * @dev 获取应收账款抵债记录
     * @param repaymentNo 还款编号
     * @return record 应收账款抵债记录
     */
    function getReceivableOffset(string calldata repaymentNo)
        external view returns (ReceivableOffsetRecord memory record)
    {
        require(receivableOffsetExists[repaymentNo], "Receivable offset not found");
        return receivableOffsetRecords[repaymentNo];
    }

    /**
     * @dev 检查还款是否存在
     * @param repaymentNo 还款编号
     * @return exists 是否存在
     */
    function exists(string calldata repaymentNo) external view returns (bool) {
        return repaymentExists[repaymentNo];
    }

    /**
     * @dev 获取还款类型名称
     * @param repaymentType 还款类型
     * @return typeName 类型名称
     */
    function getRepaymentTypeName(RepaymentType repaymentType)
        external pure returns (string memory typeName)
    {
        if (repaymentType == RepaymentType.Cash) return "Cash";
        if (repaymentType == RepaymentType.ReceiptOffset) return "ReceiptOffset";
        if (repaymentType == RepaymentType.ReceivableOffset) return "ReceivableOffset";
        if (repaymentType == RepaymentType.CollateralDisposal) return "CollateralDisposal";
        if (repaymentType == RepaymentType.Partial) return "Partial";
        return "Unknown";
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }

    function setJavaBackend(address newBackend) external onlyAdmin returns (bool) {
        require(newBackend != address(0), "Invalid address");
        javaBackend = newBackend;
        return true;
    }
}
