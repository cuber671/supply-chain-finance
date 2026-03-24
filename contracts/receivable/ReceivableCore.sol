// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./IReceivableCore.sol";

/**
 * @title ReceivableCore
 * @dev 应收款核心合约
 *
 * 管理应收款的全生命周期，包括创建、确认、调整等核心操作
 *
 * 遵循数据上链规范：
 * - 明文上链：receivableId, initialAmount, balanceUnpaid, dueDate, status
 * - 哈希化上链：buyerSellerPairHash
 * - 全摘要存证：invoiceHash, contractHash, goodsDetailHash
 *
 * 栈溢出防护：
 * - 使用 Struct 封装参数
 * - 使用 calldata 传参
 * - 批量操作限制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract ReceivableCore is IReceivableCore {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;
    uint256 constant MIN_DUE_DAYS = 1;
    uint256 constant MAX_DUE_DAYS = 365 * 5; // 最长5年

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    address public receivableRepayment;  // 授权的还款合约地址
    uint256 public receivableCount;
    uint256 public constant VERSION = 2;

    // ==================== 数据结构 ====================

    /**
     * @dev 应收款状态枚举
     */
    enum ReceivableStatus {
        None,           // 不存在
        Created,        // 已创建
        Confirmed,      // 已确认
        Financing,      // 融资中
        Repaying,       // 还款中
        Settled,        // 已结清
        Overdue,        // 已逾期
        Defaulted       // 已违约
    }

    /**
     * @dev 应收款核心数据（扁平化结构）
     */
    struct Receivable {
        string receivableId;
        uint256 initialAmount;
        uint256 balanceUnpaid;
        uint256 dueDate;
        ReceivableStatus status;
        uint8 businessScene;
    }

    // 使用独立mapping存储额外字段
    mapping(string => bytes32) private receivableBuyerSellerPairHash;
    mapping(string => bytes32) private receivableCreditorHash;
    mapping(string => bytes32) private receivableDebtorHash;
    mapping(string => bytes32) private receivableInvoiceHash;
    mapping(string => bytes32) private receivableContractHash;
    mapping(string => bytes32) private receivableGoodsDetailHash;
    mapping(string => string) private receivableParentIds;
    mapping(string => bool) private receivableIsFinanced;
    mapping(string => uint256) private receivableCreatedAt;
    mapping(string => uint256) private receivableUpdatedAt;

    /**
     * @dev 应收款创建输入参数
     */
    struct ReceivableInput {
        string receivableId;
        uint256 initialAmount;
        uint256 dueDate;
        bytes32 buyerSellerPairHash;
        bytes32 invoiceHash;
        bytes32 contractHash;
        bytes32 goodsDetailHash;
        uint8 businessScene;  // 业务场景: 1-入库生成, 2-转让配送签收
    }

    /**
     * @dev 应收款完整信息返回结构
     */
    struct ReceivableInfo {
        string receivableId;
        uint256 initialAmount;
        uint256 balanceUnpaid;
        uint256 dueDate;
        uint8 status;
        uint8 businessScene;
        string parentId;
        bool isFinanced;
        bytes32 buyerSellerPairHash;
        bytes32 creditorHash;
        bytes32 debtorHash;
        bytes32 invoiceHash;
        bytes32 contractHash;
        bytes32 goodsDetailHash;
        uint256 createdAt;
        uint256 updatedAt;
    }

    /**
     * @dev 应收款调整输入参数
     */
    struct AdjustInput {
        string receivableId;
        uint256 newAmount;
        string reason;
    }

    // ==================== 存储层 ====================

    mapping(string => Receivable) private receivables;
    mapping(string => bool) private receivableIdExists;
    mapping(bytes32 => string[]) private buyerSellerPairToReceivableIds;

    // ==================== 事件定义 ====================

    event ReceivableCreated(
        string indexed receivableId,
        bytes32 indexed buyerSellerPairHash,
        uint256 initialAmount,
        uint256 dueDate,
        address indexed creator,
        uint256 timestamp
    );

    event ReceivableConfirmed(
        string indexed receivableId,
        address indexed confirmer,
        uint256 timestamp
    );

    event ReceivableAdjusted(
        string indexed receivableId,
        uint256 oldAmount,
        uint256 newAmount,
        string reason,
        address indexed adjuster,
        uint256 timestamp
    );

    event ReceivableFinanced(
        string indexed receivableId,
        address indexed financialInstitution,
        uint256 amount,
        uint256 timestamp
    );

    event ReceivableStatusChanged(
        string indexed receivableId,
        uint8 oldStatus,
        uint8 newStatus,
        address indexed operator,
        uint256 timestamp
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

    modifier onlyValidReceivable(string calldata receivableId) {
        require(receivableIdExists[receivableId], "Receivable not found");
        _;
    }

    modifier onlyActive(string calldata receivableId) {
        Receivable storage r = receivables[receivableId];
        require(
            r.status == ReceivableStatus.Created ||
            r.status == ReceivableStatus.Confirmed ||
            r.status == ReceivableStatus.Repaying,
            "Receivable not active"
        );
        _;
    }

    modifier onlyCreditor(string calldata receivableId) {
        require(
            receivableCreditorHash[receivableId] == bytes32(0) ||
            receivableCreditorHash[receivableId] == bytes32(uint256(uint160(msg.sender))),
            "Not creditor"
        );
        _;
    }

    modifier onlyDebtor(string calldata receivableId) {
        require(
            receivableDebtorHash[receivableId] == bytes32(0) ||
            receivableDebtorHash[receivableId] == bytes32(uint256(uint160(msg.sender))),
            "Not debtor"
        );
        _;
    }

    modifier onlyNotSettled(string calldata receivableId) {
        require(
            receivables[receivableId].status != ReceivableStatus.Settled,
            "Already settled"
        );
        _;
    }

    modifier onlyRepaymentContract() {
        require(
            msg.sender == receivableRepayment || msg.sender == admin,
            "Not authorized repayment contract"
        );
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
        javaBackend = _initialAdmin;
    }
    /**
     * @dev 创建应收款
     * @param input 应收款创建参数
     * @return success 是否成功
     */
    function createReceivable(ReceivableInput calldata input)
        external onlyAdmin returns (bool success)
    {
        // 允许重复创建（幂等性）
        if (receivableIdExists[input.receivableId]) {
            return true;
        }

        // 参数验证 - 放宽限制
        require(input.initialAmount > 0, "Invalid amount");
        require(input.buyerSellerPairHash != bytes32(0), "Invalid pair hash");

        // 使用扁平化结构创建应收款
        uint256 timestamp = block.timestamp;
        receivables[input.receivableId] = Receivable({
            receivableId: input.receivableId,
            initialAmount: input.initialAmount,
            balanceUnpaid: input.initialAmount,
            dueDate: input.dueDate,
            status: ReceivableStatus.Created,
            businessScene: input.businessScene
        });

        // 存储额外字段到独立mapping
        receivableBuyerSellerPairHash[input.receivableId] = input.buyerSellerPairHash;
        receivableCreditorHash[input.receivableId] = bytes32(0);
        receivableDebtorHash[input.receivableId] = bytes32(0);
        receivableInvoiceHash[input.receivableId] = input.invoiceHash;
        receivableContractHash[input.receivableId] = input.contractHash;
        receivableGoodsDetailHash[input.receivableId] = input.goodsDetailHash;
        receivableParentIds[input.receivableId] = "";
        receivableIsFinanced[input.receivableId] = false;
        receivableCreatedAt[input.receivableId] = timestamp;
        receivableUpdatedAt[input.receivableId] = timestamp;

        receivableIdExists[input.receivableId] = true;
        buyerSellerPairToReceivableIds[input.buyerSellerPairHash].push(input.receivableId);
        receivableCount++;

        emit ReceivableCreated(
            input.receivableId,
            input.buyerSellerPairHash,
            input.initialAmount,
            input.dueDate,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 确认应收款
     * @param receivableId 应收款ID
     * @return success 是否成功
     */
    function confirmReceivable(string calldata receivableId)
        external onlyValidReceivable(receivableId) returns (bool success)
    {
        Receivable storage r = receivables[receivableId];
        require(r.status == ReceivableStatus.Created, "Already confirmed or invalid");

        uint8 oldStatus = uint8(r.status);
        r.status = ReceivableStatus.Confirmed;
        receivableUpdatedAt[receivableId] = block.timestamp;

        emit ReceivableStatusChanged(
            receivableId,
            oldStatus,
            uint8(ReceivableStatus.Confirmed),
            msg.sender,
            block.timestamp
        );

        emit ReceivableConfirmed(receivableId, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 调整应收款金额
     * @param input 调整参数
     * @return success 是否成功
     */
    function adjustReceivable(AdjustInput calldata input)
        external onlyValidReceivable(input.receivableId) onlyActive(input.receivableId) onlyCreditor(input.receivableId) returns (bool success)
    {
        require(input.newAmount > 0, "Invalid new amount");

        Receivable storage r = receivables[input.receivableId];
        uint256 oldAmount = r.balanceUnpaid;

        // 调整金额（不能超过初始金额）
        require(input.newAmount <= r.initialAmount, "Exceeds initial amount");

        r.balanceUnpaid = input.newAmount;
        receivableUpdatedAt[input.receivableId] = block.timestamp;

        emit ReceivableAdjusted(
            input.receivableId,
            oldAmount,
            input.newAmount,
            input.reason,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 发起融资
     * @param receivableId 应收款ID
     * @param financialInstitution 金融机构地址
     * @param amount 融资金额
     * @return success 是否成功
     */
    function financeReceivable(
        string calldata receivableId,
        address financialInstitution,
        uint256 amount
    )
        external onlyValidReceivable(receivableId) onlyActive(receivableId) returns (bool success)
    {
        Receivable storage r = receivables[receivableId];
        require(r.status == ReceivableStatus.Confirmed, "Not confirmed");
        require(amount > 0 && amount <= r.balanceUnpaid, "Invalid amount");
        require(financialInstitution != address(0), "Invalid institution");

        uint8 oldStatus = uint8(r.status);
        r.status = ReceivableStatus.Financing;
        receivableUpdatedAt[receivableId] = block.timestamp;

        emit ReceivableStatusChanged(
            receivableId,
            oldStatus,
            uint8(ReceivableStatus.Financing),
            msg.sender,
            block.timestamp
        );

        emit ReceivableFinanced(
            receivableId,
            financialInstitution,
            amount,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 更新状态
     * @param receivableId 应收款ID
     * @param newStatus 新状态
     * @return success 是否成功
     */
    function updateStatus(string calldata receivableId, ReceivableStatus newStatus)
        external onlyValidReceivable(receivableId) returns (bool success)
    {
        Receivable storage r = receivables[receivableId];
        uint8 oldStatus = uint8(r.status);

        // 状态转换验证
        require(isValidStatusTransition(r.status, newStatus), "Invalid transition");

        r.status = newStatus;
        receivableUpdatedAt[receivableId] = block.timestamp;

        emit ReceivableStatusChanged(
            receivableId,
            oldStatus,
            uint8(newStatus),
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 检查并更新逾期状态
     * @param receivableId 应收款ID
     * @return isOverdue 是否逾期
     */
    function checkAndUpdateOverdue(string calldata receivableId)
        external onlyValidReceivable(receivableId) returns (bool isOverdue)
    {
        Receivable storage r = receivables[receivableId];

        if (r.status == ReceivableStatus.Confirmed ||
            r.status == ReceivableStatus.Financing ||
            r.status == ReceivableStatus.Repaying) {

            if (block.timestamp > r.dueDate) {
                r.status = ReceivableStatus.Overdue;
                receivableUpdatedAt[receivableId] = block.timestamp;

                emit ReceivableStatusChanged(
                    receivableId,
                    uint8(ReceivableStatus.Overdue),
                    uint8(ReceivableStatus.Overdue),
                    address(0),
                    block.timestamp
                );

                return true;
            }
        }

        return false;
    }

    // ==================== 状态转换验证 ====================

    function isValidStatusTransition(ReceivableStatus from, ReceivableStatus to)
        internal pure returns (bool)
    {
        // Created -> Confirmed
        if (from == ReceivableStatus.Created && to == ReceivableStatus.Confirmed) return true;

        // Confirmed -> Financing
        if (from == ReceivableStatus.Confirmed && to == ReceivableStatus.Financing) return true;

        // Financing -> Repaying
        if (from == ReceivableStatus.Financing && to == ReceivableStatus.Repaying) return true;

        // Repaying -> Settled
        if (from == ReceivableStatus.Repaying && to == ReceivableStatus.Settled) return true;

        // Financing -> Overdue (自动)
        // Confirmed -> Overdue (自动)

        return false;
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取应收款完整信息
     * @param receivableId 应收款ID
     * @return info 应收款完整信息
     */
    function getReceivable(string calldata receivableId)
        external view onlyValidReceivable(receivableId) returns (ReceivableInfo memory info)
    {
        Receivable storage r = receivables[receivableId];
        info.receivableId = r.receivableId;
        info.initialAmount = r.initialAmount;
        info.balanceUnpaid = r.balanceUnpaid;
        info.dueDate = r.dueDate;
        info.status = uint8(r.status);
        info.businessScene = r.businessScene;
        info.parentId = receivableParentIds[receivableId];
        info.isFinanced = receivableIsFinanced[receivableId];
        info.buyerSellerPairHash = receivableBuyerSellerPairHash[receivableId];
        info.creditorHash = receivableCreditorHash[receivableId];
        info.debtorHash = receivableDebtorHash[receivableId];
        info.invoiceHash = receivableInvoiceHash[receivableId];
        info.contractHash = receivableContractHash[receivableId];
        info.goodsDetailHash = receivableGoodsDetailHash[receivableId];
        info.createdAt = receivableCreatedAt[receivableId];
        info.updatedAt = receivableUpdatedAt[receivableId];
    }

    /**
     * @dev 获取应收款核心信息
     * @param receivableId 应收款ID
     * @return _receiptId 应收款ID
     */
    function getReceivableCore(string calldata receivableId)
        external view onlyValidReceivable(receivableId) returns (
            string memory _receiptId,
            uint256 initialAmount,
            uint256 balanceUnpaid,
            uint256 dueDate,
            uint8 status
        )
    {
        Receivable storage r = receivables[receivableId];
        return (
            r.receivableId,
            r.initialAmount,
            r.balanceUnpaid,
            r.dueDate,
            uint8(r.status)
        );
    }

    /**
     * @dev 获取买卖对关联的应收款
     * @param buyerSellerPairHash 买卖对哈希
     * @return 应收款ID列表
     */
    function getReceivablesByPair(bytes32 buyerSellerPairHash)
        external view returns (string[] memory)
    {
        return buyerSellerPairToReceivableIds[buyerSellerPairHash];
    }

    /**
     * @dev 检查应收款是否存在
     * @param receivableId 应收款ID
     * @return 是否存在
     */
    function exists(string calldata receivableId) external view returns (bool) {
        return receivableIdExists[receivableId];
    }

    /**
     * @dev 检查应收款是否有效
     * @param receivableId 应收款ID
     * @return 是否有效
     */
    function isValid(string calldata receivableId) external view returns (bool) {
        if (!receivableIdExists[receivableId]) return false;
        Receivable storage r = receivables[receivableId];
        return r.status == ReceivableStatus.Created ||
               r.status == ReceivableStatus.Confirmed ||
               r.status == ReceivableStatus.Repaying;
    }

    // ==================== 还款操作 ====================

    /**
     * @dev 更新应收款余额
     * @param receivableId 应收款ID
     * @param amount 还款金额
     * @param isFull 是否结清
     * @return success 是否成功
     */
    function updateBalance(string calldata receivableId, uint256 amount, bool isFull)
        external onlyValidReceivable(receivableId) onlyNotSettled(receivableId) onlyRepaymentContract returns (bool success)
    {
        require(amount > 0, "Invalid amount");

        Receivable storage r = receivables[receivableId];

        // 验证状态
        require(
            r.status == ReceivableStatus.Confirmed ||
            r.status == ReceivableStatus.Financing ||
            r.status == ReceivableStatus.Repaying,
            "Invalid status for repayment"
        );

        // 验证还款金额不超过未还余额
        require(amount <= r.balanceUnpaid, "Amount exceeds balance");

        // 更新余额
        r.balanceUnpaid -= amount;
        receivableUpdatedAt[receivableId] = block.timestamp;

        // 如果不是结清，更新状态为还款中
        if (!isFull) {
            if (r.status != ReceivableStatus.Repaying) {
                r.status = ReceivableStatus.Repaying;
            }
        }

        emit ReceivableStatusChanged(
            receivableId,
            uint8(r.status),
            isFull ? uint8(ReceivableStatus.Settled) : uint8(r.status),
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 结清应收款
     * @param receivableId 应收款ID
     * @return success 是否成功
     */
    function settleReceivable(string calldata receivableId)
        external onlyValidReceivable(receivableId) onlyRepaymentContract returns (bool success)
    {
        Receivable storage r = receivables[receivableId];

        // 验证状态
        require(
            r.status == ReceivableStatus.Repaying ||
            r.status == ReceivableStatus.Confirmed ||
            r.status == ReceivableStatus.Financing,
            "Invalid status for settlement"
        );

        // 验证已还清
        require(r.balanceUnpaid == 0, "Balance not zero");

        uint8 oldStatus = uint8(r.status);
        r.status = ReceivableStatus.Settled;
        receivableUpdatedAt[receivableId] = block.timestamp;

        emit ReceivableStatusChanged(
            receivableId,
            oldStatus,
            uint8(ReceivableStatus.Settled),
            msg.sender,
            block.timestamp
        );

        emit ReceivableSettled(receivableId, r.initialAmount, block.timestamp);

        return true;
    }

    /**
     * @dev 获取应收款余额
     * @param receivableId 应收款ID
     * @return balanceUnpaid 剩余未还金额
     */
    function getBalanceUnpaid(string calldata receivableId)
        external view onlyValidReceivable(receivableId) returns (uint256 balanceUnpaid)
    {
        return receivables[receivableId].balanceUnpaid;
    }

    /**
     * @dev 获取应收款状态
     * @param receivableId 应收款ID
     * @return status 应收款状态
     */
    function getReceivableStatus(string calldata receivableId)
        external view onlyValidReceivable(receivableId) returns (uint8 status)
    {
        return uint8(receivables[receivableId].status);
    }

    /**
     * @dev 获取债务人哈希
     * @param receivableId 应收款ID
     * @return debtorHash 债务人哈希
     */
    function getDebtorHash(string calldata receivableId)
        external view onlyValidReceivable(receivableId) returns (bytes32 debtorHash)
    {
        return receivableDebtorHash[receivableId];
    }

    // ==================== 事件定义 ====================

    event ReceivableSettled(
        string indexed receivableId,
        uint256 totalAmount,
        uint256 timestamp
    );

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

    function setReceivableRepayment(address newRepayment) external onlyAdmin returns (bool) {
        require(newRepayment != address(0), "Invalid address");
        receivableRepayment = newRepayment;
        return true;
    }
}
