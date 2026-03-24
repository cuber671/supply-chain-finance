// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title EnterpriseRegistryAuth
 * @dev 企业注册权限控制库
 *
 * 提供企业模块的权限控制修饰器，包括：
 * - 管理员权限控制
 * - Java后端权限控制
 * - 企业所有者权限控制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
library EnterpriseRegistryAuth {

    /**
     * @dev 权限错误
     */
    error OnlyAdmin();
    error OnlyJavaBackend();
    error OnlyEnterpriseOwner(address owner);
    error OnlyValidEnterprise();

    /**
     * @dev 管理员权限修饰器
     * @param adminAddress 当前管理员地址
     */
    modifier onlyAdmin(address adminAddress) {
        if (msg.sender != adminAddress) {
            revert OnlyAdmin();
        }
        _;
    }

    /**
     * @dev Java后端权限修饰器
     * @param javaBackendAddress Java后端地址
     */
    modifier onlyJavaBackend(address javaBackendAddress) {
        if (msg.sender != javaBackendAddress) {
            revert OnlyJavaBackend();
        }
        _;
    }

    /**
     * @dev 企业所有者权限修饰器
     * @param enterpriseAddress 企业地址
     */
    modifier onlyEnterpriseOwner(address enterpriseAddress) {
        if (msg.sender != enterpriseAddress) {
            revert OnlyEnterpriseOwner(enterpriseAddress);
        }
        _;
    }

    /**
     * @dev 验证企业是否存在
     * @param enterpriseAddress 企业地址
     * @param checkAddress 检查地址
     */
    modifier onlyValidEnterpriseAddress(address enterpriseAddress, address checkAddress) {
        if (enterpriseAddress != checkAddress) {
            revert OnlyValidEnterprise();
        }
        _;
    }

    /**
     * @dev 组合权限修饰器：仅管理员或Java后端
     * @param adminAddress 管理员地址
     * @param javaBackendAddress Java后端地址
     */
    modifier onlyAdminOrBackend(address adminAddress, address javaBackendAddress) {
        if (msg.sender != adminAddress && msg.sender != javaBackendAddress) {
            revert OnlyAdmin();
        }
        _;
    }

    /**
     * @dev 组合权限修饰器：仅管理员或企业所有者
     * @param adminAddress 管理员地址
     * @param enterpriseAddress 企业地址
     */
    modifier onlyAdminOrOwner(address adminAddress, address enterpriseAddress) {
        if (msg.sender != adminAddress && msg.sender != enterpriseAddress) {
            revert OnlyAdmin();
        }
        _;
    }

    /**
     * @dev 验证地址有效性
     * @param addr 地址
     */
    function validateAddress(address addr) internal pure {
        require(addr != address(0), "Invalid zero address");
    }

    /**
     * @dev 验证地址数组有效性
     * @param addrs 地址数组
     */
    function validateAddresses(address[] memory addrs) internal pure {
        require(addrs.length > 0, "Empty address array");
        for (uint256 i = 0; i < addrs.length; i++) {
            require(addrs[i] != address(0), "Contains zero address");
        }
    }
}
