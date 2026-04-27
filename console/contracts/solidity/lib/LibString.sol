// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LibString
 * @dev 字符串操作工具库
 *
 * 提供常用的字符串操作功能，包括字符串比较、拼接、转换等
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
library LibString {

    /**
     * @dev 比较两个字符串是否相等
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 是否相等
     */
    function equals(string memory a, string memory b) internal pure returns (bool) {
        return keccak256(abi.encodePacked(a)) == keccak256(abi.encodePacked(b));
    }

    /**
     * @dev 比较两个字符串是否相等（忽略大小写）
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 是否相等
     */
    function equalsIgnoreCase(string memory a, string memory b) internal pure returns (bool) {
        return keccak256(abi.encodePacked(toLower(a))) == keccak256(abi.encodePacked(toLower(b)));
    }

    /**
     * @dev 字符串拼接
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 拼接后的字符串
     */
    function concat(string memory a, string memory b) internal pure returns (string memory) {
        return string(abi.encodePacked(a, b));
    }

    /**
     * @dev 字符串拼接（三个参数）
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @param c 第三个字符串
     * @return 拼接后的字符串
     */
    function concat(string memory a, string memory b, string memory c) internal pure returns (string memory) {
        return string(abi.encodePacked(a, b, c));
    }

    /**
     * @dev 获取字符串长度
     * @param s 字符串
     * @return 字符串长度
     */
    function length(string memory s) internal pure returns (uint256) {
        return bytes(s).length;
    }

    /**
     * @dev 判断字符串是否为空
     * @param s 字符串
     * @return 是否为空
     */
    function isEmpty(string memory s) internal pure returns (bool) {
        return bytes(s).length == 0;
    }

    /**
     * @dev 字符串转bytes
     * @param s 字符串
     * @return 字节数组
     */
    function toBytes(string memory s) internal pure returns (bytes memory) {
        return bytes(s);
    }

    /**
     * @dev bytes转字符串
     * @param b 字节数组
     * @return 字符串
     */
    function fromBytes(bytes memory b) internal pure returns (string memory) {
        return string(b);
    }

    /**
     * @dev 字符串转bytes32
     * @param s 字符串（长度必须为32字节）
     * @return bytes32结果
     */
    function toBytes32(string memory s) internal pure returns (bytes32) {
        require(bytes(s).length == 32, "String must be exactly 32 bytes");
        return bytes32(bytes(s));
    }

    /**
     * @dev bytes32转字符串
     * @param b bytes32数据
     * @return 字符串
     */
    function fromBytes32(bytes32 b) internal pure returns (string memory) {
        bytes memory s = new bytes(32);
        assembly {
            mstore(add(s, 32), b)
        }
        return string(s);
    }

    /**
     * @dev 字符串转小写
     * @param s 原始字符串
     * @return 小写字符串
     */
    function toLower(string memory s) internal pure returns (string memory) {
        bytes memory bStr = bytes(s);
        bytes memory bLower = new bytes(bStr.length);
        for (uint256 i = 0; i < bStr.length; i++) {
            // ASCII: A=65, Z=90, a=97, z=122
            if ((uint8(bStr[i]) >= 65) && (uint8(bStr[i]) <= 90)) {
                bLower[i] = bytes1(uint8(bStr[i]) + 32);
            } else {
                bLower[i] = bStr[i];
            }
        }
        return string(bLower);
    }

    /**
     * @dev 字符串转大写
     * @param s 原始字符串
     * @return 大写字符串
     */
    function toUpper(string memory s) internal pure returns (string memory) {
        bytes memory bStr = bytes(s);
        bytes memory bUpper = new bytes(bStr.length);
        for (uint256 i = 0; i < bStr.length; i++) {
            // ASCII: a=97, z=122, A=65, Z=90
            if ((uint8(bStr[i]) >= 97) && (uint8(bStr[i]) <= 122)) {
                bUpper[i] = bytes1(uint8(bStr[i]) - 32);
            } else {
                bUpper[i] = bStr[i];
            }
        }
        return string(bUpper);
    }

    /**
     * @dev 字符串截取
     * @param s 原始字符串
     * @param start 起始位置
     * @param length 截取长度
     * @return 截取后的字符串
     */
    function substring(string memory s, uint256 start, uint256 length) internal pure returns (string memory) {
        bytes memory bStr = bytes(s);
        require(bStr.length >= start + length, "Substring out of bounds");
        bytes memory result = new bytes(length);
        for (uint256 i = 0; i < length; i++) {
            result[i] = bStr[start + i];
        }
        return string(result);
    }

    /**
     * @dev 判断字符串是否以指定前缀开头
     * @param s 字符串
     * @param prefix 前缀
     * @return 是否以指定前缀开头
     */
    function startsWith(string memory s, string memory prefix) internal pure returns (bool) {
        bytes memory bStr = bytes(s);
        bytes memory bPrefix = bytes(prefix);
        if (bStr.length < bPrefix.length) {
            return false;
        }
        for (uint256 i = 0; i < bPrefix.length; i++) {
            if (bStr[i] != bPrefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @dev 判断字符串是否以指定后缀结尾
     * @param s 字符串
     * @param suffix 后缀
     * @return 是否以指定后缀结尾
     */
    function endsWith(string memory s, string memory suffix) internal pure returns (bool) {
        bytes memory bStr = bytes(s);
        bytes memory bSuffix = bytes(suffix);
        if (bStr.length < bSuffix.length) {
            return false;
        }
        uint256 offset = bStr.length - bSuffix.length;
        for (uint256 i = 0; i < bSuffix.length; i++) {
            if (bStr[offset + i] != bSuffix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @dev 字符串哈希
     * @param s 字符串
     * @return keccak256哈希值
     */
    function hash(string memory s) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(s));
    }
}
