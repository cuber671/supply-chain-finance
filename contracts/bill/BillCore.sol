// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

import "./IBillCore.sol";

/**
 * @title BillCore
 * @dev 票据核心合约
 *
 * 管理票据的全生命周期，包括开立、承兑、兑付、背书等核心操作
 *
 * 遵循数据上链规范：
 * - 明文上链：billId, amount, balance, dueDate, status, underlyingType, underlyingId
 * - 哈希化上链：payerHash, payeeHash, holderHash
 *
 * 栈溢出防护：
 * - 使用 Struct 封装参数
 * - 使用 calldata 传参
 * - 批量操作限制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract BillCore is IBillCore {

    // ==================== 常量定义 ====================

    uint256 constant MAX_BATCH_SIZE = 50;

    // ==================== 状态变量 ====================

    address public admin;
    address public javaBackend;
    uint256 public billCount;
    uint256 public constant VERSION = 1;

    // ==================== 数据结构 ====================

    /**
     * @dev 票据核心信息（明文上链字段）
     */
    struct BillCoreData {
        string billId;            // 票据ID（公开检索）
        uint256 amount;          // 票据金额
        uint256 balance;         // 剩余金额
        uint256 dueDate;        // 到期日
        BillStatus status;       // 状态
        UnderlyingType underlyingType; // 底层资产类型
        string underlyingId;    // 底层资产ID
        string parentBillId;    // 父票据ID（拆分追溯）
        string rootBillId;      // 原始票据ID（合并追溯）
    }

    /**
     * @dev 票据哈希数据（哈希化上链字段）
     */
    struct BillHashData {
        bytes32 payerHash;      // 开票人哈希
        bytes32 payeeHash;     // 收票人哈希
        bytes32 holderHash;    // 当前持有人哈希
        bytes32[] endorsementChain; // 背书链
    }

    /**
     * @dev 票据完整数据
     */
    struct Bill {
        BillCoreData core;
        BillHashData hashData;
    }

    /**
     * @dev 票据完整信息返回结构
     */
    struct BillInfo {
        string billId;
        uint256 amount;
        uint256 balance;
        uint256 dueDate;
        uint8 status;
        uint8 underlyingType;
        string underlyingId;
        string parentBillId;
        string rootBillId;
        bytes32 payerHash;
        bytes32 payeeHash;
        bytes32 holderHash;
    }

    // ==================== 存储层 ====================

    mapping(string => Bill) private bills;
    mapping(string => bool) private billIdExists;

    // ==================== 事件定义 ====================

    event BillIssued(
        string indexed billId,
        uint256 amount,
        uint256 dueDate,
        bytes32 payerHash,
        bytes32 payeeHash,
        UnderlyingType underlyingType,
        string underlyingId,
        address indexed issuer,
        uint256 timestamp
    );

    event BillAccepted(
        string indexed billId,
        address indexed acceptor,
        uint256 timestamp
    );

    event BillPaid(
        string indexed billId,
        uint256 amount,
        address indexed payer,
        uint256 timestamp
    );

    event BillCancelled(
        string indexed billId,
        address indexed canceller,
        string reason,
        uint256 timestamp
    );

    event BillEndorsed(
        string indexed billId,
        bytes32 indexed fromHash,
        bytes32 toHash,
        address indexed endorser,
        uint256 timestamp
    );

    event BillEndorsementConfirmed(
        string indexed billId,
        bytes32 indexed newHolderHash,
        address indexed confirmer,
        uint256 timestamp
    );

    event BillStatusChanged(
        string indexed billId,
        BillStatus oldStatus,
        BillStatus newStatus,
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

    modifier onlyValidBill(string calldata billId) {
        require(billIdExists[billId], "Bill not found");
        _;
    }

    modifier onlyActive(string calldata billId) {
        Bill storage b = bills[billId];
        require(
            b.core.status == BillStatus.Created ||
            b.core.status == BillStatus.Accepted,
            "Bill not active"
        );
        _;
    }

    modifier onlyPayer(string calldata billId) {
        require(
            bills[billId].hashData.payerHash == bytes32(uint256(uint160(msg.sender))),
            "Not payer"
        );
        _;
    }

    modifier onlyHolder(string calldata billId) {
        require(
            bills[billId].hashData.holderHash == bytes32(uint256(uint160(msg.sender))),
            "Not holder"
        );
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
     * @dev 开立票据
     */
    function issueBill(
        string calldata billId,
        uint256 amount,
        uint256 dueDate,
        bytes32 payerHash,
        bytes32 payeeHash,
        UnderlyingType underlyingType,
        string calldata underlyingId
    )
        external onlyJavaBackend returns (bool success)
    {
        require(!billIdExists[billId], "Bill ID exists");
        require(amount > 0, "Invalid amount");
        require(dueDate > block.timestamp, "Invalid due date");
        require(payerHash != bytes32(0), "Invalid payer hash");
        require(payeeHash != bytes32(0), "Invalid payee hash");
        require(underlyingType != UnderlyingType.None, "Invalid underlying type");

        // 创建票据
        bills[billId] = Bill({
            core: BillCoreData({
                billId: billId,
                amount: amount,
                balance: amount,
                dueDate: dueDate,
                status: BillStatus.Created,
                underlyingType: underlyingType,
                underlyingId: underlyingId,
                parentBillId: "",
                rootBillId: ""
            }),
            hashData: BillHashData({
                payerHash: payerHash,
                payeeHash: payeeHash,
                holderHash: payerHash,
                endorsementChain: new bytes32[](0)
            })
        });

        billIdExists[billId] = true;
        billCount++;

        emit BillIssued(
            billId,
            amount,
            dueDate,
            payerHash,
            payeeHash,
            underlyingType,
            underlyingId,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 承兑票据
     */
    function acceptBill(string calldata billId)
        external onlyValidBill(billId) onlyActive(billId) onlyPayer(billId) returns (bool success)
    {
        Bill storage b = bills[billId];
        require(b.core.status == BillStatus.Created, "Already accepted or invalid");

        BillStatus oldStatus = b.core.status;
        b.core.status = BillStatus.Accepted;

        emit BillStatusChanged(billId, oldStatus, BillStatus.Accepted, msg.sender, block.timestamp);
        emit BillAccepted(billId, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 兑付票据
     */
    function payBill(string calldata billId)
        external onlyValidBill(billId) onlyActive(billId) onlyPayer(billId) returns (bool success)
    {
        Bill storage b = bills[billId];

        uint256 payAmount = b.core.balance;

        BillStatus oldStatus = b.core.status;
        b.core.status = BillStatus.Paid;
        b.core.balance = 0;

        emit BillStatusChanged(billId, oldStatus, BillStatus.Paid, msg.sender, block.timestamp);
        emit BillPaid(billId, payAmount, msg.sender, block.timestamp);

        return true;
    }

    /**
     * @dev 撤销票据
     */
    function cancelBill(string calldata billId)
        external onlyValidBill(billId) onlyAdmin returns (bool success)
    {
        Bill storage b = bills[billId];

        // 已兑付或已撤销的票据不可撤销
        require(
            b.core.status != BillStatus.Paid &&
            b.core.status != BillStatus.Cancelled,
            "Cannot cancel"
        );

        BillStatus oldStatus = b.core.status;
        b.core.status = BillStatus.Cancelled;

        emit BillStatusChanged(billId, oldStatus, BillStatus.Cancelled, msg.sender, block.timestamp);
        emit BillCancelled(billId, msg.sender, "Cancelled by issuer", block.timestamp);

        return true;
    }

    /**
     * @dev 开票人撤销票据
     */
    function cancelBillByPayer(string calldata billId)
        external onlyValidBill(billId) onlyPayer(billId) returns (bool success)
    {
        Bill storage b = bills[billId];

        // 已兑付或已撤销的票据不可撤销
        require(
            b.core.status != BillStatus.Paid &&
            b.core.status != BillStatus.Cancelled,
            "Cannot cancel"
        );

        BillStatus oldStatus = b.core.status;
        b.core.status = BillStatus.Cancelled;

        emit BillStatusChanged(billId, oldStatus, BillStatus.Cancelled, msg.sender, block.timestamp);
        emit BillCancelled(billId, msg.sender, "Cancelled by payer", block.timestamp);

        return true;
    }

    /**
     * @dev 背书转让
     */
    function endorseBill(string calldata billId, bytes32 toHash)
        external onlyValidBill(billId) onlyActive(billId) onlyHolder(billId) returns (bool success)
    {
        require(toHash != bytes32(0), "Invalid target hash");

        Bill storage b = bills[billId];

        // 记录背书
        b.hashData.endorsementChain.push(b.hashData.holderHash);

        emit BillEndorsed(
            billId,
            b.hashData.holderHash,
            toHash,
            msg.sender,
            block.timestamp
        );

        return true;
    }

    /**
     * @dev 确认背书
     */
    function confirmEndorsement(string calldata billId)
        external onlyValidBill(billId) onlyActive(billId) returns (bool success)
    {
        Bill storage b = bills[billId];

        // 需要有pending的背书才能确认
        require(b.hashData.endorsementChain.length > 0, "No pending endorsement");

        // 获取最新的背书目标
        bytes32 newHolder = b.hashData.endorsementChain[b.hashData.endorsementChain.length - 1];

        // 验证权限：被背书人确认
        require(
            newHolder == bytes32(uint256(uint160(msg.sender))),
            "Not the endorsed party"
        );

        // 更新持有人
        b.hashData.holderHash = newHolder;

        emit BillEndorsementConfirmed(billId, newHolder, msg.sender, block.timestamp);

        return true;
    }

    // ==================== 拆分合并功能 ====================

    /**
     * @dev 拆分票据
     * @dev 注意：由于EVM栈深度限制，拆分功能暂时禁用
     * @dev 拆分功能可由应用层通过多次调用issueBill实现
     */
    function splitBill(
        string calldata billId,
        string calldata newBillId1,
        uint256 amount1,
        string calldata newBillId2,
        uint256 amount2
    )
        external onlyValidBill(billId) onlyActive(billId) onlyHolder(billId) returns (bool success)
    {
        // 拆分功能暂时禁用，由应用层处理
        revert("Split temporarily disabled");
    }

    function _createSplitBills(
        string memory parentBillId,
        string memory rootBillId,
        uint256 dueDate,
        UnderlyingType ut,
        string memory uid,
        bytes32 ph,
        bytes32 hh,
        string[] calldata newBillIds,
        uint256[] calldata amounts
    ) internal {
        for (uint256 i = 0; i < newBillIds.length; i++) {
            require(!billIdExists[newBillIds[i]], "New bill ID exists");

            // 计算rootBillId
            string memory currentRoot = bytes(rootBillId).length == 0 ? parentBillId : rootBillId;
            uint256 ts = block.timestamp;

            bills[newBillIds[i]] = Bill({
                core: BillCoreData({
                    billId: newBillIds[i],
                    amount: amounts[i],
                    balance: amounts[i],
                    dueDate: dueDate,
                    status: BillStatus.Created,
                    underlyingType: ut,
                    underlyingId: uid,
                    parentBillId: parentBillId,
                    rootBillId: currentRoot
                }),
                hashData: BillHashData({
                    payerHash: ph,
                    payeeHash: ph,
                    holderHash: hh,
                    endorsementChain: new bytes32[](0)
                })
            });

            billIdExists[newBillIds[i]] = true;
            billCount++;

            emit BillIssued(
                newBillIds[i],
                amounts[i],
                dueDate,
                ph,
                ph,
                ut,
                uid,
                msg.sender,
                ts
            );
        }
    }

    /**
     * @dev 合并票据
     * @dev 注意：由于EVM栈深度限制，合并功能暂时禁用
     */
    function mergeBills(
        string[] calldata billIds,
        string calldata newBillId,
        uint256 totalAmount
    )
        external onlyValidBill(billIds[0]) onlyHolder(billIds[0]) returns (bool success)
    {
        // 合并功能暂时禁用，由应用层处理
        revert("Merge temporarily disabled");
    }

    // ==================== 查询功能 ====================

    /**
     * @dev 获取票据完整信息
     */
    function getBill(string calldata billId)
        external view onlyValidBill(billId) returns (BillInfo memory info)
    {
        Bill storage b = bills[billId];
        info.billId = b.core.billId;
        info.amount = b.core.amount;
        info.balance = b.core.balance;
        info.dueDate = b.core.dueDate;
        info.status = uint8(b.core.status);
        info.underlyingType = uint8(b.core.underlyingType);
        info.underlyingId = b.core.underlyingId;
        info.parentBillId = b.core.parentBillId;
        info.rootBillId = b.core.rootBillId;
        info.payerHash = b.hashData.payerHash;
        info.payeeHash = b.hashData.payeeHash;
        info.holderHash = b.hashData.holderHash;
    }

    /**
     * @dev 获取票据状态
     */
    function getStatus(string calldata billId)
        external view onlyValidBill(billId) returns (BillStatus)
    {
        return bills[billId].core.status;
    }

    /**
     * @dev 检查票据是否存在
     */
    function exists(string calldata billId) external view returns (bool) {
        return billIdExists[billId];
    }

    /**
     * @dev 检查票据是否有效
     */
    function isValid(string calldata billId) external view returns (bool) {
        if (!billIdExists[billId]) return false;
        Bill storage b = bills[billId];
        return b.core.status == BillStatus.Created ||
               b.core.status == BillStatus.Accepted;
    }

    /**
     * @dev 获取持票人哈希
     * @param billId 票据ID
     * @return holderHash 持票人哈希
     */
    function getHolderHash(string calldata billId)
        external view onlyValidBill(billId) returns (bytes32 holderHash)
    {
        return bills[billId].hashData.holderHash;
    }

    /**
     * @dev 获取开票人哈希
     * @param billId 票据ID
     * @return payerHash 开票人哈希
     */
    function getPayerHash(string calldata billId)
        external view onlyValidBill(billId) returns (bytes32 payerHash)
    {
        return bills[billId].hashData.payerHash;
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
