// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LoanCore
 * @dev 质押贷款核心合约
 *
 * 管理质押贷款全生命周期，存储链上关键状态
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract LoanCore {

    // ==================== 常量定义 ====================

    uint256 constant MIN_LOAN_DAYS = 1;
    uint256 constant MAX_LOAN_DAYS = 365 * 2;
    uint256 constant GRACE_PERIOD_DAYS = 7;

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    address public loanRepayment;
    uint256 public loanCount;
    uint256 public constant VERSION = 1;

    // ==================== 贷款状态枚举 ====================

    enum LoanStatus {
        None, Pending, Approved, Disbursed,
        Repaying, Settled, Overdue, Defaulted,
        Cancelled, Disposed
    }

    // ==================== 存储层 ====================

    // 贷款基本信息
    mapping(string => bytes32) public loanBorrowerHash;
    mapping(string => bytes32) public loanFinanceEntHash;
    mapping(string => string) public loanReceiptId;
    mapping(string => uint256) public loanAmount;
    mapping(string => uint256) public loanInterestRate;
    mapping(string => uint256) public loanDays;
    mapping(string => uint256) public loanStartDate;
    mapping(string => uint256) public loanEndDate;
    mapping(string => uint256) public loanDisbursedAmount;
    mapping(string => uint256) public loanRepaidPrincipal;
    mapping(string => uint256) public loanRepaidInterest;
    mapping(string => uint256) public loanRepaidPenalty;
    mapping(string => uint8) public loanStatuses;
    mapping(string => bytes32) public loanDataHash;
    mapping(string => uint256) public loanCreateTime;
    mapping(string => uint256) public loanUpdateTime;
    mapping(string => bool) public loanExists;
    mapping(string => string) public receiptToLoan;

    // 索引映射
    mapping(bytes32 => string[]) public borrowerLoans;
    mapping(bytes32 => string[]) public financeEntLoans;

    // ==================== 事件定义 ====================

    event LoanCreated(
        string indexed loanNo,
        bytes32 indexed borrowerHash,
        bytes32 financeEntHash,
        string receiptId,
        uint256 loanAmount,
        uint256 interestRate,
        uint256 loanDays,
        uint256 timestamp
    );

    event LoanApproved(
        string indexed loanNo,
        uint256 approvedAmount,
        uint256 interestRate,
        uint256 loanDays,
        uint256 approveTime
    );

    event LoanDisbursed(
        string indexed loanNo,
        uint256 disbursedAmount,
        uint256 loanStartDate,
        uint256 loanEndDate,
        string receiptId,
        uint256 disbursedTime
    );

    event LoanRepayment(
        string indexed loanNo,
        uint256 principalAmount,
        uint256 interestAmount,
        uint256 penaltyAmount,
        uint256 totalAmount,
        string repaymentType,
        uint256 repaymentTime
    );

    event LoanSettled(
        string indexed loanNo,
        uint256 totalPrincipal,
        uint256 totalInterest,
        uint256 totalPenalty,
        uint256 settleTime
    );

    event LoanOverdue(
        string indexed loanNo,
        uint256 overdueDays,
        uint256 penaltyRate,
        uint256 penaltyAmount,
        uint256 overdueTime
    );

    event LoanDefaulted(
        string indexed loanNo,
        string disposalMethod,
        uint256 disposalAmount,
        uint256 defaultedTime
    );

    event LoanCancelled(
        string indexed loanNo,
        string cancelReason,
        uint256 cancelTime
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

    modifier onlyValidLoan(string memory loanNo) {
        require(loanExists[loanNo], "Loan not found");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
        javaBackend = _initialAdmin;
    }

    // ==================== 核心功能 ====================

    /**
     * @dev 创建贷款记录
     */
    function createLoan(
        string calldata loanNo,
        bytes32 borrowerHash,
        bytes32 financeEntHash,
        string calldata receiptId,
        uint256 _loanAmount,
        uint256 interestRate,
        uint256 _loanDays,
        bytes32 dataHash
    ) external onlyJavaBackend returns (bool success)
    {
        if (loanExists[loanNo]) {
            return true;
        }

        require(bytes(loanNo).length > 0, "loanNo is empty");
        require(bytes(receiptId).length > 0, "receiptId is empty");
        require(_loanAmount > 0, "Invalid loan amount");
        require(_loanDays >= MIN_LOAN_DAYS && _loanDays <= MAX_LOAN_DAYS, "Invalid loan days");
        require(borrowerHash != bytes32(0), "Invalid borrower hash");
        require(financeEntHash != bytes32(0), "Invalid finance entity hash");

        uint256 ts = block.timestamp;

        // 存储数据
        loanBorrowerHash[loanNo] = borrowerHash;
        loanFinanceEntHash[loanNo] = financeEntHash;
        loanReceiptId[loanNo] = receiptId;
        loanAmount[loanNo] = _loanAmount;
        loanInterestRate[loanNo] = interestRate;
        loanDays[loanNo] = _loanDays;
        loanStatuses[loanNo] = uint8(LoanStatus.Pending);
        loanDataHash[loanNo] = dataHash;
        loanCreateTime[loanNo] = ts;
        loanUpdateTime[loanNo] = ts;
        loanExists[loanNo] = true;

        borrowerLoans[borrowerHash].push(loanNo);
        financeEntLoans[financeEntHash].push(loanNo);
        loanCount++;

        emit LoanCreated(loanNo, borrowerHash, financeEntHash, receiptId, _loanAmount, interestRate, _loanDays, ts);

        return true;
    }

    /**
     * @dev 审批通过
     */
    function approveLoan(
        string calldata loanNo,
        uint256 approvedAmount,
        uint256 interestRate,
        uint256 _loanDays,
        bytes32 dataHash
    ) external onlyJavaBackend onlyValidLoan(loanNo) returns (bool success)
    {
        require(LoanStatus(loanStatuses[loanNo]) == LoanStatus.Pending, "invalid status for approve");

        loanAmount[loanNo] = approvedAmount;
        loanInterestRate[loanNo] = interestRate;
        loanDays[loanNo] = _loanDays;
        loanDataHash[loanNo] = dataHash;
        loanStatuses[loanNo] = uint8(LoanStatus.Approved);
        loanUpdateTime[loanNo] = block.timestamp;

        emit LoanApproved(loanNo, approvedAmount, interestRate, _loanDays, block.timestamp);

        return true;
    }

    /**
     * @dev 取消/拒绝贷款
     */
    function cancelLoan(string calldata loanNo, string calldata reason)
        external onlyJavaBackend onlyValidLoan(loanNo) returns (bool success)
    {
        uint8 status = loanStatuses[loanNo];
        require(status == uint8(LoanStatus.Pending) || status == uint8(LoanStatus.Approved), "invalid status for cancel");

        loanStatuses[loanNo] = uint8(LoanStatus.Cancelled);
        loanUpdateTime[loanNo] = block.timestamp;

        emit LoanCancelled(loanNo, reason, block.timestamp);

        return true;
    }

    /**
     * @dev 放款
     */
    function disburseLoan(
        string calldata loanNo,
        uint256 disbursedAmount,
        uint256 startDate,
        uint256 endDate,
        string calldata receiptId
    ) external onlyJavaBackend onlyValidLoan(loanNo) returns (bool success)
    {
        require(LoanStatus(loanStatuses[loanNo]) == LoanStatus.Approved, "invalid status for disburse");

        loanDisbursedAmount[loanNo] = disbursedAmount;
        loanStartDate[loanNo] = startDate;
        loanEndDate[loanNo] = endDate;
        loanStatuses[loanNo] = uint8(LoanStatus.Disbursed);
        loanUpdateTime[loanNo] = block.timestamp;

        receiptToLoan[receiptId] = loanNo;

        emit LoanDisbursed(loanNo, disbursedAmount, startDate, endDate, receiptId, block.timestamp);

        return true;
    }

    /**
     * @dev 记录还款
     */
    function recordRepayment(
        string calldata loanNo,
        uint256 principal,
        uint256 interest,
        uint256 penalty,
        string calldata repaymentType
    ) external onlyJavaBackend onlyValidLoan(loanNo) returns (bool success)
    {
        uint8 status = loanStatuses[loanNo];
        require(
            status == uint8(LoanStatus.Disbursed) ||
            status == uint8(LoanStatus.Repaying) ||
            status == uint8(LoanStatus.Overdue),
            "invalid status for repayment"
        );

        loanRepaidPrincipal[loanNo] += principal;
        loanRepaidInterest[loanNo] += interest;
        loanRepaidPenalty[loanNo] += penalty;
        loanUpdateTime[loanNo] = block.timestamp;

        uint256 repaidP = loanRepaidPrincipal[loanNo];
        uint256 repaidI = loanRepaidInterest[loanNo];
        uint256 repaidPen = loanRepaidPenalty[loanNo];

        if (repaidP + repaidI + repaidPen >= calculateTotalOwed(loanNo)) {
            loanStatuses[loanNo] = uint8(LoanStatus.Settled);
            emit LoanSettled(loanNo, repaidP, repaidI, repaidPen, block.timestamp);
        } else if (status != uint8(LoanStatus.Overdue)) {
            loanStatuses[loanNo] = uint8(LoanStatus.Repaying);
        }

        uint256 totalAmt = principal + interest + penalty;
        emit LoanRepayment(loanNo, principal, interest, penalty, totalAmt, repaymentType, block.timestamp);

        return true;
    }

    /**
     * @dev 标记逾期
     */
    function markOverdue(string calldata loanNo, uint256 overdueDays, uint256 penaltyRate, uint256 penaltyAmount)
        external onlyJavaBackend onlyValidLoan(loanNo) returns (bool success)
    {
        uint8 status = loanStatuses[loanNo];
        require(status == uint8(LoanStatus.Disbursed) || status == uint8(LoanStatus.Repaying), "invalid status for overdue");

        loanStatuses[loanNo] = uint8(LoanStatus.Overdue);
        loanUpdateTime[loanNo] = block.timestamp;

        emit LoanOverdue(loanNo, overdueDays, penaltyRate, penaltyAmount, block.timestamp);

        return true;
    }

    /**
     * @dev 违约处置
     */
    function markDefaulted(string calldata loanNo, string calldata disposalMethod, uint256 disposalAmount)
        external onlyJavaBackend onlyValidLoan(loanNo) returns (bool success)
    {
        require(LoanStatus(loanStatuses[loanNo]) == LoanStatus.Overdue, "invalid status for default");

        loanStatuses[loanNo] = uint8(LoanStatus.Defaulted);
        loanUpdateTime[loanNo] = block.timestamp;

        emit LoanDefaulted(loanNo, disposalMethod, disposalAmount, block.timestamp);

        return true;
    }

    // ==================== 计算功能 ====================

    function calculateTotalOwed(string calldata loanNo)
        public view onlyValidLoan(loanNo) returns (uint256 totalOwed)
    {
        uint8 status = loanStatuses[loanNo];
        if (status == uint8(LoanStatus.Settled) || status == uint8(LoanStatus.Cancelled)) {
            return 0;
        }

        uint256 principal = loanDisbursedAmount[loanNo] > 0 ? loanDisbursedAmount[loanNo] : loanAmount[loanNo];
        uint256 owed = principal + calculateInterest(loanNo);
        owed = owed - loanRepaidPrincipal[loanNo] - loanRepaidInterest[loanNo];

        return owed > 0 ? owed : 0;
    }

    function calculateInterest(string calldata loanNo)
        public view onlyValidLoan(loanNo) returns (uint256 interest)
    {
        uint8 status = loanStatuses[loanNo];
        if (status == uint8(LoanStatus.Settled) || status == uint8(LoanStatus.Cancelled)) {
            return 0;
        }

        if (loanStartDate[loanNo] == 0) {
            return 0;
        }

        uint256 principal = loanDisbursedAmount[loanNo] > 0 ? loanDisbursedAmount[loanNo] : loanAmount[loanNo];
        uint256 daysElapsed = (block.timestamp - loanStartDate[loanNo]) / (24 * 60 * 60);
        if (daysElapsed > loanDays[loanNo]) {
            daysElapsed = loanDays[loanNo];
        }

        interest = principal * loanInterestRate[loanNo] * daysElapsed / 365 / 10000;

        return interest;
    }

    function calculatePenalty(string calldata loanNo, uint256 overdueDays)
        public view onlyValidLoan(loanNo) returns (uint256 penalty)
    {
        if (overdueDays <= GRACE_PERIOD_DAYS) {
            return 0;
        }

        uint256 actualOverdueDays = overdueDays - GRACE_PERIOD_DAYS;
        uint256 principal = loanDisbursedAmount[loanNo] > 0 ? loanDisbursedAmount[loanNo] : loanAmount[loanNo];
        uint256 remainingPrincipal = principal - loanRepaidPrincipal[loanNo];
        uint256 dailyPenaltyRate = loanInterestRate[loanNo] * 15 / 365;

        penalty = remainingPrincipal * dailyPenaltyRate * actualOverdueDays / 10000;

        return penalty;
    }

    // ==================== 查询功能 ====================

    function getLoanStatus(string calldata loanNo) external view onlyValidLoan(loanNo) returns (uint8) {
        return loanStatuses[loanNo];
    }

    function getLoanByReceipt(string calldata receiptId) external view returns (string memory) {
        return receiptToLoan[receiptId];
    }

    function exists(string calldata loanNo) external view returns (bool) {
        return loanExists[loanNo];
    }

    function getOutstandingPrincipal(string calldata loanNo)
        external view onlyValidLoan(loanNo) returns (uint256)
    {
        uint256 principal = loanDisbursedAmount[loanNo] > 0 ? loanDisbursedAmount[loanNo] : loanAmount[loanNo];
        return principal > loanRepaidPrincipal[loanNo] ? principal - loanRepaidPrincipal[loanNo] : 0;
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

    function setLoanRepayment(address newRepayment) external onlyAdmin returns (bool) {
        require(newRepayment != address(0), "Invalid address");
        loanRepayment = newRepayment;
        return true;
    }
}
