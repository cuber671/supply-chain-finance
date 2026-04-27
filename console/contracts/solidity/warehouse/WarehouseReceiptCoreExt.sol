// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title WarehouseReceiptCoreExt
 * @dev 仓单核心合约扩展 - 增加贷款关联
 *
 * 在原有WarehouseReceiptCore基础上，扩展loanId字段关联
 * 用于建立仓单与质押贷款之间的关联关系
 *
 * 设计原则：
 * - 保留原有所有方法和接口
 * - 仅新增字段和可选的扩展方法
 * - 原有合约地址保持不变（向后兼容）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract WarehouseReceiptCoreExt {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    uint256 public constant VERSION = 1;

    // ==================== 数据结构 ====================

    /**
     * @dev 质押信息结构
     */
    struct PledgeInfo {
        string loanId;              // 贷款编号
        uint256 pledgeAmount;       // 质押金额
        uint256 pledgeTime;         // 质押时间
        address pledger;            // 质押操作人
        bool active;               // 是否活跃
    }

    // ==================== 存储层 ====================

    /**
     * @dev 仓单ID -> 贷款ID 映射
     */
    mapping(string => string) public receiptLoanIds;

    /**
     * @dev 贷款ID -> 质押信息 映射
     */
    mapping(string => PledgeInfo) public loanPledgeInfos;

    /**
     * @dev 仓单是否存在（外部检查）
     */
    mapping(string => bool) public receiptExists;

    // ==================== 事件定义 ====================

    event ReceiptPledgedWithLoan(
        string indexed receiptId,
        string indexed loanId,
        uint256 pledgeAmount,
        address indexed pledger,
        uint256 pledgeTime
    );

    event ReceiptUnpledgedWithLoan(
        string indexed receiptId,
        string indexed loanId,
        uint256 pledgeAmount,
        address indexed unlocker,
        uint256 unlockTime
    );

    event LoanIdUpdated(
        string indexed receiptId,
        string oldLoanId,
        string newLoanId,
        uint256 updateTime
    );

    event PledgeInfoUpdated(
        string indexed loanId,
        uint256 oldAmount,
        uint256 newAmount,
        uint256 updateTime
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

    modifier onlyValidReceipt(string memory receiptId) {
        require(receiptExists[receiptId] || bytes(receiptLoanIds[receiptId]).length > 0, "Receipt not found");
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
     * @dev 设置仓单-贷款关联
     * @param receiptId 仓单ID
     * @param loanId 贷款编号
     * @param pledgeAmount 质押金额
     * @return success 是否成功
     */
    function setReceiptLoanId(
        string calldata receiptId,
        string calldata loanId,
        uint256 pledgeAmount
    ) external onlyJavaBackend returns (bool success)
    {
        require(bytes(receiptId).length > 0, "receiptId is empty");
        require(bytes(loanId).length > 0, "loanId is empty");
        require(bytes(receiptLoanIds[receiptId]).length == 0, "loan already set for this receipt");

        uint256 timestamp = block.timestamp;

        // 设置仓单-贷款关联
        receiptLoanIds[receiptId] = loanId;

        // 记录质押信息
        loanPledgeInfos[loanId] = PledgeInfo({
            loanId: loanId,
            pledgeAmount: pledgeAmount,
            pledgeTime: timestamp,
            pledger: msg.sender,
            active: true
        });

        receiptExists[receiptId] = true;

        emit ReceiptPledgedWithLoan(receiptId, loanId, pledgeAmount, msg.sender, timestamp);

        return true;
    }

    /**
     * @dev 更新仓单-贷款关联
     * @param receiptId 仓单ID
     * @param newLoanId 新贷款编号
     * @return success 是否成功
     */
    function updateReceiptLoanId(
        string calldata receiptId,
        string calldata newLoanId
    ) external onlyJavaBackend onlyValidReceipt(receiptId) returns (bool success)
    {
        string memory oldLoanId = receiptLoanIds[receiptId];

        // 更新原贷款为非活跃
        if (bytes(oldLoanId).length > 0) {
            loanPledgeInfos[oldLoanId].active = false;
        }

        // 设置新贷款关联
        if (bytes(newLoanId).length > 0) {
            receiptLoanIds[receiptId] = newLoanId;
            loanPledgeInfos[newLoanId].active = true;
        } else {
            delete receiptLoanIds[receiptId];
        }

        emit LoanIdUpdated(receiptId, oldLoanId, newLoanId, block.timestamp);

        return true;
    }

    /**
     * @dev 更新质押信息
     * @param loanId 贷款编号
     * @param newAmount 新质押金额
     * @return success 是否成功
     */
    function updatePledgeAmount(
        string calldata loanId,
        uint256 newAmount
    ) external onlyJavaBackend returns (bool success)
    {
        require(bytes(loanId).length > 0, "loanId is empty");
        require(loanPledgeInfos[loanId].pledgeTime > 0, "Pledge info not found");

        uint256 oldAmount = loanPledgeInfos[loanId].pledgeAmount;
        loanPledgeInfos[loanId].pledgeAmount = newAmount;

        emit PledgeInfoUpdated(loanId, oldAmount, newAmount, block.timestamp);

        return true;
    }

    /**
     * @dev 解除仓单-贷款关联（结清时调用）
     * @param receiptId 仓单ID
     * @return success 是否成功
     */
    function releaseReceiptLoanId(string calldata receiptId)
        external onlyJavaBackend onlyValidReceipt(receiptId) returns (bool success)
    {
        string memory loanId = receiptLoanIds[receiptId];

        if (bytes(loanId).length > 0) {
            uint256 pledgeAmount = loanPledgeInfos[loanId].pledgeAmount;
            loanPledgeInfos[loanId].active = false;

            emit ReceiptUnpledgedWithLoan(receiptId, loanId, pledgeAmount, msg.sender, block.timestamp);
        }

        delete receiptLoanIds[receiptId];

        return true;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取仓单关联的贷款ID
     * @param receiptId 仓单ID
     * @return loanId 贷款编号
     */
    function getReceiptLoanId(string calldata receiptId) external view returns (string memory loanId) {
        return receiptLoanIds[receiptId];
    }

    /**
     * @dev 获取质押信息
     * @param loanId 贷款编号
     * @return info 质押信息
     */
    function getPledgeInfo(string calldata loanId) external view returns (PledgeInfo memory info) {
        return loanPledgeInfos[loanId];
    }

    /**
     * @dev 检查仓单是否有活跃贷款
     * @param receiptId 仓单ID
     * @return hasActiveLoan 是否有活跃贷款
     */
    function hasActiveLoan(string calldata receiptId) external view returns (bool) {
        string memory loanId = receiptLoanIds[receiptId];
        if (bytes(loanId).length == 0) return false;
        return loanPledgeInfos[loanId].active;
    }

    /**
     * @dev 验证贷款是否与仓单匹配
     * @param receiptId 仓单ID
     * @param loanId 贷款编号
     * @return isMatched 是否匹配
     */
    function isLoanMatchedWithReceipt(
        string calldata receiptId,
        string calldata loanId
    ) external view returns (bool isMatched) {
        return keccak256(bytes(receiptLoanIds[receiptId])) == keccak256(bytes(loanId));
    }

    /**
     * @dev 检查仓单是否已质押
     * @param receiptId 仓单ID
     * @return isPledged 是否已质押
     */
    function isReceiptPledged(string calldata receiptId) external view returns (bool isPledged) {
        string memory loanId = receiptLoanIds[receiptId];
        if (bytes(loanId).length == 0) return false;
        return loanPledgeInfos[loanId].active;
    }

    /**
     * @dev 获取质押状态
     * @param receiptId 仓单ID
     * @return status 质押状态: 0=未质押, 1=质押中, 2=已释放
     */
    function getPledgeStatus(string calldata receiptId) external view returns (uint8 status) {
        string memory loanId = receiptLoanIds[receiptId];
        if (bytes(loanId).length == 0) return 0; // 未质押
        if (loanPledgeInfos[loanId].active) return 1; // 质押中
        return 2; // 已释放
    }

    /**
     * @dev 获取质押金额
     * @param loanId 贷款编号
     * @return pledgeAmount 质押金额
     */
    function getPledgeAmount(string calldata loanId) external view returns (uint256 pledgeAmount) {
        return loanPledgeInfos[loanId].pledgeAmount;
    }

    /**
     * @dev 检查贷款是否活跃
     * @param loanId 贷款编号
     * @return isActive 是否活跃
     */
    function isLoanActive(string calldata loanId) external view returns (bool isActive) {
        return loanPledgeInfos[loanId].active;
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
