# FISCO BCOS 链上数据查询方法

## 1. 概述

本文档记录如何使用 FISCO BCOS Console 查询链上智能合约数据。

**前置条件：**
- FISCO BCOS 节点正常运行
- Console 环境已配置（节点连接、账户等）
- 目标合约已部署并已知地址

## 2. 查询命令格式

### 2.1 基本语法

```bash
call <合约路径> <合约地址> <方法名> [参数1] [参数2] ...
```

### 2.2 参数说明

| 参数 | 说明 |
|------|------|
| 合约路径 | .sol 源文件的完整路径（控制台从文件解析 ABI） |
| 合约地址 | 合约部署地址（20字节，0x 开头） |
| 方法名 | 要调用的合约方法名（必须是 view/pure 方法） |
| 参数 | 方法参数，多个参数用空格分隔 |

## 3. 实际案例：查询仓单数据

### 3.1 查询场景

查询仓单合约 `WarehouseReceiptCore` 中某个仓单的详细信息。

### 3.2 执行命令

```bash
docker exec fisco-console bash -c 'echo "call /data/contracts/solidity/warehouse/WarehouseReceiptCore.sol 0x2d13fca449737d740716c0f0841becb16f0f9e78 getReceipt \"2042829397927989258\"" | timeout 15 bash -c "cd /data && ./start.sh" 2>&1'
```

### 3.3 命令分解

| 部分 | 值 |
|------|-----|
| 合约路径 | `/data/contracts/solidity/warehouse/WarehouseReceiptCore.sol` |
| 合约地址 | `0x2d13fca449737d740716c0f0841becb16f0f9e78` |
| 方法名 | `getReceipt` |
| 参数 | `2042829397927989258`（仓单ID） |

### 3.4 返回结果

```
Return code: 0
description: transaction executed successfully
Return message: Success
---------------------------------------------------------------------------------------------
Return value size: 7
Return types: (STRING, BYTES, BYTES, UINT, STRING, UINT, UINT)
Return values: (2042829397927989258, hex://0x0000000000000000000000000000000000000000000000001c5ca4ac41c07001, hex://0x0000000000000000000000000000000000000000000000000000000000000003, 200, 个, 1, 1)
---------------------------------------------------------------------------------------------
```

### 3.5 返回值解析

| 索引 | 类型 | 返回值 | 含义 |
|------|------|--------|------|
| 0 | STRING | 2042829397927989258 | 仓单ID |
| 1 | BYTES | 0x...1c5ca4ac41c07001 | 货主哈希（entityIdToBytes32 编码） |
| 2 | BYTES | 0x...03 | 仓库哈希 |
| 3 | UINT | 200 | 重量 |
| 4 | STRING | 个 | 单位 |
| 5 | UINT | 1 | 数量 |
| 6 | UINT | 1 | 状态（1=InStorage） |

## 4. 合约地址配置参考

常用合约地址（来自 `.env`）：

```bash
# 仓单模块
CONTRACT_WAREHOUSE_CORE=0x2d13fca449737d740716c0f0841becb16f0f9e78
CONTRACT_WAREHOUSE_OPS=0x8fce611560492151fde78c8f1cebcd719ee42124

# 企业模块
CONTRACT_ENTERPRISE=0x5f04ffa3cab8f99ddf421605a47ce1b4f63fa12c

# 信用模块
CONTRACT_CREDIT_CORE=0xafcdafa5be0a0e2c34328adf10d893a591b5e774

# 物流模块
CONTRACT_LOGISTICS_CORE=0xf10f6913a12ab1300b12f29e37251e03dda5faaa
```

## 5. 常见问题

### 5.1 合约文件不存在

确保 .sol 文件在 `/data/contracts/solidity/` 目录下。

### 5.2 合约地址错误

```
Error: contract not found at address xxx
```

检查 `.env` 文件中的 `CONTRACT_*` 配置是否正确。

### 5.3 方法参数类型不匹配

```
Error: invalid argument 0
```

确保参数数量和类型与合约方法签名一致。

## 6. 自动化脚本模板

```bash
#!/bin/bash
# 查询链上仓单数据

CONTRACT_PATH="/data/contracts/solidity/warehouse/WarehouseReceiptCore.sol"
CONTRACT_ADDR="0x2d13fca449737d740716c0f0841becb16f0f9e78"
RECEIPT_ID="$1"

if [ -z "$RECEIPT_ID" ]; then
    echo "Usage: $0 <receiptId>"
    exit 1
fi

docker exec fisco-console bash -c "echo \"call $CONTRACT_PATH $CONTRACT_ADDR getReceipt \\\"$RECEIPT_ID\\\"\" | timeout 15 bash -c \"cd /data && ./start.sh\" 2>&1"
```

## 7. 相关文档

- [FISCO BCOS Console 使用文档](https://fisco-bcos-documentation.readthedocs.io/zh_CN/latest/docs/console/console.html)
- 合约 ABI 定义：`/data/contracts/solidity/warehouse/IWarehouseReceiptCore.sol`
