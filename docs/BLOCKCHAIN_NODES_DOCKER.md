# FISCO BCOS 节点与控制台 Docker 配置说明

## 1. 概述

本项目使用 Docker Compose 部署 FISCO BCOS 区块链网络，包含 4 个区块链节点和 1 个控制台容器。

## 2. 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                    fisco-app-net (172.26.0.0/16)                │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌───────────┐
│  │ fisco-node0 │  │ fisco-node1 │  │ fisco-node2 │  │ fisco-node3│
│  │172.26.0.20  │  │172.26.0.21  │  │172.26.0.22  │  │172.26.0.23 │
│  │ RPC:20000   │  │ RPC:20001   │  │ RPC:20002   │  │ RPC:20003  │
│  │ P2P:30300   │  │ P2P:30301   │  │ P2P:30302   │  │ P2P:30303  │
│  │ CH:30400    │  │ CH:30401    │  │ CH:30402    │  │ CH:30403   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └───────────┘
│                                                                 │
│  ┌─────────────┐                                                │
│  │fisco-console│ 172.26.0.30 - 区块链控制台                      │
│  │ (可交互)    │                                                │
│  └─────────────┘                                                │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │           fisco-gateway-service (172.26.0.16:8087)          ││
│  │                    区块链网关服务                            ││
│  │         连接节点: ./fisco/nodes/127.0.0.1/sdk               ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## 3. 节点配置详情

### 3.1 节点容器列表

| 容器名 | IP地址 | RPC端口 | P2P端口 | Channel端口 | 数据卷 |
|--------|--------|---------|---------|-------------|--------|
| fisco-node0 | 172.26.0.20 | 20000 | 30300 | 30400 | ./fisco/nodes/127.0.0.1/node0 |
| fisco-node1 | 172.26.0.21 | 20001 | 30301 | 30401 | ./fisco/nodes/127.0.0.1/node1 |
| fisco-node2 | 172.26.0.22 | 20002 | 30302 | 30402 | ./fisco/nodes/127.0.0.1/node2 |
| fisco-node3 | 172.26.0.23 | 20003 | 30303 | 30403 | ./fisco/nodes/127.0.0.1/node3 |

### 3.2 节点镜像

```yaml
image: fiscoorg/fiscobcos:v3.12.1
```

### 3.3 节点启动命令

```bash
command: ["-c", "config.ini", "-g", "config.genesis"]
```

- `-c config.ini`: 使用 config.ini 配置文件
- `-g config.genesis`: 使用 config.genesis 创世块配置

### 3.4 节点资源配置

```yaml
deploy:
  resources:
    limits:
      memory: 2048M
      cpus: '1.5'
    reservations:
      memory: 1024M
      cpus: '0.5'
```

## 4. 控制台配置

### 4.1 控制台容器

| 容器名 | IP地址 | 说明 |
|--------|--------|------|
| fisco-console | 172.26.0.30 | FISCO BCOS 控制台（可交互） |

### 4.2 控制台镜像

```yaml
build:
  context: .
  dockerfile: Dockerfile.console
image: fisco/fisco-console:${IMAGE_TAG}
```

### 4.3 控制台工作目录

```yaml
working_dir: /data
volumes:
  - ./console:/data   # 挂载本地 console 目录到容器 /data
```

### 4.4 控制台启动方式

```bash
# 进入控制台容器
docker exec -it fisco-console bash

# 在容器内执行命令
cd /data && ./start.sh
```

## 5. 常用操作

### 5.1 查看节点状态

```bash
# 查看节点容器
docker ps --format "table {{.Names}}\t{{.Status}}" | grep fisco-node

# 查看节点日志
docker logs fisco-node0 --tail 50

# 进入控制台查看链上状态
docker exec -it fisco-console bash -c 'echo "getNodeVersion" | timeout 15 bash -c "cd /data && ./start.sh" 2>&1'
```

### 5.2 查询链上数据

```bash
# 查询仓单信息
docker exec -it fisco-console bash -c 'echo "call /data/contracts/solidity/warehouse/WarehouseReceiptCore.sol 0x2d13fca449737d740716c0f0841becb16f0f9e78 getReceipt \"WR-1776749339022-0269\"" | timeout 15 bash -c "cd /data && ./start.sh" 2>&1'

# 查询物流委派单状态
docker exec -it fisco-console bash -c 'echo "call /data/contracts/solidity/logistics/LogisticsCore.sol 0xf10f6913a12ab1300b12f29e37251e03dda5faaa getStatus \"DPDO202604214BBDE61F\"" | timeout 15 bash -c "cd /data && ./start.sh" 2>&1'
```

### 5.3 重启节点

```bash
# 重启单个节点
docker compose restart fisco-node0

# 重启所有节点
docker compose restart fisco-node0 fisco-node1 fisco-node2 fisco-node3
```

## 6. 合约地址

### 6.1 合约地址配置（在 .env 文件中）

```bash
# 企业模块
CONTRACT_ENTERPRISE=0x5f04ffa3cab8f99ddf421605a47ce1b4f63fa12c
CONTRACT_ENTERPRISE_AUTH=0xc860ab27901b3c2b810165a6096c64d88763617f

# 仓单模块
CONTRACT_WAREHOUSE_CORE=0x2d13fca449737d740716c0f0841becb16f0f9e78
CONTRACT_WAREHOUSE_OPS=0x8fce611560492151fde78c8f1cebcd719ee42124
CONTRACT_WAREHOUSE_CORE_EXT=0x55b63f96d81f094729af702adfc7af72bd75c54e

# 物流模块
CONTRACT_LOGISTICS_CORE=0xf10f6913a12ab1300b12f29e37251e03dda5faaa
CONTRACT_LOGISTICS_OPS=0x525f4e5362d8b15e5d4aa0335b2d153c70aa5eca

# 金融模块
CONTRACT_RECEIVABLE_CORE=0x45f32e71e57a847716b33aaf2996e2cec9259145
CONTRACT_RECEIVABLE_REPAYMENT=0x542c7a65c4ae367ceb7de3a428facbc94de77a51
CONTRACT_LOAN_CORE=0xbf14744175b48ac9a2e1fc4ebc6c0a5f4afd0ad2
CONTRACT_LOAN_REPAYMENT=0xd688eabe4597d2d23180045a4444d0c3450b6ab9

# 信用模块
CONTRACT_CREDIT_CORE=0xafcdafa5be0a0e2c34328adf10d893a591b5e774
CONTRACT_CREDIT_SCORE=0x6ea6907f036ff456d2f0f0a858afa9807ff4b788
```

### 6.2 合约文件路径（控制台内）

```
/data/contracts/solidity/warehouse/WarehouseReceiptCore.sol
/data/contracts/solidity/warehouse/WarehouseReceiptOps.sol
/data/contracts/solidity/logistics/LogisticsCore.sol
/data/contracts/solidity/logistics/LogisticsOps.sol
/data/contracts/solidity/enterprise/EnterpriseRegistryV2.sol
/data/contracts/solidity/enterprise/EnterpriseAuth.sol
/data/contracts/solidity/credit/CreditCore.sol
/data/contracts/solidity/credit/CreditScore.sol
/data/contracts/solidity/receivable/ReceivableCore.sol
/data/contracts/solidity/receivable/ReceivableRepayment.sol
/data/contracts/solidity/loan/LoanCore.sol
/data/contracts/solidity/loan/LoanRepayment.sol
```

## 7. 区块链网关服务连接

fisco-gateway-service 通过以下方式连接节点：

```yaml
# docker-compose.yml 中
volumes:
  - ./fisco/nodes/127.0.0.1/sdk:/app/sdk   # SDK 配置挂载

environment:
  - FISCO_CONFIG_FILE=/app/sdk/config.toml
  - FISCO_GROUP=group0
```

SDK 配置位于宿主机的 `./fisco/nodes/127.0.0.1/sdk` 目录，挂载到容器的 `/app/sdk`。

## 8. 端口映射

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|----------|------------|------|
| fisco-node0 RPC | 20000 | 20000 | 节点0 RPC接口 |
| fisco-node1 RPC | 20001 | 20001 | 节点1 RPC接口 |
| fisco-node2 RPC | 20002 | 20002 | 节点2 RPC接口 |
| fisco-node3 RPC | 20003 | 20003 | 节点3 RPC接口 |
| fisco-console | - | - | 无端口映射，需 exec 进入 |

## 9. 数据持久化

节点数据通过 Docker Volume 持久化：

```yaml
volumes:
  - ./fisco/nodes/127.0.0.1/node0:/data/node   # 节点数据目录挂载
  - fisco-node0-logs:/logs                      # 节点日志卷
```

**注意**：节点数据同时挂载到宿主机目录（持久）和 Docker 卷（临时），确保数据安全。

## 10. 常见问题

### 10.1 控制台连接失败

```bash
# 检查节点是否运行
docker ps | grep fisco-node

# 检查节点日志
docker logs fisco-node0 --tail 20

# 重启节点
docker compose restart fisco-node0
```

### 10.2 合约调用失败 "contract not found"

检查合约地址是否正确，以及合约文件路径是否正确。

### 10.3 节点数据不一致

如需重置节点数据：
```bash
# 停止容器
docker compose down

# 删除节点数据（谨慎操作！）
rm -rf ./fisco/nodes/127.0.0.1/node*/data

# 重新启动
docker compose up -d
```

## 11. 相关文档

- [链上数据查询方法](./chain-query-method.md)
- [合约升级指南](./CONTRACT_UPGRADE_GUIDE.md)
