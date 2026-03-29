// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title IReceivableCore
 * @dev 应收款核心合约接口
 *
 * 用于跨合约调用，定义应收款核心业务操作的接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
interface IReceivableCore {

    // ==================== 还款操作 ====================

    /**
     * @dev 更新应收款余额
     * @param receivableId 应收款ID
     * @param amount 还款金额
     * @param isFull 是否结清
     * @return success 是否成功
     */
    function updateBalance(string calldata receivableId, uint256 amount, bool isFull)
        external returns (bool success);

    /**
     * @dev 结清应收款
     * @param receivableId 应收款ID
     * @return success 是否成功
     */
    function settleReceivable(string calldata receivableId)
        external returns (bool success);

    // ==================== 查询操作 ====================

    /**
     * @dev 获取应收款余额
     * @param receivableId 应收款ID
     * @return balanceUnpaid 剩余未还金额
     */
    function getBalanceUnpaid(string calldata receivableId)
        external view returns (uint256 balanceUnpaid);

    /**
     * @dev 获取应收款状态
     * @param receivableId 应收款ID
     * @return status 应收款状态
     */
    function getReceivableStatus(string calldata receivableId)
        external view returns (uint8 status);

    /**
     * @dev 检查应收款是否存在
     * @param receivableId 应收款ID
     * @return exists 是否存在
     */
    function exists(string calldata receivableId) external view returns (bool exists);

    /**
     * @dev 获取债务人哈希
     * @param receivableId 应收款ID
     * @return debtorHash 债务人哈希
     */
    function getDebtorHash(string calldata receivableId) external view returns (bytes32 debtorHash);
}
