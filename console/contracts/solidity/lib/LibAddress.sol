// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LibAddress
 * @dev 地址操作工具库
 *
 * 提供常用的地址操作功能，包括地址验证、地址数组处理等
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
library LibAddress {

    /**
     * @dev 验证地址是否有效（非零地址）
     * @param addr 地址
     * @return 是否有效
     */
    function isValid(address addr) internal pure returns (bool) {
        return addr != address(0);
    }

    /**
     * @dev 验证地址数组是否有效（所有地址都非零）
     * @param addrs 地址数组
     * @return 是否全部有效
     */
    function areValid(address[] memory addrs) internal pure returns (bool) {
        for (uint256 i = 0; i < addrs.length; i++) {
            if (addrs[i] == address(0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @dev 检查地址数组中是否包含指定地址
     * @param addrs 地址数组
     * @param addr 要检查的地址
     * @return 是否包含
     */
    function contains(address[] memory addrs, address addr) internal pure returns (bool) {
        for (uint256 i = 0; i < addrs.length; i++) {
            if (addrs[i] == addr) {
                return true;
            }
        }
        return false;
    }

    /**
     * @dev 在地址数组中查找指定地址的索引
     * @param addrs 地址数组
     * @param addr 要查找的地址
     * @return 索引（如果未找到返回 uint256.max）
     */
    function indexOf(address[] memory addrs, address addr) internal pure returns (uint256) {
        for (uint256 i = 0; i < addrs.length; i++) {
            if (addrs[i] == addr) {
                return i;
            }
        }
        return type(uint256).max;
    }

    /**
     * @dev 从地址数组中移除指定地址
     * @param addrs 地址数组
     * @param addr 要移除的地址
     * @return 移除后的新数组
     */
    function remove(address[] memory addrs, address addr) internal pure returns (address[] memory) {
        uint256 index = indexOf(addrs, addr);
        if (index == type(uint256).max) {
            return addrs;
        }

        address[] memory result = new address[](addrs.length - 1);
        for (uint256 i = 0; i < index; i++) {
            result[i] = addrs[i];
        }
        for (uint256 i = index; i < result.length; i++) {
            result[i] = addrs[i + 1];
        }
        return result;
    }

    /**
     * @dev 地址数组去重
     * @param addrs 地址数组
     * @return 去重后的新数组
     */
    function distinct(address[] memory addrs) internal pure returns (address[] memory) {
        bool[] memory seen = new bool[](addrs.length);
        uint256 count = 0;

        for (uint256 i = 0; i < addrs.length; i++) {
            if (!seen[i]) {
                for (uint256 j = i + 1; j < addrs.length; j++) {
                    if (addrs[i] == addrs[j]) {
                        seen[j] = true;
                    }
                }
                seen[i] = true;
                count++;
            }
        }

        address[] memory result = new address[](count);
        uint256 index = 0;
        for (uint256 i = 0; i < addrs.length; i++) {
            if (seen[i]) {
                result[index] = addrs[i];
                index++;
            }
        }
        return result;
    }

    /**
     * @dev 验证地址是否为合约地址
     * @param addr 地址
     * @return 是否为合约地址
     */
    function isContract(address addr) internal view returns (bool) {
        uint256 size;
        assembly {
            size := extcodesize(addr)
        }
        return size > 0;
    }

    /**
     * @dev 地址转字符串
     * @param addr 地址
     * @return 地址的字符串表示
     */
    function toString(address addr) internal pure returns (string memory) {
        bytes memory bytesAddr = new bytes(42);
        bytes memory prefix = "0x";
        bytes20 addrBytes = bytes20(addr);

        bytes memory chars = hex"0123456789abcdef";

        bytes memory str = bytesAddr;
        str[0] = prefix[0];
        str[1] = prefix[1];

        for (uint256 i = 0; i < 20; i++) {
            str[2 + i * 2] = chars[uint8(addrBytes[i]) >> 4];
            str[2 + i * 2 + 1] = chars[uint8(addrBytes[i]) & 0xf];
        }
        return string(str);
    }

    /**
     * @dev 从地址生成确定性哈希
     * @param addr 地址
     * @param salt 盐值
     * @return 确定性哈希
     */
    function hash(address addr, bytes32 salt) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(addr, salt));
    }

    /**
     * @dev 计算两个地址的共同前缀哈希
     * @param addr1 第一个地址
     * @param addr2 第二个地址
     * @return 共同前缀哈希（用于买卖对验证）
     */
    function pairHash(address addr1, address addr2) internal pure returns (bytes32) {
        if (addr1 < addr2) {
            return keccak256(abi.encodePacked(addr1, addr2));
        } else {
            return keccak256(abi.encodePacked(addr2, addr1));
        }
    }

    /**
     * @dev 验证签名
     * @param signatory 签名地址
     * @param hash 哈希值
     * @param signature 签名
     * @return 签名是否有效
     */
    function isValidSignature(address signatory, bytes32 hash, bytes memory signature) internal pure returns (bool) {
        if (signature.length != 65) {
            return false;
        }

        bytes32 r;
        bytes32 s;
        uint8 v;

        assembly {
            r := mload(add(signature, 32))
            s := mload(add(signature, 64))
            v := byte(0, mload(add(signature, 96)))
        }

        if (v < 27) {
            v += 27;
        }

        if (v != 27 && v != 28) {
            return false;
        }

        return ecrecover(hash, v, r, s) == signatory;
    }
}
