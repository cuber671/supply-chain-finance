// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;

/**
 * @title LibBytes
 * @dev 字节操作工具库
 *
 * 提供常用的字节数组操作功能，减少合约代码重复
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
library LibBytes {

    /**
     * @dev 字节数组合并
     * @param a 第一个字节数组
     * @param b 第二个字节数组
     * @return 合并后的字节数组
     */
    function concat(bytes memory a, bytes memory b) internal pure returns (bytes memory) {
        return abi.encodePacked(a, b);
    }

    /**
     * @dev 字节数组切片
     * @param data 原始字节数组
     * @param start 起始位置
     * @param length 切片长度
     * @return 切片后的字节数组
     */
    function slice(bytes memory data, uint256 start, uint256 length) internal pure returns (bytes memory) {
        require(data.length >= start + length, "Slice out of bounds");
        bytes memory result = new bytes(length);
        for (uint256 i = 0; i < length; i++) {
            result[i] = data[start + i];
        }
        return result;
    }

    /**
     * @dev 比较两个字节数组是否相等
     * @param a 第一个字节数组
     * @param b 第二个字节数组
     * @return 是否相等
     */
    function equals(bytes memory a, bytes memory b) internal pure returns (bool) {
        if (a.length != b.length) {
            return false;
        }
        for (uint256 i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @dev 字节数组转 bytes32
     * @param data 字节数组（长度必须为32）
     * @return bytes32 结果
     */
    function toBytes32(bytes memory data) internal pure returns (bytes32) {
        require(data.length == 32, "Input must be 32 bytes");
        bytes32 result;
        assembly {
            result := mload(add(data, 32))
        }
        return result;
    }

    /**
     * @dev bytes32 转字节数组
     * @param data bytes32 数据
     * @return 32字节数组
     */
    function fromBytes32(bytes32 data) internal pure returns (bytes memory) {
        bytes memory result = new bytes(32);
        assembly {
            mstore(add(result, 32), data)
        }
        return result;
    }

    /**
     * @dev 将字节数组填充到指定长度（左侧填充0）
     * @param data 原始字节数组
     * @param length 目标长度
     * @return 填充后的字节数组
     */
    function padLeft(bytes memory data, uint256 length) internal pure returns (bytes memory) {
        require(data.length <= length, "Data too long");
        bytes memory result = new bytes(length);
        uint256 offset = length - data.length;
        for (uint256 i = 0; i < offset; i++) {
            result[i] = bytes1(0);
        }
        for (uint256 i = 0; i < data.length; i++) {
            result[offset + i] = data[i];
        }
        return result;
    }
}
