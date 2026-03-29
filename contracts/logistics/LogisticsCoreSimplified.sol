// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

contract LogisticsCore {

    address public admin;
    uint256 public delegateCount;
    mapping(string => bool) private voucherNoExists;
    mapping(string => uint8) private voucherStatus;

    event LogisticsDelegateCreated(string indexed voucherNo, address indexed operator, uint256 timestamp);
    event CarrierAssigned(string indexed voucherNo, bytes32 indexed carrierHash, address indexed operator, uint256 timestamp);
    event StatusUpdated(string indexed voucherNo, uint8 oldStatus, uint8 newStatus, address indexed operator, uint256 timestamp);

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin");
        _;
    }

    constructor(address _initialAdmin) {
        require(_initialAdmin != address(0), "Admin cannot be zero");
        admin = _initialAdmin;
    }

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
        voucherStatus[voucherNo] = 1;
        delegateCount++;
        emit LogisticsDelegateCreated(voucherNo, msg.sender, block.timestamp);
        return true;
    }

    function assignCarrier(string calldata voucherNo, bytes32 carrierHash) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 1, "Invalid status");
        voucherStatus[voucherNo] = 2;
        emit CarrierAssigned(voucherNo, carrierHash, msg.sender, block.timestamp);
        return true;
    }

    function pickup(string calldata voucherNo, uint256 quantity) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 2, "Invalid status for pickup");
        voucherStatus[voucherNo] = 3;
        emit StatusUpdated(voucherNo, 2, 3, msg.sender, block.timestamp);
        return true;
    }

    function arrive(string calldata voucherNo) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 3, "Invalid status for arrive");
        voucherStatus[voucherNo] = 4;
        emit StatusUpdated(voucherNo, 3, 4, msg.sender, block.timestamp);
        return true;
    }

    function arriveAndAddQuantity(
        string calldata voucherNo,
        string calldata targetReceiptId,
        uint256 quantity
    ) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 3, "Invalid status for arrive");
        voucherStatus[voucherNo] = 4;
        emit StatusUpdated(voucherNo, 3, 4, msg.sender, block.timestamp);
        return true;
    }

    function arriveAndCreateReceipt(
        string calldata voucherNo,
        string calldata newReceiptId,
        uint256 weight,
        string calldata unit,
        bytes32 ownerHash,
        bytes32 warehouseHash
    ) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        voucherStatus[voucherNo] = 4;
        emit StatusUpdated(voucherNo, 3, 4, msg.sender, block.timestamp);
        return true;
    }

    function confirmDelivery(
        string calldata voucherNo,
        uint256 action,
        string calldata targetReceiptId
    ) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] == 4, "Invalid status for confirm");
        voucherStatus[voucherNo] = 5;
        emit StatusUpdated(voucherNo, 4, 5, msg.sender, block.timestamp);
        return true;
    }

    function invalidate(string calldata voucherNo) external returns (bool success) {
        require(voucherNoExists[voucherNo], "Voucher not found");
        require(voucherStatus[voucherNo] != 5 && voucherStatus[voucherNo] != 0, "Cannot invalidate");
        voucherStatus[voucherNo] = 6;
        emit StatusUpdated(voucherNo, voucherStatus[voucherNo], 6, msg.sender, block.timestamp);
        return true;
    }

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

    function setAdmin(address newAdmin) external onlyAdmin returns (bool) {
        require(newAdmin != address(0), "Invalid address");
        admin = newAdmin;
        return true;
    }
}
