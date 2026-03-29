# FISCO BCOS 供应链金融平台 API 文档

> **生成方式**: 运行 `scripts/generate-api-docs.sh` 自动从各服务获取 OpenAPI 规范并生成文档
>
> **在线文档**: 各服务启动后访问 `http://localhost:{port}/swagger-ui.html`

## 服务端口映射

| 服务 | 端口 | API 规范地址 | 说明 |
|------|------|-------------|------|
| auth-service | 8081 | `/v3/api-docs` | 用户认证、注册、Token 管理 |
| enterprise-service | 8082 | `/v3/api-docs` | 企业管理、区块链上链 |
| warehouse-service | 8083 | `/v3/api-docs` | 仓单全生命周期管理 |
| logistics-service | 8084 | `/v3/api-docs` | 物流委派单、追踪 |
| finance-service | 8085 | `/v3/api-docs` | 贷款、应收账款 |
| credit-service | 8086 | `/v3/api-docs` | 企业信用评分、档案 |
| fisco-gateway-service | 8087 | `/v3/api-docs` | 区块链网关、合约调用 |

## 认证说明

所有接口（除 `/health` 外）均需要 JWT Bearer Token 认证。

### 请求头格式
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 认证流程
1. 调用 `POST /api/v1/auth/login` 获取 Access Token 和 Refresh Token
2. 在所有请求的 Header 中携带 Access Token
3. Token 过期后调用 `POST /api/v1/auth/refresh` 刷新

## 通用响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误响应
```json
{
  "code": 401,
  "message": "未授权或 Token 已过期",
  "data": null
}
```

## 服务 API 详情

### 1. 用户认证服务 (auth-service:8081)

#### 登录
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

#### 管理员登录
```
POST /api/v1/auth/admin/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

#### 刷新 Token
```
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "string"
}
```

#### 验证 Token
```
GET /api/v1/auth/validate
Authorization: Bearer {token}
```

#### 查询登录状态
```
GET /api/v1/auth/login/status
Authorization: Bearer {token}
```

---

### 2. 企业管理服务 (enterprise-service:8082)

#### 企业注册
```
POST /api/v1/enterprise/register
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseName": "string",
  "creditCode": "string",
  "role": 0,
  "password": "string",
  "invitationCode": "string"
}
```

#### 企业登录
```
POST /api/v1/enterprise/login
Content-Type: application/json

{
  "creditCode": "string",
  "password": "string"
}
```

#### 查询企业详情
```
GET /api/v1/enterprise/detail/{enterpriseId}
Authorization: Bearer {token}
```

#### 更新企业状态
```
PUT /api/v1/enterprise/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseId": "long",
  "newStatus": 1
}
```

#### 生成邀请码
```
POST /api/v1/enterprise/invitation-code
Authorization: Bearer {token}
```

#### 企业区块链上链
```
POST /api/v1/enterprise/register/blockchain
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseAddress": "string",
  "creditCode": "string",
  "role": 0,
  "metadataHash": "string"
}
```

---

### 3. 仓单管理服务 (warehouse-service:8083)

#### 创建仓单
```
POST /api/v1/warehouse/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptNo": "string",
  "warehouseId": "long",
  "ownerName": "string",
  "goodsType": "string",
  "weight": 1000.0,
  "unit": "kg",
  "quantity": 100
}
```

#### 查询仓单详情
```
GET /api/v1/warehouse/detail/{receiptId}
Authorization: Bearer {token}
```

#### 仓单背书转让
```
POST /api/v1/warehouse/endorse
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "long",
  "toEnterpriseId": "long"
}
```

#### 仓单质押
```
POST /api/v1/warehouse/pledge
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "long",
  "loanId": "long",
  "pledgeAmount": 800000.0
}
```

#### 仓单解锁
```
POST /api/v1/warehouse/unlock
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "long"
}
```

#### 仓单核销
```
POST /api/v1/warehouse/burn
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "long",
  "reason": "string"
}
```

---

### 4. 物流管理服务 (logistics-service:8084)

#### 创建物流委托单
```
POST /api/v1/logistics/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "receiptId": "long",
  "carrierId": "long",
  "sourceWarehouseId": "long",
  "targetWarehouseId": "long",
  "transportQuantity": 1000.0,
  "unit": "kg",
  "validUntil": 1714118400000
}
```

#### 确认提货
```
POST /api/v1/logistics/pickup
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "quantity": 1000.0
}
```

#### 确认到达
```
POST /api/v1/logistics/arrive
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "targetReceiptId": "long",
  "quantity": 1000.0
}
```

#### 确认交付
```
POST /api/v1/logistics/deliver
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "action": 1
}
```

#### 查询物流轨迹
```
GET /api/v1/logistics/track/{voucherNo}
Authorization: Bearer {token}
```

---

### 5. 金融管理服务 (finance-service:8085)

#### 申请贷款
```
POST /api/v1/loan/apply
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseId": "long",
  "receiptId": "long",
  "applyAmount": 1000000.0,
  "loanDays": 30,
  "purpose": "string"
}
```

#### 审批贷款
```
POST /api/v1/loan/approve
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanId": "long",
  "approvedAmount": 1000000.0,
  "interestRate": 0.05,
  "loanDays": 30
}
```

#### 放款
```
POST /api/v1/loan/disburse
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanId": "long"
}
```

#### 还款
```
POST /api/v1/loan/repay
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanId": "long",
  "amount": 100000.0,
  "installmentIndex": 0
}
```

#### 创建应收账款
```
POST /api/v1/receivable/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseId": "long",
  "buyerEnterpriseId": "long",
  "invoiceNo": "string",
  "amount": 1000000.0,
  "dueDate": 1714118400000,
  "businessScene": 0
}
```

#### 应收账款融资
```
POST /api/v1/receivable/finance
Authorization: Bearer {token}
Content-Type: application/json

{
  "receivableId": "long",
  "financeAmount": 800000.0,
  "financeInstitution": "string"
}
```

---

### 6. 信用评估服务 (credit-service:8086)

#### 查询企业信用档案
```
GET /api/v1/credit/profile/{enterpriseId}
Authorization: Bearer {token}
```

#### 获取信用评分
```
GET /api/v1/credit/score/{enterpriseId}
Authorization: Bearer {token}
```

#### 检查信用额度
```
POST /api/v1/credit/check-limit
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseId": "long",
  "requiredAmount": 1000000.0
}
```

#### 上报信用事件
```
POST /api/v1/credit/event/report
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseId": "long",
  "eventType": "string",
  "eventAmount": 100000.0,
  "description": "string"
}
```

---

### 7. FISCO 区块链网关服务 (fisco-gateway-service:8087)

#### 区块链基础操作

##### 查询区块信息
```
GET /api/v1/blockchain/block/{blockNumber}
Authorization: Bearer {token}
```

##### 查询交易信息
```
GET /api/v1/blockchain/transaction/{txHash}
Authorization: Bearer {token}
```

##### 调用合约（只读）
```
POST /api/v1/blockchain/call
Authorization: Bearer {token}
Content-Type: application/json

{
  "contractName": "string",
  "contractAddress": "string",
  "method": "string",
  "params": []
}
```

##### 发送交易（写操作）
```
POST /api/v1/blockchain/sendTransaction
Authorization: Bearer {token}
Content-Type: application/json

{
  "contractName": "string",
  "contractAddress": "string",
  "method": "string",
  "params": []
}
```

#### 企业上链操作

##### 注册企业
```
POST /api/v1/blockchain/enterprise/register
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseAddress": "0x...",
  "creditCode": "string",
  "role": 0,
  "metadataHash": "0x..."
}
```

##### 更新企业状态
```
POST /api/v1/blockchain/enterprise/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "enterpriseAddress": "0x...",
  "newStatus": 1
}
```

#### 仓单上链操作

##### 开立仓单
```
POST /api/v1/blockchain/receipt/issue
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "string",
  "ownerHash": "0x...",
  "warehouseHash": "0x...",
  "goodsDetailHash": "0x...",
  "weight": 1000,
  "unit": "kg",
  "quantity": 100,
  "storageDate": 1711526400000,
  "expiryDate": 1714118400000
}
```

##### 仓单背书
```
POST /api/v1/blockchain/receipt/endorse
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "string",
  "fromHash": "0x...",
  "toHash": "0x..."
}
```

##### 仓单拆分
```
POST /api/v1/blockchain/receipt/split
Authorization: Bearer {token}
Content-Type: application/json

{
  "originalReceiptId": "string",
  "newReceiptIds": ["string1", "string2"],
  "weights": [500, 500],
  "ownerHashes": ["0x...", "0x..."],
  "unit": "kg"
}
```

##### 仓单合并
```
POST /api/v1/blockchain/receipt/merge
Authorization: Bearer {token}
Content-Type: application/json

{
  "sourceReceiptIds": ["string1", "string2"],
  "targetReceiptId": "string",
  "targetOwnerHash": "0x...",
  "unit": "kg",
  "totalWeight": 1000
}
```

##### 仓单注销
```
POST /api/v1/blockchain/receipt/burn
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiptId": "string",
  "signatureHash": "0x..."
}
```

#### 物流上链操作

##### 创建物流委托单
```
POST /api/v1/blockchain/logistics/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "businessScene": 0,
  "receiptId": "string",
  "transportQuantity": 1000,
  "unit": "kg",
  "ownerHash": "0x...",
  "carrierHash": "0x...",
  "sourceWhHash": "0x...",
  "targetWhHash": "0x...",
  "validUntil": 1714118400000
}
```

##### 确认提货
```
POST /api/v1/blockchain/logistics/pickup
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "quantity": 1000
}
```

##### 确认到达
```
POST /api/v1/blockchain/logistics/arrive
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherNo": "string",
  "targetReceiptId": "string",
  "quantity": 1000
}
```

#### 贷款上链操作

##### 创建贷款
```
POST /api/v1/blockchain/loan/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanNo": "string",
  "borrowerHash": "0x...",
  "amount": 1000000,
  "loanDays": 30,
  "receiptId": "string",
  "pledgeAmount": 800000
}
```

##### 审批贷款
```
POST /api/v1/blockchain/loan/approve
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanNo": "string",
  "approvedAmount": 1000000,
  "interestRate": 0.05,
  "loanDays": 30
}
```

##### 放款
```
POST /api/v1/blockchain/loan/disburse
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanNo": "string",
  "receiptId": "string"
}
```

##### 还款
```
POST /api/v1/blockchain/loan/repay
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanNo": "string",
  "amount": 100000,
  "installmentIndex": 0
}
```

##### 标记逾期
```
POST /api/v1/blockchain/loan/overdue
Authorization: Bearer {token}
Content-Type: application/json

{
  "loanNo": "string",
  "overdueDays": 7,
  "penaltyRate": 0.001,
  "penaltyAmount": 1000
}
```

#### 应收账款上链操作

##### 创建应收账款
```
POST /api/v1/blockchain/receivable/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "receivableId": "string",
  "initialAmount": 1000000,
  "dueDate": 1714118400000,
  "buyerSellerPairHash": "0x...",
  "invoiceHash": "0x...",
  "contractHash": "0x...",
  "goodsDetailHash": "0x...",
  "businessScene": 0
}
```

##### 确认应收账款
```
POST /api/v1/blockchain/receivable/confirm
Authorization: Bearer {token}
Content-Type: application/json

{
  "receivableId": "string",
  "signature": "0x..."
}
```

##### 应收账款融资
```
POST /api/v1/blockchain/receivable/finance
Authorization: Bearer {token}
Content-Type: application/json

{
  "receivableId": "string",
  "financeAmount": 800000,
  "financeEntity": "0x..."
}
```

##### 债务抵消
```
POST /api/v1/blockchain/receivable/offset
Authorization: Bearer {token}
Content-Type: application/json

{
  "receivableId": "string",
  "receiptId": "string",
  "offsetAmount": 500000,
  "signatureHash": "0x..."
}
```

---

## 枚举值说明

### 企业角色 (EnterpriseRole)
| 值 | 说明 |
|----|------|
| 0 | 核心企业 |
| 1 | 供应商 |
| 2 | 经销商 |

### 企业状态 (EnterpriseStatus)
| 值 | 说明 |
|----|------|
| 0 | 待审核 |
| 1 | 正常 |
| 2 | 冻结 |
| 3 | 注销 |

### 仓单状态 (ReceiptStatus)
| 值 | 说明 |
|----|------|
| 0 | 草稿 |
| 1 | 已签发 |
| 2 | 已质押 |
| 3 | 已解锁 |
| 4 | 已核销 |

### 物流状态 (LogisticsStatus)
| 值 | 说明 |
|----|------|
| 0 | 已创建 |
| 1 | 已提货 |
| 2 | 运输中 |
| 3 | 已到达 |
| 4 | 已交付 |
| 5 | 已作废 |

### 贷款状态 (LoanStatus)
| 值 | 说明 |
|----|------|
| 0 | 待审批 |
| 1 | 审批通过 |
| 2 | 审批拒绝 |
| 3 | 已放款 |
| 4 | 已还款 |
| 5 | 逾期 |
| 6 | 坏账 |

### 应收账款状态 (ReceivableStatus)
| 值 | 说明 |
|----|------|
| 0 | 待确认 |
| 1 | 已确认 |
| 2 | 融资中 |
| 3 | 已结清 |
| 4 | 逾期 |

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或 Token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 企业不存在 |
| 1002 | 企业状态不允许此操作 |
| 2001 | 仓单不存在 |
| 2002 | 仓单状态不允许此操作 |
| 3001 | 贷款不存在 |
| 3002 | 贷款状态不允许此操作 |
| 4001 | 应收账款不存在 |
| 5001 | 区块链交易失败 |
