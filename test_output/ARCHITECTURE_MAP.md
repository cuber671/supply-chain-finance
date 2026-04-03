# 架构地图报告

## 服务拓扑

| 服务名 | 端口 | IP地址 | 数据库 | 角色 |
|--------|------|--------|--------|------|
| auth-service | 8081 | 172.26.0.10 | fisco_data@172.26.0.100:3306 | 用户认证、Flyway Owner |
| enterprise-service | 8082 | 172.26.0.11 | fisco_data@172.26.0.100:3306 | 企业管理 |
| warehouse-service | 8083 | 172.26.0.12 | fisco_data@172.26.0.100:3306 | 仓单管理 |
| logistics-service | 8084 | 172.26.0.13 | fisco_data@172.26.0.100:3306 | 物流管理 |
| finance-service | 8085 | 172.26.0.14 | fisco_data@172.26.0.100:3306 | 金融（贷款、应收款） |
| credit-service | 8086 | 172.26.0.15 | fisco_data@172.26.0.100:3306 | 信用管理 |
| fisco-gateway-service | 8087 | 172.26.0.16 | fisco_data@172.26.0.100:3306 | 区块链网关（唯一FISCO SDK） |
| nginx | 80/443 | 172.26.0.2 | - | API网关 |
| mysql | 3306 | 172.26.0.100 | fisco_data | MySQL 8.0.36 |
| fisco-node[0-3] | 20000-20003 | 172.26.0.20-23 | - | FISCO BCOS 3.12.1 |

**FISCO配置**: 群组=group0, SDK配置=/app/sdk/config.toml

## 接口全量清单（按服务分组）

### AuthService (8081) — `/api/v1/auth`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 1 | POST | /login | 否 | 用户/企业登录（USER/ENTERPRISE双模式） |
| 2 | POST | /admin/login | 否 | 管理员登录 |
| 3 | POST | /refresh | 否 | 刷新Token |
| 4 | POST | /validate | 否 | 验证Token |
| 5 | POST | /login/status | 否 | 查询企业登录TCC事务状态 |

### UserController (8081) — `/api/v1/auth/users`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 6 | POST | /register | 否 | 用户注册 |
| 7 | GET | /{userId} | 是 | 查询用户详情 |
| 8 | PUT | /{userId}/status | 是 | 变更用户状态 |
| 9 | PUT | /{userId}/role | 是 | 变更用户角色 |
| 10 | GET | /profile | 是 | 获取当前用户资料 |
| 11 | PUT | /update | 是 | 修改个人资料 |
| 12 | POST | /password | 是 | 修改密码 |
| 13 | GET | /list | 是 | 分页查询企业员工列表 |
| 14 | GET | /pending | 是 | 查询待审核用户列表 |
| 15 | POST | /{userId}/audit | 是 | 审核用户注册申请 |
| 16 | POST | /cancel/apply | 是 | 申请注销账号 |
| 17 | POST | /cancel/revoke | 是 | 撤回注销申请 |
| 18 | GET | /cancel/pending | 是 | 查询待审核注销用户列表 |
| 19 | POST | /{userId}/cancel/audit | 是 | 审核注销申请 |
| 20 | PUT | /disable/{userId} | 是 | 强制禁用用户 |
| 21 | DELETE | /{userId} | 是 | 删除用户 |

### EnterpriseController (8082) — `/api/v1/enterprise`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 22 | POST | /register | 否 | 注册企业 |
| 23 | GET | /{entId} | 是 | 根据ID获取企业信息 |
| 24 | GET | /list | 是 | 获取企业列表（分页） |
| 25 | GET | /pending | 是 | 获取待审核企业列表 |
| 26 | PUT | /{entId}/status | 是 | 更新企业状态 |
| 27 | POST | /{entId}/audit | 是 | 审核企业申请 |
| 28 | POST | /login | 否 | 企业登录 |
| 29 | PUT | /password/login | 是 | 修改登录密码 |
| 30 | PUT | /password/pay | 是 | 重置交易密码 |
| 31 | GET | /detail | 是 | 获取企业详情 |
| 32 | GET | /invite-codes | 是 | 生成邀请码 |
| 33 | GET | /invite-codes/list | 是 | 查询邀请码列表 |
| 34 | DELETE | /invite-codes/{codeId} | 是 | 删除邀请码 |
| 35 | POST | /cancellation/apply | 是 | 发起注销申请 |
| 36 | POST | /cancellation/revoke | 是 | 撤回注销申请 |
| 37 | GET | /cancellation/pending | 是 | 获取待审核注销企业列表 |
| 38 | POST | /{entId}/cancellation/audit | 是 | 审核企业注销申请 |
| 39 | POST | /cancellation/force | 是 | 管理员强制注销企业 |
| 40 | GET | /asset-balance | 是 | 查询企业资产余额 |
| 41 | POST | /invitation/validate | 否 | 校验邀请码有效性 |
| 42 | GET | /blockchain-address/{address} | 是 | 通过区块链地址查询企业 |
| 43 | GET | /check-financial-institution/{entId} | 否 | 验证企业是否为金融机构 |
| 44 | GET | /chain/code/{creditCode} | 是 | 通过信用代码查询链上企业地址 |
| 45 | GET | /chain/list | 是 | 查询链上企业列表 |
| 46 | PUT | /{entId}/rating | 是 | 更新企业信用评级上链 |
| 47 | PUT | /{entId}/credit-limit | 是 | 设置企业信用额度上链 |

### WarehouseReceiptController (8083) — `/api/v1/warehouse`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 48 | POST | /stock-in/apply | 是 | 申请入库 |
| 49 | POST | /stock-in/{stockOrderId}/confirm | 是 | 确认入库单（仓储方） |
| 50 | POST | /stock-in/{stockOrderId}/cancel | 是 | 取消入库单 |
| 51 | GET | /stock-in/{stockOrderIdOrNo} | 是 | 查询入库单 |
| 52 | GET | /stock-in/list | 是 | 查询企业入库单列表 |
| 53 | GET | /stock-in/list/paginated | 是 | 分页查询企业入库单列表 |
| 54 | POST | /receipt/mint | 是 | 签发仓单（仓储方） |
| 55 | GET | /receipt/{receiptId} | 是 | 根据ID查询仓单 |
| 56 | GET | /receipt/by-chain/{onChainId} | 是 | 根据链上ID查询仓单 |
| 57 | GET | /receipt/list | 是 | 查询企业仓单列表 |
| 58 | GET | /receipt/list/paginated | 是 | 分页查询企业仓单列表 |
| 59 | GET | /receipt/in-stock | 是 | 查询企业在库仓单 |
| 60 | GET | /receipt/in-stock/paginated | 是 | 分页查询企业在库仓单 |
| 61 | GET | /receipt/{receiptId}/validate-ownership | 是 | 校验仓单所有权 |
| 62 | POST | /endorsement/launch | 是 | 发起背书转让 |
| 63 | POST | /endorsement/{endorsementId}/confirm | 是 | 确认/拒绝背书转让 |
| 64 | POST | /endorsement/{endorsementId}/revoke | 是 | 撤回背书 |
| 65 | GET | /endorsement/list | 是 | 查询仓单背书记录 |
| 66 | POST | /split/apply | 是 | 发起拆分申请 |
| 67 | POST | /merge/apply | 是 | 发起合并申请 |
| 68 | POST | /split-merge/{opLogId}/execute | 是 | 执行/驳回拆分合并（仓储方） |
| 69 | GET | /split-merge/{opLogId} | 是 | 查询拆分合并记录 |
| 70 | POST | /split-merge/{opLogId}/cancel | 是 | 撤销拆分合并申请 |
| 71 | POST | /receipt/{receiptId}/lock | 是 | 质押锁定仓单（金融机构） |
| 72 | POST | /receipt/{receiptId}/unlock | 是 | 还款解押仓单（金融机构） |
| 73 | POST | /receipt/{receiptId}/force-unlock | 是 | 管理员强制解锁仓单 |
| 74 | POST | /receipt/{receiptId}/void | 是 | 作废仓单 |
| 75 | POST | /burn/apply | 是 | 申请核销出库 |
| 76 | POST | /burn/{stockOrderId}/confirm | 是 | 确认核销出库（仓储方） |
| 77 | POST | /warehouse/create | 是 | 创建仓库 |
| 78 | GET | /warehouse/list | 是 | 查询仓库列表 |
| 79 | GET | /warehouse/list/paginated | 是 | 分页查询仓库列表 |
| 80 | GET | /receipt/{receiptId}/trace | 是 | 全路径溯源查询 |

### LogisticsController (8084) — `/api/v1/logistics`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 81 | POST | /create | 是 | 创建物流委派单 |
| 82 | GET | /delegate/{voucherNo} | 是 | 查询委派单详情 |
| 83 | GET | /delegate/list | 是 | 查询企业委派单列表 |
| 84 | POST | /assign | 是 | 物流指派任务 |
| 85 | POST | /pickup | 是 | 仓库提货确认 |
| 86 | POST | /arrive | 是 | 到货入库申请 |
| 87 | GET | /track | 是 | 物流状态追踪 |
| 88 | GET | /track/list | 是 | 查询物流轨迹列表 |
| 89 | GET | /track/latest | 是 | 获取最新轨迹 |
| 90 | GET | /track/deviations | 是 | 查询偏航记录 |
| 91 | POST | /track/report | 是 | 上报物流轨迹 |
| 92 | PUT | /status | 是 | 更新物流状态 |
| 93 | POST | /delivery/confirm | 是 | 确认交付 |
| 94 | POST | /invalidate | 是 | 使委派单失效 |
| 95 | GET | /validate | 是 | 验证物流委派单 |

### FinanceController (8085) — `/api/v1/finance`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 96 | POST | /receivable/generate | 是 | 生成应收款 |
| 97 | POST | /receivable/confirm | 是 | 确认应收款 |
| 98 | PATCH | /receivable/adjust | 是 | 调整应收款金额 |
| 99 | GET | /receivable/{id} | 是 | 查询应收款详情 |
| 100 | GET | /receivable/no/{receivableNo} | 是 | 根据编号查询应收款 |
| 101 | GET | /receivable/creditor/list | 是 | 查询债权人的应收款列表 |
| 102 | GET | /receivable/debtor/list | 是 | 查询债务人的应收款列表 |
| 103 | GET | /receivable/ent/{entId} | 是 | 查询指定企业的应收款列表 |
| 104 | POST | /receivable/repayment/cash | 是 | 现金还款 |
| 105 | POST | /receivable/repayment/offset | 是 | 仓单抵债 |
| 106 | GET | /receivable/{id}/repayments | 是 | 查询还款记录 |
| 107 | POST | /receivable/finance | 是 | 应收款融资 |
| 108 | POST | /receivable/{id}/settle | 是 | 应收款结算 |

### LoanController (8085) — `/api/v1/finance/loan`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 109 | POST | /apply | 是 | 申请质押贷款 |
| 110 | GET | /list | 是 | 贷款列表 |
| 111 | GET | /{id} | 是 | 贷款详情 |
| 112 | POST | /{id}/approve | 是 | 审批通过 |
| 113 | POST | /{id}/reject | 是 | 审批拒绝 |
| 114 | POST | /{id}/cancel | 是 | 取消申请 |
| 115 | POST | /{id}/disburse | 是 | 放款 |
| 116 | POST | /{id}/repay | 是 | 提交还款 |
| 117 | GET | /{id}/repayments | 是 | 还款记录列表 |
| 118 | GET | /{id}/installments | 是 | 分期计划 |
| 119 | POST | /calculator | 是 | 贷款试算 |
| 120 | GET | /my | 是 | 我的贷款 |
| 121 | GET | /pending | 是 | 待审批列表 |

### CreditController (8086) — `/api/v1/credit`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 122 | GET | /profile | 是 | 获取企业信用画像 |
| 123 | GET | /profile/me | 是 | 获取当前企业信用画像 |
| 124 | GET | /score | 是 | 获取信用评分 |
| 125 | POST | /event/report | 是 | 上报信用事件 |
| 126 | POST | /event/logistics-deviation | 是 | 物流偏航触发信用扣分 |
| 127 | GET | /events | 是 | 查询企业信用事件列表 |
| 128 | PUT | /limit | 是 | 设置授信额度 |
| 129 | POST | /limit/check | 是 | 额度校验 |
| 130 | POST | /limit/lock | 是 | 额度实时锁死 |
| 131 | GET | /limit/available | 是 | 获取可用信用额度 |
| 132 | PATCH | /reevaluate | 是 | 信用等级重算 |
| 133 | PATCH | /reevaluate/batch | 是 | 批量信用等级重算 |
| 134 | GET | /blacklist/check | 是 | 检查是否触发信用黑名单 |
| 135 | POST | /blacklist/trigger | 是 | 触发信用黑名单 |
| 136 | DELETE | /blacklist/remove | 是 | 移除信用黑名单 |

### BlockchainController (8087) — `/api/v1/blockchain`

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 137 | GET | /status | 是 | 获取区块链状态 |
| 138 | GET | /health | 是 | 健康检查 |
| 139 | GET | /blockNumber | 是 | 获取当前块高 |
| 140 | GET | /block/{blockNumber} | 是 | 根据块号获取区块信息 |
| 141 | GET | /blockHash/{blockNumber} | 是 | 根据块号获取区块哈希 |
| 142 | GET | /receipt/{txHash} | 是 | 根据交易哈希获取交易收据 |
| 143 | GET | /account | 是 | 获取当前系统账户地址 |
| 144 | GET | /balance/{address} | 是 | 查询账户余额 |
| 145 | POST | /call | 是 | 调用合约只读方法 |
| 146 | POST | /transaction | 是 | 发送合约交易（ADMIN） |
| 147 | GET | /group | 是 | 获取群组信息 |
| 148 | GET | /groups | 是 | 获取群组列表 |

### BlockchainDomainController (8087) — `/api/v1/blockchain` (业务域操作)

| # | 方法 | 路径 | 认证 | 业务描述 |
|---|------|------|------|----------|
| 149 | POST | /enterprise/register | 是 | 注册企业上链 |
| 150 | POST | /enterprise/update-status | 是 | 更新企业状态上链 |
| 151 | POST | /enterprise/update-credit-rating | 是 | 更新企业信用评级上链 |
| 152 | POST | /enterprise/set-credit-limit | 是 | 设置企业授信额度上链 |
| 153 | GET | /enterprise/{address} | 是 | 获取企业信息（链上） |
| 154 | GET | /enterprise/by-credit-code/{creditCode} | 是 | 根据信用代码获取企业地址 |
| 155 | GET | /enterprise/list | 是 | 获取企业列表（链上） |
| 156 | GET | /enterprise/valid/{address} | 是 | 验证企业有效性 |
| 157 | POST | /receipt/issue | 是 | 签发仓单上链 |
| 158 | POST | /receipt/launch-endorsement | 是 | 发起仓单背书上链 |
| 159 | POST | /receipt/confirm-endorsement | 是 | 确认仓单背书上链 |
| 160 | POST | /receipt/split | 是 | 拆分仓单上链 |
| 161 | POST | /receipt/merge | 是 | 合并仓单上链 |
| 162 | POST | /receipt/lock | 是 | 锁定仓单上链 |
| 163 | POST | /receipt/unlock | 是 | 解锁仓单上链 |
| 164 | POST | /receipt/burn | 是 | 核销仓单上链 |
| 165 | POST | /logistics/create | 是 | 创建物流委托单上链 |
| 166 | POST | /logistics/pickup | 是 | 提货确认上链 |
| 167 | POST | /logistics/arrive-add | 是 | 到货增加数量上链 |
| 168 | POST | /logistics/assign-carrier | 是 | 分配承运人上链 |
| 169 | POST | /logistics/confirm-delivery | 是 | 确认交付上链 |
| 170 | POST | /logistics/update-status | 是 | 更新物流状态上链 |
| 171 | GET | /logistics/track/{voucherNo} | 是 | 获取物流轨迹 |
| 172 | GET | /logistics/valid/{voucherNo} | 是 | 验证物流委托 |
| 173 | POST | /logistics/invalidate | 是 | 使物流委托单失效 |
| 174 | POST | /loan/create | 是 | 创建贷款上链 |
| 175 | POST | /loan/approve | 是 | 审批贷款上链 |
| 176 | POST | /loan/cancel | 是 | 取消贷款上链 |
| 177 | POST | /loan/disburse | 是 | 放款上链 |
| 178 | POST | /loan/repay | 是 | 记录还款上链 |
| 179 | POST | /loan/mark-overdue | 是 | 标记逾期上链 |
| 180 | POST | /loan/mark-defaulted | 是 | 标记违约上链 |
| 181 | POST | /loan/set-receipt | 是 | 设置仓单-贷款关联上链 |
| 182 | POST | /loan/update-receipt | 是 | 更新仓单-贷款关联上链 |
| 183 | GET | /loan/core/{loanNo} | 是 | 获取贷款核心信息 |
| 184 | GET | /loan/status/{loanNo} | 是 | 获取贷款状态 |
| 185 | GET | /loan/by-receipt/{receiptId} | 是 | 获取仓单关联的贷款 |
| 186 | GET | /loan/exists/{loanNo} | 是 | 检查贷款是否存在 |
| 187 | POST | /receivable/create | 是 | 创建应收款上链 |
| 188 | POST | /receivable/confirm | 是 | 确认应收款上链 |
| 189 | POST | /receivable/adjust | 是 | 调整应收款上链 |
| 190 | POST | /receivable/finance | 是 | 应收款融资上链 |
| 191 | POST | /receivable/settle | 是 | 应收款结算上链 |
| 192 | GET | /receivable/status/{receivableId} | 是 | 获取应收款状态 |
| 193 | POST | /receivable/record-repayment | 是 | 记录还款上链 |
| 194 | POST | /receivable/record-full-repayment | 是 | 记录全额还款上链 |
| 195 | POST | /receivable/offset-debt | 是 | 以物抵债上链 |

**总计：195个接口**

## 数据库Schema (Flyway Migrations)

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | initial_schema | 初始Schema（区块链交易记录表） |
| V2 | blockchain_transaction_record | 区块链交易记录表 |
| V3 | create_enterprise_tables | 企业相关表 |
| V4 | create_user_table | 用户表 |
| V5 | create_warehouse_tables | 仓库相关表 |
| V6 | add_stock_order_chain_fields | 入库单链上字段 |
| V7 | add_warehouse_receipt_loan_id | 仓单关联贷款ID |
| V8 | create_credit_profile_table | 信用档案表 |
| V9 | create_credit_event_table | 信用事件表 |
| V10 | create_logistics_tables | 物流相关表 |
| V11 | create_finance_tables | 金融相关表（应收账款、还款记录） |
| V12 | add_stock_order_stock_no | 入库单编号字段 |
| V13 | create_loan_table | 贷款主表 |
| V14 | create_loan_repayment_table | 贷款还款记录表 |
| V15 | create_loan_installment_table | 贷款分期计划表 |
| V16 | add_warehouse_receipt_missing_columns | 仓单缺失列 |
| V17 | add_logistics_delegate_pickup_qr_code | 物流委派单提货二维码 |
| V18 | add_stock_order_receipt_id | 入库单关联仓单ID |
| V19 | create_login_transaction_table | 登录事务表（TCC） |
| V20 | init_sys_admin | 初始化系统管理员 |
| V21 | add_receivable_finance_fields | 应收款融资字段 |
| V22 | add_warehouse_receipt_remark | 仓单备注字段 |

## 合约清单（从docker-compose.yml和.env）

| 合约名 | 地址 | 关键方法 |
|--------|------|----------|
| CONTRACT_LIB_BYTES | 0x19a6434154de51c7a7406edf312f01527441b561 | 工具库 |
| CONTRACT_LIB_STRING | 0x745d4de0cf93b7d1db8dd8892daf05ac745766ce | 工具库 |
| CONTRACT_ENTERPRISE | 0xe3fffb217e885578f75e1ac07f1fbff859171fe3 | 企业注册、状态更新 |
| CONTRACT_ENTERPRISE_AUTH | 0xc860ab27901b3c2b810165a6096c64d88763617f | 企业授权 |
| CONTRACT_CREDIT_CORE | 0xafcdafa5be0a0e2c34328adf10d893a591b5e774 | 信用核心 |
| CONTRACT_CREDIT_SCORE | 0x6ea6907f036ff456d2f0f0a858afa9807ff4b788 | 信用评分 |
| CONTRACT_WAREHOUSE_CORE | 0x5e0aa2793a9db58513610d8ff35aa877cee75b8e | 仓单核心 |
| CONTRACT_WAREHOUSE_OPS | 0xa26565f61568353af17f8ce9beeb8e685140d6fe | 仓单运营 |
| CONTRACT_WAREHOUSE_CORE_EXT | 0x55b63f96d81f094729af702adfc7af72bd75c54e | 仓单扩展 |
| CONTRACT_RECEIVABLE_CORE | 0xb31661caf079ddd45d5ed8af7becc220199fab29 | 应收款核心 |
| CONTRACT_RECEIVABLE_REPAYMENT | 0x1d38f5d0c8c1ae7ed63a2d0ec905b9e9a17e70cf | 应收款还款 |
| CONTRACT_LOGISTICS_CORE | 0x3d06c6a1df7d56effec855f813f797a64ea1cee5 | 物流核心 |
| CONTRACT_LOGISTICS_OPS | 0x525f4e5362d8b15e5d4aa0335b2d153c70aa5eca | 物流运营 |
| CONTRACT_LOAN_CORE | 0xbf14744175b48ac9a2e1fc4ebc6c0a5f4afd0ad2 | 贷款核心 |
| CONTRACT_LOAN_REPAYMENT | 0xd688eabe4597d2d23180045a4444d0c3450b6ab9 | 贷款还款 |
| CONTRACT_BILL_CORE | 0x2af6160e266f763652f433a80b94fc13f4065303 | 票据核心 |
| CONTRACT_BILL_OPS | 0xa6cdc4fed96bbf8c9c013cc19200b3c8ba95c93e | 票据运营 |

## 数据血缘异常

- 无明显异常（Flyway schema与Entity字段基本对应）

## 企业角色说明

| entRole值 | 角色名 | 说明 |
|-----------|--------|------|
| 1 | 核心企业 | 供应链核心企业 |
| 2 | 金融机构 | 银行等金融机构 |
| 3 | 供应商 | 原材料供应商 |
| 4 | 经销商 | 经销商 |
| 5 | 仓储企业 | 仓储服务提供方 |
| 6 | 物流企业 | 物流运输企业 |

## 业务闭环路径（测试计划依据）

完整供应链金融业务闭环：

1. **平台管理员** 登录 auth-service → 注册/审核企业
2. **仓储企业** 注册仓库 → 审核通过
3. **货主企业** 申请入库 → 仓储方确认入库
4. **仓储方** 签发仓单 → 货主获得仓单
5. **货主企业** 仓单背书转让给下游 → 下游确认背书
6. **下游企业** 仓单质押贷款 → 金融机构审批 → 放款
7. **金融机构** 创建物流委派单 → 物流方承运
8. **物流方** 提货确认 → 到货入库
9. **借款方** 提交还款 → 金融机构确认 → 仓单解锁
10. **到期还款结清**

---
*架构地图生成时间: $(date -u +%Y-%m-%dT%H:%M:%SZ)*
