// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LogisticsCore
 * @dev 物流核心合约（极简版）
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
contract LogisticsCore {

    // ==================== 状态变量 ====================

    address public admin;
    uint256 public delegateCount;
    mapping(string => bool) private voucherNoExists;
    mapping(string => uint8) private voucherStatus; // 0=不存在, 1=Pending, 2=Assigned, 3=InTransit, 4=Delivered, 5=Invalid

    // ==================== 事件定义 ====================

    event LogisticsDelegateCreated(string indexed voucherNo, address indexed operator, uint256 timestamp);
    event CarrierAssigned(string indexed voucherNo, bytes32 indexed carrierHash, address indexed operator, uint256 timestamp);
    event StatusUpdated(string indexed voucherNo, uint8 oldStatus, uint8 newStatus, address indexed operator, uint256 timestamp);

    // ==================== 修饰器 ====================

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    // ==================== 构造函数 ====================

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
    }

    // ==================== 核心功能 ====================

    /**
     * @dev 创建物流委派单
     * @param voucherNo 委派单编号
     * @param businessScene 业务场景 (1=直接移库, 2=转让后移库, 3=发往指定仓库)
     * @param receiptId 关联仓单ID
     * @param transportQuantity 运输数量
     * @param unit 计量单位
     * @param ownerHash 货主哈希
     * @param carrierHash 承运方哈希
     * @param sourceWhHash 起运地仓库哈希
     * @param targetWhHash 目的地仓库哈希
     * @param validUntil 有效期
     * @return success 是否成功
     */
    function createLogisticsDelegate(
        string calldata voucherNo,
        uint256 businessScene,
        string calldata receiptId,
        uint256 transportQuantity,
        string calldata unit,
        bytes32 ownerHash,
        bytes32 carrierHash,
        bytes32 sourceWhHash,
        bytes32 targetWhHash,
        uint256 validUntil
    ) external onlyAdmin returns (bool success) {
        require(!voucherNoExists[voucherNo], "Voucher exists");
        voucherNoExists[voucherNo] = true;
        voucherStatus[voucherNo] = 1; // Pending
        delegateCount++;
        emit LogisticsDelegateCreated(voucherNo, msg.sender, block.timestamp);
        return true;
    }

    function assignCarrier(string calldata voucherNo, bytes32 carrierHash) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 1, "Invalid status");
        voucherStatus[voucherNo] = 2; // Assigned
        emit CarrierAssigned(voucherNo, carrierHash, msg.sender, block.timestamp);
        return true;
    }

    // 提货确认 (状态: 2->3)
    function pickup(string calldata voucherNo, uint256 quantity) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 2, "Invalid status for pickup");
        voucherStatus[voucherNo] = 3; // InTransit
        emit StatusUpdated(voucherNo, 2, 3, msg.sender, block.timestamp);
        return true;
    }

    // 到达确认 (状态: 3->4)
    function arrive(string calldata voucherNo) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 3, "Invalid status for arrive");
        voucherStatus[voucherNo] = 4; // Delivered
        emit StatusUpdated(voucherNo, 3, 4, msg.sender, block.timestamp);
        return true;
    }

    /**
     * @dev 到货时增量入库
     * @param voucherNo 委派单编号
     * @param targetReceiptId 目标仓单ID
     * @param quantity 增量数量
     * @return success 是否成功
     */
    function arriveAndAddQuantity(
        string calldata voucherNo,
        string calldata targetReceiptId,
        uint256 quantity
    ) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 3, "Invalid status for arrive");
        emit StatusUpdated(voucherNo, 3, 4, msg.sender, block.timestamp);
        return true;
    }

    /**
     * @dev 到货时生成新仓单
     * @param voucherNo 委派单编号
     * @param newReceiptId 新仓单ID
     * @param weight 新仓单重量
     * @param unit 计量单位
     * @param ownerHash 货主哈希
     * @param warehouseHash 仓库哈希
     * @return success 是否成功
     */
    function arriveAndCreateReceipt(
        string calldata voucherNo,
        string calldata newReceiptId,
        uint256 weight,
        string calldata unit,
        bytes32 ownerHash,
        bytes32 warehouseHash
    ) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        emit StatusUpdated(voucherNo, 3, 4, msg.sender, block.timestamp);
        return true;
    }

    /**
     * @dev 确认交付
     * @param voucherNo 委派单编号
     * @param action 到货处理动作 (1=创建新仓单, 2=并入已有仓单)
     * @param targetReceiptId 目标仓单ID（增量入库时使用）
     * @return success 是否成功
     */
    function confirmDelivery(
        string calldata voucherNo,
        uint256 action,
        string calldata targetReceiptId
    ) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 4, "Invalid status for confirm");
        voucherStatus[voucherNo] = 5; // Completed
        emit StatusUpdated(voucherNo, 4, 5, msg.sender, block.timestamp);
        return true;
    }

    // 失效物流单
    function invalidate(string calldata voucherNo) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] != 5 && voucherStatus[voucherNo] != 0, "Cannot invalidate");
        voucherStatus[voucherNo] = 6; // Invalid
        emit StatusUpdated(voucherNo, voucherStatus[voucherNo], 6, msg.sender, block.timestamp);
        return true;
    }

    // 获取货主哈希（用于权限校验）
    function getOwnerHash(string calldata voucherNo) external view returns (bytes32) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        return bytes32(uint256(uint160(msg.sender)));
    }

    function getStatus(string calldata voucherNo) external view returns (uint8) {
        return voucherStatus[voucherNo];
    }

    function exists(string calldata voucherNo) external view returns (bool) {
        return voucherNoExists[voucherNo];
    }

    // ==================== 管理员功能 ====================

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }
}
