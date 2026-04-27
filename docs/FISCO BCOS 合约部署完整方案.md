# FISCO BCOS 合约部署完整方案

## 概述

本文档描述将智能合约部署到 FISCO BCOS 区块链新节点的完整流程，包括编译、部署、Java绑定类生成、ABI处理等所有步骤。

**适用场景**：区块链节点重置/新搭、合约升级、合约重新部署

---

## 前置检查清单

### 1. 环境状态确认

```bash
# 启动节点
cd /home/llm_rca/fisco/my-bcos-app
docker-compose up -d

# 确认容器运行
docker ps --format "table {{.Names}}\t{{.Status}}"

# 验证节点连通性
curl -s -X POST http://localhost:20000 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}'
```

**必须运行的容器**：fisco-node0, fisco-node1, fisco-node2, fisco-node3, fisco-console

### 2. 算法类型确认

| 类型          | 配置值       | 生成Java类包路径                 |
| ------------- | ------------ | -------------------------------- |
| 非国密(ECDSA) | cryptoType=0 | com.fisco.bcos.sdk...            |
| 国密(SM2)     | cryptoType=1 | org.fisco.bcos.sdk.crypto.sm2... |

检查方式：查看 `docker-compose.yml` 或 `config.toml` 中节点的 cryptoType 配置。

### 3. 账户权限对齐

**关键**：控制台部署用的私钥必须与网关服务（fisco-gateway-service）中配置的私钥为同一账户，否则上链交易会报 `Permission Denied` 或 `Revert`。

检查配置文件：

```bash
cat docker/config/config.toml | grep -E "accountAddress|keyStoreDir"
```

默认管理员账户地址：`0x6976c14175ddfae575ffab3845d89e155e09569d`

---

## 合约部署流程

### 步骤一：编译合约

#### 方式A - 使用控制台内嵌编译器（推荐）

```bash
cd /home/llm_rca/fisco/my-bcos-app
./scripts/compile-contracts.sh
```

#### 方式B - 手动编译

```bash
# 1. 复制合约到容器
docker cp contracts/enterprise/EnterpriseRegistryV2.sol \
  fisco-console:/data/contracts/solidity/

# 2. 在容器内编译
docker exec fisco-console solc --abi --bin --optimize \
  -o /data/contracts/build \
  /data/contracts/solidity/EnterpriseRegistryV2.sol
```

编译产物位置：

- ABI: `/data/contracts/build/abi/*.abi`
- BIN: `/data/contracts/build/bin/*.bin`

---

### 步骤二：部署合约（按依赖顺序）

#### 2.1 先部署库合约（无依赖）

```bash
# 部署 LibBytes
LIB_BYTES_OUTPUT=$(echo "deploy LibBytes" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0 2>&1)
LIB_BYTES_ADDR=$(echo "$LIB_BYTES_OUTPUT" | grep -oE '0x[0-9a-fA-F]{40}' | head -1)
echo "LibBytes: $LIB_BYTES_ADDR"

# 部署 LibString
LIB_STRING_OUTPUT=$(echo "deploy LibString" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0 2>&1)
LIB_STRING_ADDR=$(echo "$LIB_STRING_OUTPUT" | grep -oE '0x[0-9a-fA-F]{40}' | head -1)
echo "LibString: $LIB_STRING_ADDR"
```

#### 2.2 部署主合约

**无需库依赖的合约**：

```bash
# EnterpriseRegistryV2
echo "deploy EnterpriseRegistryV2" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# EnterpriseRegistryAuth
echo "deploy EnterpriseRegistryAuth" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# CreditLimitCore
echo "deploy CreditLimitCore" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# CreditLimitScore
echo "deploy CreditLimitScore" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# WarehouseReceiptCore
echo "deploy WarehouseReceiptCore" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# WarehouseReceiptOps
echo "deploy WarehouseReceiptOps" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# WarehouseReceiptHistory
echo "deploy WarehouseReceiptHistory" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# ReceivableCore
echo "deploy ReceivableCore" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# ReceivableRepayment
echo "deploy ReceivableRepayment" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# LogisticsCore
echo "deploy LogisticsCore" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

# LogisticsOps
echo "deploy LogisticsOps" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0
```

**依赖库合约的合约**（如新版 LoanCore）：

```bash
# 使用 -l 参数注入库地址
echo "deploy LoanCore" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0

echo "deploy LoanRepayment" | docker exec -i fisco-console \
  java -cp "apps/*:conf/:lib/*:classes/" console.Console group0
```

#### 2.3 记录部署地址

部署成功后控制台会输出：

```
contract address: 0x7a9b6d564d5d191093a29b7c760dd6af931cae73
```

保存到部署日志：

```bash
echo "$(date '+%Y-%m-%d %H:%M:%S')  [group:group0]  EnterpriseRegistryV2  0x7a9b6d564d5d191093a29b7c760dd6af931cae73" \
  >> /home/llm_rca/fisco/my-bcos-app/console/deploylog.txt
```

---

### 步骤三：生成Java绑定类

#### 3.1 执行生成命令

```bash
docker exec fisco-console java -cp "apps/*:lib/*:conf/" \
  console.common.ConsoleUtils solidity \
  -s /data/contracts/solidity/ \
  -o /data/contracts/sdk/java/ \
  -p com.fisco.app.contract \
  -l LibBytes:0x19a6434154de51c7a7406edf312f01527441b561 \
  -l LibString:0x745d4de0cf93b7d1db8dd8892daf05ac745766ce
```

**参数说明**：

| 参数   | 说明                        | 示例值                        |
| ------ | --------------------------- | ----------------------------- |
| `-s` | 合约文件或目录路径          | `/data/contracts/solidity/` |
| `-o` | 输出Java代码目录            | `/data/contracts/sdk/java/` |
| `-p` | 包名                        | `com.fisco.app.contract`    |
| `-l` | 库名:地址（多个用空格分隔） | `LibBytes:0x19a...`         |
| `-t` | 交易版本（建议2）           | `2`                         |
| `-e` | 启用异步调用                | （可选）                      |

#### 3.2 拷贝Java类到项目（关键修正）

**常见错误**：使用 `docker cp src dest` 会导致目录嵌套为 `.../contract/contract/xxx.java`

**正确方式**：使用 `/.` 只拷贝目录内容

```bash
# ★ 修正：使用 /. 避免嵌套目录
docker cp fisco-console:/data/contracts/sdk/java/. \
  /home/llm_rca/fisco/supply-chain-finance/services/fisco-gateway-service/src/main/java/

# ★ 修正：修改属主避免Root权限锁定
sudo chown -R $(whoami):$(whoami) \
  /home/llm_rca/fisco/supply-chain-finance/services/fisco-gateway-service/src/main/java/com/fisco/app/contract/
```

#### 3.3 验证Java类

```bash
# 确认文件已正确拷贝
ls -la /home/llm_rca/fisco/supply-chain-finance/services/fisco-gateway-service/src/main/java/com/fisco/app/contract/

# 确认没有嵌套目录（不应该有 com/fisco/app/contract/com/fisco/app/contract/）
find /home/llm_rca/fisco/supply-chain-finance/services/fisco-gateway-service/src/main/java/com/fisco/app/contract/ -name "*.java" | head -5
```

---

### 步骤四：拷贝ABI文件

```bash
# 拷贝ABI文件
docker cp fisco-console:/data/contracts/*.abi \
  /home/llm_rca/fisco/supply-chain-finance/console/contracts/

# ★ 修正：修改属主
sudo chown -R $(whoami):$(whoami) \
  /home/llm_rca/fisco/supply-chain-finance/console/contracts/
```

ABI文件用途：Java SDK与区块链交互时解码/编码交易数据

---

### 步骤五：更新.env配置

#### 5.1 手动更新

将部署的合约地址更新到 `.env` 文件：

```bash
vi /home/llm_rca/fisco/my-bcos-app/.env
```

格式：

```bash
CONTRACT_LIB_BYTES=0x19a6434154de51c7a7406edf312f01527441b561
CONTRACT_LIB_STRING=0x745d4de0cf93b7d1db8dd8892daf05ac745766ce
CONTRACT_ENTERPRISE=0x7a9b6d564d5d191093a29b7c760dd6af931cae73
CONTRACT_ENTERPRISE_AUTH=0xc860ab27901b3c2b810165a6096c64d88763617f
# ... 其他合约
```

#### 5.2 自动提取地址脚本

```bash
# 从部署日志提取最新地址
extract_addresses() {
    local log_file="/home/llm_rca/fisco/my-bcos-app/console/deploylog.txt"
    echo "CONTRACT_LIB_BYTES=$(grep 'LibBytes' "$log_file" | tail -1 | awk '{print $NF}')"
    echo "CONTRACT_LIB_STRING=$(grep 'LibString' "$log_file" | tail -1 | awk '{print $NF}')"
    echo "CONTRACT_ENTERPRISE=$(grep 'EnterpriseRegistryV2' "$log_file" | tail -1 | awk '{print $NF}')"
    # ... 其他合约
}

extract_addresses >> /home/llm_rca/fisco/my-bcos-app/.env
```

---

### 步骤六：重启服务

```bash
# 重启网关服务
cd /home/llm_rca/fisco/supply-chain-finance
docker compose restart fisco-gateway-service

# 检查启动日志
docker logs fisco-gateway-service --tail 100 | grep -E "contract|加载|地址|ERROR"
```

**验证成功标志**：

```
SDK账户地址: 0x6976c14175ddfae575ffab3845d89e155e09569d
企业合约加载成功，地址: 0x7a9b6d564d5d191093a29b7c760dd6af931cae73
```

---

## 常见错误与解决方案

| 错误现象              | 原因                         | 解决方案                                                            |
| --------------------- | ---------------------------- | ------------------------------------------------------------------- |
| `Permission Denied` | 部署账户与网关账户不一致     | 检查 `config.toml` 的 `accountAddress` 与控制台部署账户是否一致 |
| ABI嵌套目录           | 拷贝命令少了 `/.`          | 使用 `docker cp src/. dest` 格式                                  |
| 文件只读              | docker cp 产生 root 文件     | 执行 `sudo chown -R $(whoami):$(whoami) ...`                      |
| Java编译报错          | 包名路径与 `-p` 参数不匹配 | 检查目标目录与 `-p com.fisco.app.contract` 是否对应               |
| 库地址错误            | 用了旧版库地址               | 重新部署库合约，获取新地址后更新 `-l` 参数                        |
| 无法连接节点          | 节点未启动或端口错误         | 检查 fisco-node0~3 容器状态和 20000 端口                            |

---

## 合约部署清单

### 部署前

- [ ] 确认 5 个 Docker 容器运行正常
- [ ] 确认算法类型（ECDSA/国密）
- [ ] 确认管理员账户地址（控制台 vs 网关配置一致）
- [ ] 备份当前 .env 配置

### 部署中

- [ ] 编译所有合约（ABI + BIN）
- [ ] 按依赖顺序部署（库合约 → 主合约）
- [ ] 记录每个合约的部署地址

### 部署后

- [ ] 生成 Java 绑定类
- [ ] 使用 `/.` 格式拷贝到项目源码目录
- [ ] 执行 `chown` 修改属主
- [ ] 拷贝 ABI 文件
- [ ] 更新 .env 配置文件
- [ ] 重启 fisco-gateway-service
- [ ] 验证启动日志确认合约加载成功

---

## 关键路径汇总

| 产物     | 容器内路径                    | 宿主机路径                                        |
| -------- | ----------------------------- | ------------------------------------------------- |
| 合约源码 | `/data/contracts/solidity/` | `contracts/*/`                                  |
| ABI文件  | `/data/contracts/*.abi`     | `console/contracts/`                            |
| Java类   | `/data/contracts/sdk/java/` | `services/fisco-gateway-service/src/main/java/` |
| 部署日志 | `/data/deploylog.txt`       | `console/deploylog.txt`                         |
| SDK配置  | `/data/contracts/sdk/`      | `fisco/nodes/127.0.0.1/sdk/`                    |

---

*文档版本：v1.0*
*最后更新：2026-04-25*
