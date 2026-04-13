# FISCO BCOS 智能合约部署指南

## 概述

本文档记录 FISCO BCOS 供应链金融平台的智能合约部署方法。

**部署环境：**
- 操作系统：Linux (WSL2)
- 容器：Docker Compose
- 区块链：FISCO BCOS 3.8.0
- 控制台：fisco-console

---

## 部署前准备

### 1. 环境检查

确保以下容器处于运行状态：

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

必须运行的容器：
- fisco-node0 ~ fisco-node3（FISCO 节点）
- fisco-console（控制台）
- fisco-mysql（数据库，可选）

### 2. 区块链管理员账户

部署使用管理员账户：
- **地址：** `0x6976c14175ddfae575ffab3845d89e155e09569d`
- **PEM 密钥：** `/home/llm_rca/fisco/my-bcos-app/fisco/nodes/127.0.0.1/sdk/account/0x6976c14175ddfae575ffab3845d89e155e09569d.pem`

控制台配置中已加载此账户（见 `config.toml`）。

---

## 标准部署流程

### Step 1：同步合约文件到控制台

将本地编译好的 Solidity 合约复制到控制台容器：

```bash
docker cp /home/llm_rca/fisco/supply-chain-finance/contracts/warehouse/WarehouseReceiptCore.sol fisco-console:/data/contracts/solidity/warehouse/
docker cp /home/llm_rca/fisco/supply-chain-finance/contracts/warehouse/IWarehouseReceiptCore.sol fisco-console:/data/contracts/solidity/warehouse/
```

### Step 2：部署合约

#### 关键发现：控制台部署命令语法

**重要：** FISCO Console 3.8.0 的 `deploy` 命令有特殊语法要求：

1. **必须使用绝对路径**，相对路径会报 "does not exist" 错误
2. **合约构造参数直接跟在路径后面**，空格分隔
3. **不带任何标志位**

```bash
docker exec -i fisco-console bash -c "cd /data && echo 'deploy /data/contracts/solidity/warehouse/WarehouseReceiptCore 0x6976c14175ddfae575ffab3845d89e155e09569d' | java -cp 'apps/*:conf/:lib/*:classes/' console.Console group0 2>&1"
```

**参数说明：**
- `/data/contracts/solidity/warehouse/WarehouseReceiptCore` - 合约的绝对路径
- `0x6976c14175ddfae575ffab3845d89e155e09569d` - 构造函数参数（admin 地址）

**成功输出示例：**
```
transaction hash: 0x90164473fc5523be4fad1fa9c99e9ea45aaaafac0740cb2395d02bc347cda963
contract address: 0xc8fdfc3e427e0f0d74533724f5ea198252a3b2d5
currentAccount: 0x6976c14175ddfae575ffab3845d89e155e09569d
```

### Step 3：验证部署记录

查看历史部署记录：

```bash
docker exec -i fisco-console bash -c "cd /data && echo 'listDeployContractAddress /data/contracts/solidity/warehouse/WarehouseReceiptCore' | java -cp 'apps/*:conf/:lib/*:classes/' console.Console group0 2>&1"
```

### Step 4：更新配置文件

#### 4.1 更新 .env 文件

```bash
# 编辑 .env 文件
vim /home/llm_rca/fisco/supply-chain-finance/.env

# 修改 CONTRACT_WAREHOUSE_CORE 为新地址
CONTRACT_WAREHOUSE_CORE=0xc8fdfc3e427e0f0d74533724f5ea198252a3b2d5
```

#### 4.2 更新 docker-compose.yml

```bash
# 编辑 docker-compose.yml
vim /home/llm_rca/fisco/supply-chain-finance/docker-compose.yml

# 找到 fisco-gateway-service 的环境变量部分，修改对应地址
- CONTRACT_WAREHOUSE_CORE=0xc8fdfc3e427e0f0d74533724f5ea198252a3b2d5
```

#### 4.3 更新部署日志

```bash
echo "2026-04-07 00:33:20  [group:group0]  WarehouseReceiptCore  0xc8fdfc3e427e0f0d74533724f5ea198252a3b2d5" >> /home/llm_rca/fisco/supply-chain-finance/console/deploylog.txt
```

### Step 5：重启服务

**关键：** 必须使用 `down && up` 强制重新加载环境变量，单纯 `restart` 不会重新读取环境变量：

```bash
# 停止所有服务
docker compose down

# 重新启动所有服务
docker compose up -d

# 验证新合约地址已加载
docker exec fisco-gateway-service env | grep CONTRACT_WAREHOUSE
```

### Step 6：验证服务

```bash
# 检查服务健康状态
docker ps --format "table {{.Names}}\t{{.Status}}"

# 查看 fisco-gateway-service 日志确认新合约加载
docker logs fisco-gateway-service 2>&1 | grep -i warehouse
```

**成功标志：**
```
仓单核心合约加载成功，地址: 0xc8fdfc3e427e0f0d74533724f5ea198252a3b2d5
```

---

## 常见问题

### Q1: "WarehouseReceiptCore does not exist"

**原因：** 控制台未找到编译后的合约文件

**解决：**
1. 确认合约文件已复制到正确位置：
   ```bash
   docker exec fisco-console ls -la /data/contracts/solidity/warehouse/
   ```

2. 使用**绝对路径**部署：
   ```bash
   deploy /data/contracts/solidity/warehouse/WarehouseReceiptCore <constructor_args>
   ```

### Q2: "expected 1 parameters but provided 0 parameters"

**原因：** 合约构造函数需要参数，但未提供

**解决：** 添加构造函数参数（如 admin 地址）

### Q3: 环境变量未更新

**原因：** Docker Compose 的 `restart` 命令不会重新读取修改后的环境变量

**解决：** 使用 `down && up -d` 强制重建容器

### Q4: 合约字节码超限 (EIP-170)

**原因：** 合约编译后字节码超过 24,576 bytes 限制

**解决：**
1. 删除未使用的函数
2. 删除重复的事件定义
3. 使用库合约提取公共代码
4. 启用优化编译 (`--optimize`)

**验证字节码大小：**
```bash
# 查看编译后的 bin 文件大小
docker exec fisco-console ls -la /data/contracts/.compiled/WarehouseReceiptCore.bin
```

---

## 快速参考

### 部署命令模板

```bash
# 1. 复制合约
docker cp <local_path>/<ContractName>.sol fisco-console:/data/contracts/solidity/<module>/

# 2. 部署（带构造参数）
docker exec -i fisco-console bash -c "cd /data && echo 'deploy /data/contracts/solidity/<module>/<ContractName> <param1> <param2>' | java -cp 'apps/*:conf/:lib/*:classes/' console.Console group0 2>&1"

# 3. 记录返回的地址

# 4. 更新配置
# .env
# docker-compose.yml

# 5. 重启服务
docker compose down && docker compose up -d
```

### 常用控制台命令

```bash
# 列出已部署的合约地址
listDeployContractAddress <contractPath>

# 查看部署历史
getDeployLog

# 查询链上代码
getCode <contractAddress>

# 查询区块高度
getBlockNumber
```

### 合约配置变量名对照表

| 合约 | 环境变量 |
|------|----------|
| WarehouseReceiptCore | CONTRACT_WAREHOUSE_CORE |
| WarehouseReceiptOps | CONTRACT_WAREHOUSE_OPS |
| EnterpriseRegistryV2 | CONTRACT_ENTERPRISE |
| CreditLimitCore | CONTRACT_CREDIT_CORE |
| LoanCore | CONTRACT_LOAN_CORE |
| LogisticsCore | CONTRACT_LOGISTICS_CORE |

---

## 附录：部署脚本参考

项目中的自动化部署脚本位于：
- `/home/llm_rca/fisco/my-bcos-app/scripts/compile-contracts.sh`
- `/home/llm_rca/fisco/my-bcos-app/scripts/deploy-all-contracts.sh`

这些脚本提供了批量编译和部署的功能，但需要注意控制台环境的差异。

---

**最后更新：** 2026-04-07
