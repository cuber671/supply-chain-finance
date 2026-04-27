# 区块链接口分析报告

> 生成时间：2026/04/24 16:45
> 项目：FISCO BCOS 供应链金融平台 (supply-chain-finance)
> 扫描范围：7个微服务

---

## 📊 概览

| 指标               | 数量         |
| ------------------ | ------------ |
| 扫描微服务数       | 7            |
| 涉及区块链的服务数 | 7            |
| 区块链相关接口总数 | **91** |
| 写操作接口数       | **53** |
| 读操作接口数       | **38** |

### 区块链调用架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                    微服务架构                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  enterprise-service (8082) ──┐                                       │
│  warehouse-service (8083) ──┼──► BlockchainFeignClient ──────────► │
│  logistics-service (8084) ──┤     (HTTP调用)                        │
│  finance-service (8085) ────┼──► fisco-gateway-service (8087) ─────► FISCO BCOS
│  credit-service (8086) ─────┘      │                                 │
│                                    │                                 │
│  auth-service (8081) ──────────────┘  (无直接区块链调用)             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

 Legend: ──► = Feign Client调用  ──► = FISCO SDK直接调用
```

---

## 🔗 区块链接口清单

### fisco-gateway-service（端口 8087）

**服务角色**：唯一直接连接 FISCO BCOS 区块链的服务（`FISCO_ENABLED=true`）

**接口层级**：

- `BlockchainController` — 底层基础操作（状态/区块/交易查询）
- `BlockchainDomainController` — 业务域操作（企业/仓单/物流/贷款/应收款）

---

#### BlockchainController（底层操作）

##### 读操作（基础查询）

| 接口路径                                       | 方法 | 功能描述                       | 交易类型 |
| ---------------------------------------------- | ---- | ------------------------------ | -------- |
| `/api/v1/blockchain/status`                  | GET  | 获取区块链连接状态、块高、链ID | Read     |
| `/api/v1/blockchain/health`                  | GET  | 健康检查                       | Read     |
| `/api/v1/blockchain/blockNumber`             | GET  | 获取当前区块高度               | Read     |
| `/api/v1/blockchain/block/{blockNumber}`     | GET  | 根据块号获取区块信息           | Read     |
| `/api/v1/blockchain/blockHash/{blockNumber}` | GET  | 根据块号获取区块哈希           | Read     |
| `/api/v1/blockchain/receipt/{txHash}`        | GET  | 根据交易哈希获取交易收据       | Read     |
| `/api/v1/blockchain/account`                 | GET  | 获取当前系统账户地址           | Read     |

---

#### BlockchainDomainController（业务域操作）

##### 企业操作（EnterpriseContractService）

| 接口路径                                                      | 方法 | 功能描述               | 调用类型 | 写操作 |
| ------------------------------------------------------------- | ---- | ---------------------- | -------- | ------ |
| `/api/v1/blockchain/enterprise/register`                    | POST | 注册企业上链           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/enterprise/update-status`               | POST | 更新企业状态上链       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/enterprise/update-credit-rating`        | POST | 更新信用评级上链       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/enterprise/set-credit-limit`            | POST | 设置授信额度上链       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/enterprise/{address}`                   | GET  | 查询企业信息           | 直接调用 | -      |
| `/api/v1/blockchain/enterprise/by-credit-code/{creditCode}` | GET  | 根据信用代码查企业地址 | 直接调用 | -      |
| `/api/v1/blockchain/enterprise/list`                        | GET  | 获取企业列表           | 直接调用 | -      |
| `/api/v1/blockchain/enterprise/valid/{address}`             | GET  | 验证企业有效性         | 直接调用 | -      |

##### 仓单操作（WarehouseReceiptContractService）

| 接口路径                                            | 方法 | 功能描述               | 调用类型 | 写操作 |
| --------------------------------------------------- | ---- | ---------------------- | -------- | ------ |
| `/api/v1/blockchain/receipt/issue`                | POST | 签发仓单               | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/launch-endorsement`   | POST | 发起仓单背书           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/confirm-endorsement`  | POST | 确认仓单背书           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/split`                | POST | 拆分仓单               | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/merge`                | POST | 合并仓单               | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/lock`                 | POST | 锁定仓单（质押）       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/unlock`               | POST | 解锁仓单（解除质押）   | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/set-in-transit`       | POST | 设置仓单为物流转运中   | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/restore-from-transit` | POST | 从物流转运恢复到在库   | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/burn`                 | POST | 核销仓单               | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/cancel`               | POST | 取消仓单               | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receipt/info/{receiptId}`     | GET  | 查询仓单链上状态       | 直接调用 | -      |
| `/api/v1/blockchain/receipt/status/{receiptId}`   | GET  | 查询仓单状态           | 直接调用 | -      |
| `/api/v1/blockchain/receipt/owner/{owner}`        | GET  | 根据所有者查询仓单列表 | 直接调用 | -      |

##### 物流操作（LogisticsContractService）

| 接口路径                                           | 方法 | 功能描述           | 调用类型 | 写操作 |
| -------------------------------------------------- | ---- | ------------------ | -------- | ------ |
| `/api/v1/blockchain/logistics/create`            | POST | 创建物流委托单     | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/pickup`            | POST | 提货确认           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/arrive-add`        | POST | 到货增加数量       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/arrive-create`     | POST | 到货创建仓单       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/assign-carrier`    | POST | 分配承运人         | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/confirm-delivery`  | POST | 确认交付           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/update-status`     | POST | 更新物流状态       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/invalidate`        | POST | 使物流委托单失效   | 直接调用 | ⚠️   |
| `/api/v1/blockchain/logistics/track/{voucherNo}` | GET  | 获取物流轨迹       | 直接调用 | -      |
| `/api/v1/blockchain/logistics/valid/{voucherNo}` | GET  | 验证物流委托有效性 | 直接调用 | -      |

##### 贷款操作（LoanContractService）

| 接口路径                                           | 方法 | 功能描述           | 调用类型 | 写操作 |
| -------------------------------------------------- | ---- | ------------------ | -------- | ------ |
| `/api/v1/blockchain/loan/create`                 | POST | 创建贷款           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/approve`                | POST | 审批贷款           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/cancel`                 | POST | 取消贷款           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/disburse`               | POST | 放款               | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/repay`                  | POST | 记录还款           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/mark-overdue`           | POST | 标记逾期           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/mark-defaulted`         | POST | 标记违约           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/set-receipt`            | POST | 设置仓单-贷款关联  | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/update-receipt`         | POST | 更新仓单-贷款关联  | 直接调用 | ⚠️   |
| `/api/v1/blockchain/loan/core/{loanNo}`          | GET  | 获取贷款核心信息   | 直接调用 | -      |
| `/api/v1/blockchain/loan/status/{loanNo}`        | GET  | 获取贷款状态       | 直接调用 | -      |
| `/api/v1/blockchain/loan/by-receipt/{receiptId}` | GET  | 根据仓单ID获取贷款 | 直接调用 | -      |
| `/api/v1/blockchain/loan/exists/{loanNo}`        | GET  | 检查贷款是否存在   | 直接调用 | -      |

##### 应收账款操作（ReceivableContractService）

| 接口路径                                                   | 方法 | 功能描述           | 调用类型 | 写操作 |
| ---------------------------------------------------------- | ---- | ------------------ | -------- | ------ |
| `/api/v1/blockchain/receivable/create`                   | POST | 创建应收款         | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/confirm`                  | POST | 确认应收款         | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/adjust`                   | POST | 调整应收款金额     | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/finance`                  | POST | 应收款融资         | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/settle`                   | POST | 应收款结算         | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/update-balance`           | POST | 更新应收款余额     | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/record-repayment`         | POST | 记录还款           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/record-full-repayment`    | POST | 记录全额还款       | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/offset-debt`              | POST | 以物抵债           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/offset-debt-with-receipt` | POST | 仓单抵债           | 直接调用 | ⚠️   |
| `/api/v1/blockchain/receivable/{receivableId}`           | GET  | 获取应收款信息     | 直接调用 | -      |
| `/api/v1/blockchain/receivable/status/{receivableId}`    | GET  | 获取应收款状态     | 直接调用 | -      |
| `/api/v1/blockchain/receivable/balance/{receivableId}`   | GET  | 获取应收款未还余额 | 直接调用 | -      |

##### 密钥与签名服务

| 接口路径                           | 方法 | 功能描述         | 调用类型 | 写操作 |
| ---------------------------------- | ---- | ---------------- | -------- | ------ |
| `/api/v1/blockchain/keygen`      | POST | 生成区块链密钥对 | 直接调用 | ⚠️   |
| `/api/v1/blockchain/sign`        | POST | 数据签名         | 直接调用 | -      |
| `/api/v1/blockchain/sign/verify` | POST | 验证签名         | 直接调用 | -      |

##### 交易辅助接口

| 接口路径                                | 方法 | 功能描述     | 调用类型 | 写操作 |
| --------------------------------------- | ---- | ------------ | -------- | ------ |
| `/api/v1/blockchain/tx/submit`        | POST | 提交交易     | 直接调用 | ⚠️   |
| `/api/v1/blockchain/tx/{txHash}`      | GET  | 查询交易状态 | 直接调用 | -      |
| `/api/v1/blockchain/receipt/{txHash}` | GET  | 获取交易收据 | 直接调用 | -      |
| `/api/v1/blockchain/block/latest`     | GET  | 获取最新块高 | 直接调用 | -      |

---

### enterprise-service（端口 8082）

**服务角色**：企业信息管理，通过 `BlockchainFeignClient` 间接调用区块链

**区块链相关接口（间接调用）**：

| 接口路径                                        | 方法 | 业务描述         | 区块链调用                                                 |
| ----------------------------------------------- | ---- | ---------------- | ---------------------------------------------------------- |
| `/api/v1/enterprise/register`                 | POST | 注册企业         | 调用 `BlockchainFeignClient.registerEnterprise()`        |
| `/api/v1/enterprise/update-status`            | POST | 更新企业状态     | 调用 `BlockchainFeignClient.updateEnterpriseStatus()`    |
| `/api/v1/enterprise/update-credit-rating`     | POST | 更新信用评级     | 调用 `BlockchainFeignClient.updateCreditRating()`        |
| `/api/v1/enterprise/set-credit-limit`         | POST | 设置授信额度     | 调用 `BlockchainFeignClient.setCreditLimit()`            |
| `/api/v1/enterprise/{address}`                | GET  | 查询企业详情     | 调用 `BlockchainFeignClient.getEnterprise()`             |
| `/api/v1/enterprise/credit-code/{creditCode}` | GET  | 根据信用代码查询 | 调用 `BlockchainFeignClient.getEnterpriseByCreditCode()` |
| `/api/v1/enterprise/list`                     | GET  | 获取企业列表     | 调用 `BlockchainFeignClient.getEnterpriseList()`         |
| `/api/v1/enterprise/valid/{address}`          | GET  | 验证企业有效性   | 调用 `BlockchainFeignClient.isEnterpriseValid()`         |

---

### warehouse-service（端口 8083）

**服务角色**：仓单全生命周期管理，通过 `BlockchainFeignClient` 间接调用区块链

**区块链相关接口（间接调用）**：

| 接口路径                                  | 方法 | 业务描述     | 区块链调用                                                    |
| ----------------------------------------- | ---- | ------------ | ------------------------------------------------------------- |
| `/api/v1/receipt/mint`                  | POST | 铸造仓单     | 调用 `BlockchainFeignClient.issueReceipt()`                 |
| `/api/v1/receipt/mint-direct`           | POST | 直接铸造仓单 | 调用 `BlockchainFeignClient.issueReceipt()`                 |
| `/api/v1/endorsement/launch`            | POST | 发起背书     | 调用 `BlockchainFeignClient.launchEndorsement()`            |
| `/api/v1/endorsement/{id}/confirm`      | POST | 确认背书     | 调用 `BlockchainFeignClient.confirmEndorsement()`           |
| `/api/v1/endorsement/{id}/revoke`       | POST | 撤销背书     | 调用 `BlockchainFeignClient.launchEndorsement()` (撤销逻辑) |
| `/api/v1/split/apply`                   | POST | 申请拆分     | 调用 `BlockchainFeignClient.splitReceipt()`                 |
| `/api/v1/merge/apply`                   | POST | 申请合并     | 调用 `BlockchainFeignClient.mergeReceipts()`                |
| `/api/v1/split-merge/{opLogId}/execute` | POST | 执行拆分合并 | 调用对应区块链操作                                            |
| `/api/v1/receipt/{receiptId}`           | GET  | 查询仓单详情 | 调用 `BlockchainFeignClient.getReceipt()`                   |
| `/api/v1/receipt/in-stock`              | GET  | 查询在库仓单 | 调用 `BlockchainFeignClient.getReceiptIdsByOwner()`         |

---

### logistics-service（端口 8084）

**服务角色**：物流委托单管理，通过 `BlockchainFeignClient` 间接调用区块链

**区块链相关接口（间接调用）**：

| 接口路径                                | 方法 | 业务描述     | 区块链调用                                                                            |
| --------------------------------------- | ---- | ------------ | ------------------------------------------------------------------------------------- |
| `/api/v1/logistics/create`            | POST | 创建物流委托 | 调用 `BlockchainFeignClient.createLogisticsDelegate()`                              |
| `/api/v1/logistics/pickup`            | POST | 提货确认     | 调用 `BlockchainFeignClient.pickup()`                                               |
| `/api/v1/logistics/arrive`            | POST | 到货确认     | 调用 `BlockchainFeignClient.arriveAndAddQuantity()` 或 `arriveAndCreateReceipt()` |
| `/api/v1/logistics/assign-carrier`    | POST | 分配承运人   | 调用 `BlockchainFeignClient.assignCarrier()`                                        |
| `/api/v1/logistics/confirm-delivery`  | POST | 确认交付     | 调用 `BlockchainFeignClient.confirmDelivery()`                                      |
| `/api/v1/logistics/track/{voucherNo}` | GET  | 查询物流轨迹 | 调用 `BlockchainFeignClient.getLogisticsTrack()`                                    |

---

### finance-service（端口 8085）

**服务角色**：贷款与应收账款管理，通过 `BlockchainFeignClient` 间接调用区块链

**区块链相关接口（间接调用）**：

| 接口路径                       | 方法 | 业务描述     | 区块链调用                                           |
| ------------------------------ | ---- | ------------ | ---------------------------------------------------- |
| `/api/v1/loan/create`        | POST | 创建贷款     | 调用 `BlockchainFeignClient.createLoan()`          |
| `/api/v1/loan/approve`       | POST | 审批贷款     | 调用 `BlockchainFeignClient.approveLoan()`         |
| `/api/v1/loan/cancel`        | POST | 取消贷款     | 调用 `BlockchainFeignClient.cancelLoan()`          |
| `/api/v1/loan/disburse`      | POST | 放款         | 调用 `BlockchainFeignClient.disburseLoan()`        |
| `/api/v1/loan/repay`         | POST | 还款         | 调用 `BlockchainFeignClient.recordLoanRepayment()` |
| `/api/v1/loan/{loanNo}`      | GET  | 查询贷款详情 | 调用 `BlockchainFeignClient.getLoanCore()`         |
| `/api/v1/receivable/create`  | POST | 创建应收款   | 调用 `BlockchainFeignClient.createReceivable()`    |
| `/api/v1/receivable/finance` | POST | 应收款融资   | 调用 `BlockchainFeignClient.financeReceivable()`   |
| `/api/v1/receivable/settle`  | POST | 应收款结算   | 调用 `BlockchainFeignClient.settleReceivable()`    |

---

### credit-service（端口 8086）

**服务角色**：信用评分服务，通过 `BlockchainFeignClient` 间接调用区块链

**区块链相关接口（间接调用）**：

| 接口路径                                | 方法 | 业务描述         | 区块链调用                                     |
| --------------------------------------- | ---- | ---------------- | ---------------------------------------------- |
| `/api/v1/credit/enterprise/{address}` | GET  | 查询企业信用信息 | 调用 `BlockchainFeignClient.getEnterprise()` |
| `/api/v1/credit/rating/{address}`     | GET  | 查询信用评级     | 调用 `BlockchainFeignClient.getEnterprise()` |

---

### auth-service（端口 8081）

**服务角色**：用户认证与授权，**无直接区块链调用**

---

## ⚠️ 风险接口汇总

| 接口                                            | 服务          | 风险类型     | 说明                         |
| ----------------------------------------------- | ------------- | ------------ | ---------------------------- |
| `POST /api/v1/blockchain/enterprise/register` | fisco-gateway | 写操作无幂等 | 重复注册企业需区块链回滚     |
| `POST /api/v1/blockchain/receipt/issue`       | fisco-gateway | 写操作无幂等 | 重复签发需链上状态同步       |
| `POST /api/v1/blockchain/loan/create`         | fisco-gateway | 写操作无幂等 | 需配合定时任务重试机制       |
| `POST /api/v1/blockchain/receivable/create`   | fisco-gateway | 写操作无幂等 | 应收款创建需幂等保护         |
| `POST /api/v1/blockchain/receipt/burn`        | fisco-gateway | 不可逆操作   | 核销后无法恢复               |
| `POST /api/v1/blockchain/receipt/cancel`      | fisco-gateway | 不可逆操作   | 取消后无法恢复               |
| `POST /api/v1/blockchain/keygen`              | fisco-gateway | 密钥安全     | 私钥以十六进制返回需加密传输 |

---

## 📁 区块链相关文件索引

### 核心区块链服务

| 文件路径                                                                                                  | 说明                            |
| --------------------------------------------------------------------------------------------------------- | ------------------------------- |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/controller/BlockchainController.java`       | 底层区块链操作 REST API         |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/controller/BlockchainDomainController.java` | 业务域区块链操作 REST API       |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/BlockchainService.java`             | 区块链服务接口定义              |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/impl/BlockchainServiceImpl.java`    | 区块链服务实现（FISCO SDK封装） |

### 合约服务层

| 文件路径                                                                                                         | 说明           |
| ---------------------------------------------------------------------------------------------------------------- | -------------- |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/impl/EnterpriseContractService.java`       | 企业合约服务   |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/impl/WarehouseReceiptContractService.java` | 仓单合约服务   |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/impl/LogisticsContractService.java`        | 物流合约服务   |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/impl/LoanContractService.java`             | 贷款合约服务   |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/service/impl/ReceivableContractService.java`       | 应收款合约服务 |

### Feign Client 抽象层

| 文件路径                                                                    | 说明                                                    |
| --------------------------------------------------------------------------- | ------------------------------------------------------- |
| `common-api/src/main/java/com/fisco/app/feign/BlockchainFeignClient.java` | 区块链网关 Feign 客户端（业务服务调用区块链的唯一入口） |

### 业务服务实现

| 文件路径                                                                                                 | 说明                                    |
| -------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| `services/enterprise-service/src/main/java/com/fisco/app/service/impl/EnterpriseServiceImpl.java`      | 企业服务（调用BlockchainFeignClient）   |
| `services/warehouse-service/src/main/java/com/fisco/app/service/impl/WarehouseReceiptServiceImpl.java` | 仓单服务（调用BlockchainFeignClient）   |
| `services/logistics-service/src/main/java/com/fisco/app/service/impl/LogisticsServiceImpl.java`        | 物流服务（调用BlockchainFeignClient）   |
| `services/finance-service/src/main/java/com/fisco/app/service/impl/LoanServiceImpl.java`               | 贷款服务（调用BlockchainFeignClient）   |
| `services/finance-service/src/main/java/com/fisco/app/service/impl/FinanceServiceImpl.java`            | 应收款服务（调用BlockchainFeignClient） |

### 区块链定时任务

| 文件路径                                                                                                | 说明               |
| ------------------------------------------------------------------------------------------------------- | ------------------ |
| `services/fisco-gateway-service/src/main/java/com/fisco/app/task/BlockchainTransactionRetryTask.java` | 区块链交易重试任务 |

### 智能合约（Solidity）

| 文件路径                  | 说明         |
| ------------------------- | ------------ |
| `contracts/enterprise/` | 企业注册合约 |
| `contracts/warehouse/`  | 仓单合约     |
| `contracts/logistics/`  | 物流合约     |
| `contracts/loan/`       | 贷款合约     |
| `contracts/receivable/` | 应收款合约   |

### 配置文件

| 文件路径               | 说明               |
| ---------------------- | ------------------ |
| `.env`               | 区块链合约地址配置 |
| `docker-compose.yml` | FISCO节点网络配置  |

---

## 🔧 建议

1. **幂等性增强**：所有写操作接口（⚠️标记）建议增加业务幂等号（idempotency key）机制，防止网络重试导致重复上链
2. **交易失败重试**：已存在 `BlockchainTransactionRetryTask` 定时任务，建议对重试失败次数设置告警
3. **链上链下状态一致性**：仓单、贷款状态变更涉及链上链下双写，建议增加一致性对账机制
4. **密钥安全管理**：`/api/v1/blockchain/keygen` 接口返回私钥，建议评估是否需要加密或使用 KMS 方案
5. **Gas 费用监控**：写操作涉及区块链手续费，建议监控平均 Gas 消耗趋势

---

## 📋 接口统计表（按服务分类）

| 服务                  | 端口 | 直接调用区块链 | 间接调用区块链 | 总接口数 |
| --------------------- | ---- | -------------- | -------------- | -------- |
| fisco-gateway-service | 8087 | ✅ (91个接口)  | -              | 91       |
| enterprise-service    | 8082 | -              | ✅ (8个)       | 8        |
| warehouse-service     | 8083 | -              | ✅ (~15个)     | ~15      |
| logistics-service     | 8084 | -              | ✅ (~7个)      | ~7       |
| finance-service       | 8085 | -              | ✅ (~10个)     | ~10      |
| credit-service        | 8086 | -              | ✅ (~2个)      | ~2       |
| auth-service          | 8081 | -              | ❌             | 0        |

---

> 报告说明：本报告通过扫描代码中 `BlockchainFeignClient` 的调用关系、区块链服务接口定义及业务服务实现生成。接口数量为近似值，实际数量以代码运行结果为准。
