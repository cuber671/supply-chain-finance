// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

contract LogisticsCore {
    address public admin;
    mapping(string => bool) private voucherExists;
    mapping(string => uint8) private voucherStatus;

    event Created(string indexed, address, uint256);
    event StatusChanged(string indexed, uint8, uint8, address, uint256);

    constructor(address _admin) {
        admin = _admin;
    }

    function create(string calldata voucherNo) external {
        require(!voucherExists[voucherNo]);
        voucherExists[voucherNo] = true;
        voucherStatus[voucherNo] = 1;
        emit Created(voucherNo, msg.sender, block.timestamp);
    }

    function confirmDelivery(string calldata voucherNo, uint256 action, string calldata targetReceiptId) external {
        require(voucherExists[voucherNo]);
        require(voucherStatus[voucherNo] == 4);
        voucherStatus[voucherNo] = 5;
        emit StatusChanged(voucherNo, 4, 5, msg.sender, block.timestamp);
    }

    function invalidate(string calldata voucherNo) external {
        require(voucherExists[voucherNo]);
        voucherStatus[voucherNo] = 6;
        emit StatusChanged(voucherNo, 0, 6, msg.sender, block.timestamp);
    }

    function getStatus(string calldata voucherNo) external view returns (uint8) {
        return voucherStatus[voucherNo];
    }
}
