# 全链路联调测试计划（完整版）

## 测试概述

| 项目 | 内容 |
|------|------|
| 接口总数（锁定基准）| N = 201 |
| 测试计划步骤总数 | S01 - S201 |
| 测试前缀格式 | AUTO_TEST_{timestamp}_{random}_ |
| TraceID格式 | TRACE_{TEST_PREFIX}S{StepID} |
| 执行约束 | 一轮一动、遇错即停、全程留痕 |

---

## Phase 1: 公共接口 + 用户认证 (S01-S27)

### S01 — 用户登录
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-001 (POST /api/v1/auth/login) |
| 物理路径 | curl → auth-service:8081 → AuthController#login |
| 前置依赖 | 无 |
| 请求体 | `{"username":"admin","password":"123456"}` |
| 产出变量 | ADMIN_TOKEN |

### S02 — 管理员登录
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-002 (POST /api/v1/auth/admin/login) |
| 物理路径 | curl → auth-service:8081 → AuthController#adminLogin |
| 前置依赖 | 无 |
| 请求体 | `{"username":"admin","password":"123456"}` |
| 产出变量 | ADMIN_TOKEN |

### S03 — 刷新Token
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-003 (POST /api/v1/auth/refresh) |
| 物理路径 | curl → auth-service:8081 → AuthController#refresh |
| 前置依赖 | S01/S02 |
| 请求体 | `{"refreshToken":"${REFRESH_TOKEN}"}` |
| 产出变量 | NEW_ACCESS_TOKEN |

### S04 — 验证Token
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-004 (POST /api/v1/auth/validate) |
| 物理路径 | curl → auth-service:8081 → AuthController#validate |
| 前置依赖 | S01/S02 |
| 请求体 | `{"accessToken":"${ADMIN_TOKEN}"}` |
| 产出变量 | - |

### S05 — 登录状态查询
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-005 (POST /api/v1/auth/login/status) |
| 物理路径 | curl → auth-service:8081 → AuthController#loginStatus |
| 前置依赖 | S01/S02 |
| 请求体 | `{"username":"admin"}` |
| 产出变量 | - |

### S06 — 用户登出
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-006 (POST /api/v1/auth/logout) |
| 物理路径 | curl → auth-service:8081 → LogoutController#logout |
| 前置依赖 | S01/S02 |
| 请求体 | `{}` |
| 产出变量 | - |

### S07 — 用户注册
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-007 (POST /api/v1/auth/users/register) |
| 物理路径 | curl → auth-service:8081 → UserController#register |
| 前置依赖 | 无 |
| 请求体 | `{"username":"${TEST_PREFIX}user","password":"Pass123456","enterpriseId":1,"phone":"13800000001","userRole":"ADMIN"}` |
| 产出变量 | USER_ID |

### S08 — 获取用户信息
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-008 (GET /api/v1/auth/users/{userId}) |
| 物理路径 | curl → auth-service:8081 → UserController#getUserById |
| 前置依赖 | S07 |
| 产出变量 | - |

### S09 — 更新用户状态
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-009 (PUT /api/v1/auth/users/{userId}/status) |
| 物理路径 | curl → auth-service:8081 → UserController#updateStatus |
| 前置依赖 | S07 |
| 请求体 | `{"status":2}` |
| 产出变量 | - |

### S10 — 更新用户角色
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-010 (PUT /api/v1/auth/users/{userId}/role) |
| 物理路径 | curl → auth-service:8081 → UserController#updateRole |
| 前置依赖 | S07 |
| 请求体 | `{"role":"FINANCE"}` |
| 产出变量 | - |

### S11 — 获取用户资料
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-011 (GET /api/v1/auth/users/profile) |
| 物理路径 | curl → auth-service:8081 → UserController#getProfile |
| 前置依赖 | S07 |
| 产出变量 | - |

### S12 — 更新用户资料
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-012 (PUT /api/v1/auth/users/update) |
| 物理路径 | curl → auth-service:8081 → UserController#updateProfile |
| 前置依赖 | S07 |
| 请求体 | `{"phone":"13900000001"}` |
| 产出变量 | - |

### S13 — 修改密码
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-013 (POST /api/v1/auth/users/password) |
| 物理路径 | curl → auth-service:8081 → UserController#changePassword |
| 前置依赖 | S07 |
| 请求体 | `{"oldPassword":"Pass123456","newPassword":"Pass654321"}` |
| 产出变量 | - |

### S14 — 获取用户列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-014 (GET /api/v1/auth/users/list) |
| 物理路径 | curl → auth-service:8081 → UserController#getUserList |
| 前置依赖 | S02 |
| 产出变量 | - |

### S15 — 获取待审核用户列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-015 (GET /api/v1/auth/users/pending) |
| 物理路径 | curl → auth-service:8081 → UserController#getPendingUsers |
| 前置依赖 | S02 |
| 产出变量 | - |

### S16 — 审核用户
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-016 (POST /api/v1/auth/users/{userId}/audit) |
| 物理路径 | curl → auth-service:8081 → UserController#auditUser |
| 前置依赖 | S07 |
| 请求体 | `{"approved":true}` |
| 产出变量 | - |

### S17 — 申请注销用户
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-017 (POST /api/v1/auth/users/cancel/apply) |
| 物理路径 | curl → auth-service:8081 → UserController#applyCancel |
| 前置依赖 | S07 |
| 请求体 | `{"reason":"测试注销"}` |
| 产出变量 | - |

### S18 — 撤销注销申请
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-018 (POST /api/v1/auth/users/cancel/revoke) |
| 物理路径 | curl → auth-service:8081 → UserController#revokeCancel |
| 前置依赖 | S17 |
| 请求体 | `{}` |
| 产出变量 | - |

### S19 — 获取待注销用户列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-019 (GET /api/v1/auth/users/cancel/pending) |
| 物理路径 | curl → auth-service:8081 → UserController#getPendingCancelUsers |
| 前置依赖 | S02 |
| 产出变量 | - |

### S20 — 审核用户注销
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-020 (POST /api/v1/auth/users/{userId}/cancel/audit) |
| 物理路径 | curl → auth-service:8081 → UserController#auditCancel |
| 前置依赖 | S17 |
| 请求体 | `{"auditStatus":"APPROVED"}` |
| 产出变量 | - |

### S21-S27 — 健康检查接口
| Step ID | 覆盖接口 |
|---------|---------|
| S21 | API-021 (GET /health - auth-service) |
| S22 | API-036 (GET /health - credit-service) |
| S23 | API-063 (GET /health - enterprise-service) |
| S24 | API-090 (GET /health - finance-service) |
| S25 | API-150 (GET /health - fisco-gateway-service) |
| S26 | API-166 (GET /health - logistics-service) |
| S27 | API-200 (GET /health - warehouse-service) |

---

## Phase 2: 企业域 (S28-S53)

### S28 — 企业注册
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-037 (POST /api/v1/enterprise/register) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#register |
| 前置依赖 | ADMIN_TOKEN |
| 请求体 | `{"username":"${TEST_PREFIX}ent","password":"Pass123456","payPassword":"Pay123456","enterpriseName":"${TEST_PREFIX}ENT","orgCode":"91110000${TEST_PREFIX}01","entRole":1,"localAddress":"测试地址","phone":"13800000001"}` |
| 产出变量 | ENT_ID |

### S29 — 获取企业详情
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-038 (GET /api/v1/enterprise/{entId}) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getEnterprise |
| 前置依赖 | S28 |
| 产出变量 | - |

### S30 — 获取企业列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-039 (GET /api/v1/enterprise/list) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getEnterpriseList |
| 前置依赖 | S02 |
| 产出变量 | - |

### S31 — 获取待审核企业列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-040 (GET /api/v1/enterprise/pending) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getPendingEnterprises |
| 前置依赖 | S02 |
| 产出变量 | - |

### S32 — 更新企业状态
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-041 (PUT /api/v1/enterprise/{entId}/status) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#updateStatus |
| 前置依赖 | S28 |
| 请求体 | `{"status":1}` |
| 产出变量 | - |

### S33 — 审核企业
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-042 (POST /api/v1/enterprise/{entId}/audit) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#auditEnterprise |
| 前置依赖 | S28 |
| 请求体 | `{"approved":true,"remark":"测试通过"}` |
| 产出变量 | - |

### S34 — 企业登录
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-043 (POST /api/v1/enterprise/login) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#login |
| 前置依赖 | 无 |
| 请求体 | `{"username":"${TEST_PREFIX}ent","password":"Pass123456"}` |
| 产出变量 | ENT_TOKEN |

### S35 — 修改登录密码
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-044 (PUT /api/v1/enterprise/password/login) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#changeLoginPassword |
| 前置依赖 | S34 |
| 请求体 | `{"oldPassword":"Pass123456","newPassword":"Pass654321"}` |
| 产出变量 | - |

### S36 — 修改支付密码
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-045 (PUT /api/v1/enterprise/password/pay) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#changePayPassword |
| 前置依赖 | S34 |
| 请求体 | `{"oldPassword":"Pass123456","newPassword":"Pass654321"}` |
| 产出变量 | - |

### S37 — 获取企业详细信息
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-046 (GET /api/v1/enterprise/detail) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getEnterpriseDetail |
| 前置依赖 | S34 |
| 产出变量 | - |

### S38 — 获取邀请码
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-047 (GET /api/v1/enterprise/invite-codes) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getInviteCodes |
| 前置依赖 | S34 |
| 产出变量 | INVITE_CODE |

### S39 — 获取邀请码列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-048 (GET /api/v1/enterprise/invite-codes/list) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getInviteCodeList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S40 — 删除邀请码
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-049 (DELETE /api/v1/enterprise/invite-codes/{codeId}) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#deleteInviteCode |
| 前置依赖 | S38 |
| 产出变量 | - |

### S41 — 申请企业注销
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-050 (POST /api/v1/enterprise/cancellation/apply) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#applyCancellation |
| 前置依赖 | S34 |
| 请求体 | `entId=2039627594145030146&reason=测试注销` (query params) |
| 产出变量 | - |

### S42 — 撤销企业注销
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-051 (POST /api/v1/enterprise/cancellation/revoke) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#revokeCancellation |
| 前置依赖 | S41 |
| 请求体 | `{}` |
| 产出变量 | - |

### S43 — 获取待注销企业列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-052 (GET /api/v1/enterprise/cancellation/pending) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getPendingCancellations |
| 前置依赖 | S02 |
| 产出变量 | - |

### S44 — 审核企业注销
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-053 (POST /api/v1/enterprise/{entId}/cancellation/audit) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#auditCancellation |
| 前置依赖 | S41 |
| 请求体 | `{"auditStatus":"APPROVED"}` |
| 产出变量 | - |

### S50 — 通过信用代码查询链上企业
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-059 (GET /api/v1/enterprise/chain/code/{creditCode}) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getByCreditCodeOnChain |
| 前置依赖 | S34 |
| 产出变量 | - |



### S45 — 强制企业注销
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-054 (POST /api/v1/enterprise/cancellation/force) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#forceCancellation |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID}}` |
| 产出变量 | - |

### S46 — 获取资产余额
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-055 (GET /api/v1/enterprise/asset-balance) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getAssetBalance |
| 前置依赖 | S34 |
| 产出变量 | - |

### S47 — 验证邀请码
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-056 (POST /api/v1/enterprise/invitation/validate) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#validateInvitation |
| 前置依赖 | 无 |
| 请求体 | `{"inviteCode":"${INVITE_CODE}"}` |
| 产出变量 | - |

### S48 — 通过区块链地址查询企业
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-057 (GET /api/v1/enterprise/blockchain-address/{address}) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getByBlockchainAddress |
| 前置依赖 | S34 |
| 产出变量 | - |

### S49 — 检查是否为金融机构
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-058 (GET /api/v1/enterprise/check-financial-institution/{entId}) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#isFinancialInstitution |
| 前置依赖 | S28 |
| 产出变量 | - |



### S51 — 获取链上企业列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-060 (GET /api/v1/enterprise/chain/list) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#getChainEnterpriseList |
| 前置依赖 | S02 |
| 产出变量 | - |

### S52 — 更新企业信用评级
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-061 (PUT /api/v1/enterprise/{entId}/rating) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#updateRating |
| 前置依赖 | S28 |
| 请求体 | `{"rating":"AA"}` |
| 产出变量 | - |

### S53 — 更新企业授信额度
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-062 (PUT /api/v1/enterprise/{entId}/credit-limit) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#updateCreditLimit |
| 前置依赖 | S28 |
| 请求体 | `{"creditLimit":1000000}` |
| 产出变量 | - |

---

## Phase 3: 仓库域 (S54-S86)

### S53X — 创建仓库（入库前置步骤）
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-166 (POST /api/v1/warehouse/warehouse/create) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#createWarehouse |
| 前置依赖 | S34 |
| 请求体 | `{"name":"${TEST_PREFIX}仓库","address":"测试地址","contactUser":"张三","contactPhone":"13800138000"}` |
| 产出变量 | WAREHOUSE_ID |

### S54 — 申请入库
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-167 (POST /api/v1/warehouse/stock-in/apply) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#applyStockIn |
| 前置依赖 | S53X |
| 请求体 | `{"warehouseId":${WAREHOUSE_ID},"goodsName":"${TEST_PREFIX}货物","weight":100,"unit":"吨"}` |
| 产出变量 | STOCK_ORDER_ID |

### S55 — 确认入库
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-168 (POST /api/v1/warehouse/stock-in/{stockOrderId}/confirm) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#confirmStockIn |
| 前置依赖 | S54 |
| 请求体 | `{"actualWeight":100}` |
| 产出变量 | - |

### S56 — 取消入库申请
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-169 (POST /api/v1/warehouse/stock-in/{stockOrderId}/cancel) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#cancelStockIn |
| 前置依赖 | S54 |
| 请求体 | `{"reason":"测试取消"}` |
| 产出变量 | - |

### S57 — 查询入库单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-170 (GET /api/v1/warehouse/stock-in/{stockOrderIdOrNo}) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getStockIn |
| 前置依赖 | S54 |
| 产出变量 | - |

### S58 — 获取入库单列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-171 (GET /api/v1/warehouse/stock-in/list) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getStockInList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S59 — 分页获取入库单列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-172 (GET /api/v1/warehouse/stock-in/list/paginated?pageNum=1&pageSize=10) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getStockInListPaginated |
| 前置依赖 | S34 |
| 请求参数 | `pageNum=1`, `pageSize=10` |
| 产出变量 | - |

### S60 — 铸造仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-173 (POST /api/v1/warehouse/receipt/mint) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#mintReceipt |
| 前置依赖 | S55 |
| 请求体 | `{"stockOrderId":${STOCK_ORDER_ID},"ownerEntId":${ENT_ID},"totalQuantity":100}` |
| 产出变量 | RECEIPT_ID |

### S61 — 获取仓单详情
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-174 (GET /api/v1/warehouse/receipt/{receiptId}) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getReceipt |
| 前置依赖 | S60 |
| 产出变量 | - |

### S62 — 通过链上ID查询仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-175 (GET /api/v1/warehouse/receipt/by-chain/{onChainId}) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getReceiptByChainId |
| 前置依赖 | S60 |
| 产出变量 | - |

### S63 — 获取仓单列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-176 (GET /api/v1/warehouse/receipt/list) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getReceiptList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S64 — 分页获取仓单列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-177 (GET /api/v1/warehouse/receipt/list/paginated) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getReceiptListPaginated |
| 前置依赖 | S34 |
| 产出变量 | - |

### S65 — 获取在库仓单列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-178 (GET /api/v1/warehouse/receipt/in-stock) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getInStockReceipts |
| 前置依赖 | S34 |
| 产出变量 | - |

### S66 — 分页获取在库仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-179 (GET /api/v1/warehouse/receipt/in-stock/paginated) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getInStockReceiptsPaginated |
| 前置依赖 | S34 |
| 产出变量 | - |

### S67 — 验证仓单所有权
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-180 (GET /api/v1/warehouse/receipt/{receiptId}/validate-ownership) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#validateOwnership |
| 前置依赖 | S60 |
| 产出变量 | - |

### S68 — 发起仓单背书转让
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-181 (POST /api/v1/warehouse/endorsement/launch) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#launchEndorsement |
| 前置依赖 | S60 |
| 请求体 | `{"receiptId":${RECEIPT_ID},"transfereeEntId":2,"signatureHash":"0xplaceholder"}` |
| 产出变量 | ENDORSEMENT_ID |

### S69 — 确认背书转让
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-182 (POST /api/v1/warehouse/endorsement/{endorsementId}/confirm?accept=true) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#confirmEndorsement |
| 前置依赖 | S68 |
| 请求参数 | `accept=true` (Query参数) |
| 产出变量 | - |

### S70 — 撤销背书转让
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-183 (POST /api/v1/warehouse/endorsement/{endorsementId}/revoke) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#revokeEndorsement |
| 前置依赖 | S68 |
| 请求体 | `{"reason":"测试撤销"}` |
| 产出变量 | - |

### S71 — 获取背书记录列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-184 (GET /api/v1/warehouse/endorsement/list?receiptId=${RECEIPT_ID}) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getEndorsementList |
| 前置依赖 | S60 |
| 请求参数 | `receiptId=${RECEIPT_ID}` |
| 产出变量 | - |

### S72 — 申请仓单拆分
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-185 (POST /api/v1/warehouse/split/apply) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#applySplit |
| 前置依赖 | S60 |
| 请求体 | `{"receiptId":${RECEIPT_ID},"targetWeights":[50.00,50.00]}` |
| 产出变量 | SPLIT_LOG_ID |

### S73 — 申请仓单合并
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-186 (POST /api/v1/warehouse/merge/apply) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#applyMerge |
| 前置依赖 | S60 |
| 请求体 | `{"sourceReceiptIds":[1,2]}` |
| 产出变量 | MERGE_LOG_ID |

### S74 — 执行拆分/合并
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-187 (POST /api/v1/warehouse/split-merge/{opLogId}/execute) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#executeSplitMerge |
| 前置依赖 | S72/S73 |
| 请求体 | `{}` |
| 产出变量 | - |

### S75 — 查询拆分/合并详情
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-188 (GET /api/v1/warehouse/split-merge/{opLogId}) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getSplitMergeDetail |
| 前置依赖 | S72/S73 |
| 产出变量 | - |

### S76 — 取消拆分/合并
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-189 (POST /api/v1/warehouse/split-merge/{opLogId}/cancel) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#cancelSplitMerge |
| 前置依赖 | S72/S73 |
| 请求体 | `{"reason":"测试取消"}` |
| 产出变量 | - |

### S77 — 锁定仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-190 (POST /api/v1/warehouse/receipt/{receiptId}/lock) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#lockReceipt |
| 前置依赖 | S60 |
| 请求体 | `{"loanId":1}` |
| 产出变量 | - |

### S78 — 解锁仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-191 (POST /api/v1/warehouse/receipt/{receiptId}/unlock) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#unlockReceipt |
| 前置依赖 | S77 |
| 请求体 | `{}` |
| 产出变量 | - |

### S79 — 强制解锁仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-192 (POST /api/v1/warehouse/receipt/{receiptId}/force-unlock) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#forceUnlockReceipt |
| 前置依赖 | S77 |
| 请求体 | `{"reason":"测试强制解锁"}` |
| 产出变量 | - |

### S80 — 作废仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-193 (POST /api/v1/warehouse/receipt/{receiptId}/void) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#voidReceipt |
| 前置依赖 | S60 |
| 请求体 | `{"reason":"测试作废"}` |
| 产出变量 | - |

### S81 — 申请核销仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-194 (POST /api/v1/warehouse/burn/apply) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#applyBurn |
| 前置依赖 | S60 |
| 请求体 | `{"receiptId":${RECEIPT_ID},"reason":"测试核销"}` |
| 产出变量 | BURN_ID |

### S82 — 确认核销仓单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-195 (POST /api/v1/warehouse/burn/{stockOrderId}/confirm) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#confirmBurn |
| 前置依赖 | S81 |
| 请求体 | `{}` |
| 产出变量 | - |

### S83 — 创建仓库
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-196 (POST /api/v1/warehouse/warehouse/create) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#createWarehouse |
| 前置依赖 | S34 |
| 请求体 | `{"name":"${TEST_PREFIX}仓库","address":"测试地址","capacity":10000}` |
| 产出变量 | WAREHOUSE_ID |

### S84 — 获取仓库列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-197 (GET /api/v1/warehouse/warehouse/list) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getWarehouseList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S85 — 分页获取仓库列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-198 (GET /api/v1/warehouse/warehouse/list/paginated) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getWarehouseListPaginated |
| 前置依赖 | S34 |
| 产出变量 | - |

### S86 — 查询仓单追溯信息
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-199 (GET /api/v1/warehouse/receipt/{receiptId}/trace) |
| 物理路径 | curl → warehouse-service:8083 → WarehouseReceiptController#getReceiptTrace |
| 前置依赖 | S60 |
| 产出变量 | - |

---

## Phase 4: 物流域 (S87-S101)

### S86X — 注册物流企业（物流委托前置步骤）
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-032 (POST /api/v1/enterprise/register) |
| 物理路径 | curl → enterprise-service:8082 → EnterpriseController#registerEnterprise |
| 前置依赖 | S34 |
| 请求体 | `{"username":"${TEST_PREFIX}log","password":"Pass123456","payPassword":"Pay123456","enterpriseName":"${TEST_PREFIX}LOG","orgCode":"91110000${TEST_PREFIX}LOG01","entRole":12,"localAddress":"测试地址","phone":"13800000002"}` |
| 产出变量 | LOGISTICS_ENT_ID |

### S86XA — 创建物流企业用户（auth-service前置步骤）
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-002 (POST /api/v1/auth/users/register) |
| 物理路径 | curl → auth-service:8081 → UserController#register |
| 前置依赖 | S86X |
| 请求体 | `{"username":"${TEST_PREFIX}log","password":"123456","realName":"物流企业用户","phone":"13800000002","enterpriseId":${LOGISTICS_ENT_ID},"userRole":"OPERATOR"}` |
| 产出变量 | - |

### S86Y — 物流企业登录（指派前置步骤）
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-001 (POST /api/v1/auth/login) |
| 物理路径 | curl → auth-service:8081 → AuthController#login |
| 前置依赖 | S86X |
| 请求体 | `{"username":"${TEST_PREFIX}log","password":"123456","loginType":"USER"}` |
| 产出变量 | LOGISTICS_ENT_TOKEN |

### S87 — 创建物流委托
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-151 (POST /api/v1/logistics/create) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#create |
| 前置依赖 | S86X |
| 请求体 | `{"receiptId":${RECEIPT_ID},"businessScene":1,"transportQuantity":100,"unit":"吨","carrierEntId":${LOGISTICS_ENT_ID},"sourceWhId":${WAREHOUSE_ID},"targetWhId":${WAREHOUSE_ID}}` |
| 产出变量 | VOUCHER_NO |

### S88 — 查询物流委托单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-152 (GET /api/v1/logistics/delegate/{voucherNo}) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#getDelegate |
| 前置依赖 | S87 |
| 产出变量 | - |

### S89 — 获取物流委托列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-153 (GET /api/v1/logistics/delegate/list) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#getDelegateList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S90 — 指派承运方
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-154 (POST /api/v1/logistics/assign) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#assignCarrier |
| 前置依赖 | S87 |
| Token | `${LOGISTICS_ENT_TOKEN}` |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}","driverId":"TEST_DRIVER_001","driverName":"测试司机","vehicleNo":"京A12345"}` |
| 产出变量 | - |

### S91 — 确认提货
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-155 (POST /api/v1/logistics/pickup) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#pickup |
| 前置依赖 | S90 |
| Token | `${LOGISTICS_ENT_TOKEN}` |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}","authCode":"${AUTH_CODE}"}` |
| 产出变量 | - |
| 说明 | AUTH_CODE必须从S90响应的`data.authCode`字段提取（不是pickupQrCode），S90返回示例: `"authCode":"UM77TFDP"` |

### S92 — 确认到达
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-156 (POST /api/v1/logistics/arrive) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#arrive |
| 前置依赖 | S91 |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}","actionType":1}` |
| 产出变量 | - |

### S93 — 查询物流轨迹
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-157 (GET /api/v1/logistics/track) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#getTrack |
| 前置依赖 | S87 |
| 产出变量 | - |

### S94 — 获取轨迹列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-158 (GET /api/v1/logistics/track/list) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#getTrackList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S95 — 获取最新轨迹
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-159 (GET /api/v1/logistics/track/latest) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#getLatestTrack |
| 前置依赖 | S87 |
| 产出变量 | - |

### S96 — 获取轨迹偏航列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-160 (GET /api/v1/logistics/track/deviations) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#getDeviations |
| 前置依赖 | S34 |
| 产出变量 | - |

### S97 — 上报物流轨迹
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-161 (POST /api/v1/logistics/track/report) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#reportTrack |
| 前置依赖 | S91 |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}","latitude":39.9,"longitude":116.4}` |
| 产出变量 | - |

### S98 — 更新物流状态
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-162 (PUT /api/v1/logistics/status) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#updateStatus |
| 前置依赖 | S87 |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}","status":3}` |
| 产出变量 | - |

### S99 — 确认交付
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-163 (POST /api/v1/logistics/delivery/confirm) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#confirmDelivery |
| 前置依赖 | S92 |
| Token | `${ENT_TOKEN}` |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}"}` |
| 产出变量 | - |

### S100 — 作废物流单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-164 (POST /api/v1/logistics/invalidate) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#invalidate |
| 前置依赖 | S87 |
| Token | `${ENT_TOKEN}` (仅ownerEntId可作废) |
| 请求体 | `{"voucherNo":"${VOUCHER_NO}","reason":"测试作废"}` |
| 产出变量 | - |
| 说明 | 作废操作需要ownerEntId的token，不能使用LOGISTICS_ENT_TOKEN |

### S101 — 验证物流单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-165 (GET /api/v1/logistics/validate) |
| 物理路径 | curl → logistics-service:8084 → LogisticsController#validate |
| 前置依赖 | S87 |
| 产出变量 | - |

---

## Phase 5: 金融域 - 应收款 (S102-S114)

### S102 — 生成应收款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-064 (POST /api/v1/finance/receivable/generate) |
| 物理路径 | curl → finance-service:8085 → FinanceController#generateReceivable |
| 前置依赖 | S34 |
| 请求体 | `{"debtorEntId":${ENT_ID},"amount":50000}` |
| 产出变量 | RECEIVABLE_ID |

### S103 — 确认应收款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-065 (POST /api/v1/finance/receivable/confirm) |
| 物理路径 | curl → finance-service:8085 → FinanceController#confirmReceivable |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":${RECEIVABLE_ID}}` |
| 产出变量 | - |

### S104 — 调整应收款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-066 (PATCH /api/v1/finance/receivable/adjust) |
| 物理路径 | curl → finance-service:8085 → FinanceController#adjustReceivable |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":${RECEIVABLE_ID},"adjustedAmount":45000}` |
| 产出变量 | - |

### S105 — 获取应收款详情
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-067 (GET /api/v1/finance/receivable/{id}) |
| 物理路径 | curl → finance-service:8085 → FinanceController#getReceivable |
| 前置依赖 | S102 |
| 产出变量 | - |

### S106 — 通过编号查询应收款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-068 (GET /api/v1/finance/receivable/no/{receivableNo}) |
| 物理路径 | curl → finance-service:8085 → FinanceController#getReceivableByNo |
| 前置依赖 | S102 |
| 产出变量 | - |

### S107 — 获取债权人应收款列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-069 (GET /api/v1/finance/receivable/creditor/list) |
| 物理路径 | curl → finance-service:8085 → FinanceController#getCreditorReceivables |
| 前置依赖 | S34 |
| 产出变量 | - |

### S108 — 获取债务人应收款列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-070 (GET /api/v1/finance/receivable/debtor/list) |
| 物理路径 | curl → finance-service:8085 → FinanceController#getDebtorReceivables |
| 前置依赖 | S34 |
| 产出变量 | - |

### S109 — 获取企业应收款列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-071 (GET /api/v1/finance/receivable/ent/{entId}) |
| 物理路径 | curl → finance-service:8085 → FinanceController#getEnterpriseReceivables |
| 前置依赖 | S34 |
| 产出变量 | - |

### S110 — 现金还款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-072 (POST /api/v1/finance/receivable/repayment/cash) |
| 物理路径 | curl → finance-service:8085 → FinanceController#cashRepayment |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":${RECEIVABLE_ID},"amount":50000}` |
| 产出变量 | - |

### S111 — 抵债还款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-073 (POST /api/v1/finance/receivable/repayment/offset) |
| 物理路径 | curl → finance-service:8085 → FinanceController#offsetRepayment |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":${RECEIVABLE_ID},"offsetReceiptId":${RECEIPT_ID}}` |
| 产出变量 | - |

### S112 — 获取还款记录
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-074 (GET /api/v1/finance/receivable/{id}/repayments) |
| 物理路径 | curl → finance-service:8085 → FinanceController#getRepayments |
| 前置依赖 | S110 |
| 产出变量 | - |

### S112A — 金融企业登录（融资前置步骤）
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-001 (POST /api/v1/auth/login) |
| 物理路径 | curl → auth-service:8081 → AuthController#login |
| 前置依赖 | S102 |
| 请求体 | `{"username":"${TEST_PREFIX}fin","password":"123456","loginType":"USER"}` |
| 产出变量 | FINANCE_TOKEN |

### S113 — 应收款融资
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-075 (POST /api/v1/finance/receivable/finance) |
| 物理路径 | curl → finance-service:8085 → FinanceController#financeReceivable |
| 前置依赖 | S102 |
| Token | `${ENT_TOKEN}` |
| 请求体 | `{"receivableId":${RECEIVABLE_ID},"financeAmount":40000,"financeEntId":2039896024012963842}` |
| 产出变量 | - |

### S114 — 结算应收款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-076 (POST /api/v1/finance/receivable/{id}/settle) |
| 物理路径 | curl → finance-service:8085 → FinanceController#settleReceivable |
| 前置依赖 | S102 |
| 请求体 | `{}` |
| 产出变量 | - |

---

## Phase 6: 金融域 - 贷款 (S115-S127)

### S115 — 申请贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-077 (POST /api/v1/finance/loan/apply) |
| 物理路径 | curl → finance-service:8085 → LoanController#applyLoan |
| 前置依赖 | S60 |
| 请求体 | `{"receiptId":${RECEIPT_ID},"amount":50000,"term":30}` |
| 产出变量 | LOAN_ID |

### S116 — 获取贷款列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-078 (GET /api/v1/finance/loan/list) |
| 物理路径 | curl → finance-service:8085 → LoanController#getLoanList |
| 前置依赖 | S34 |
| 产出变量 | - |

### S117 — 获取贷款详情
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-079 (GET /api/v1/finance/loan/{id}) |
| 物理路径 | curl → finance-service:8085 → LoanController#getLoan |
| 前置依赖 | S115 |
| 产出变量 | - |

### S118 — 审批贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-080 (POST /api/v1/finance/loan/{id}/approve) |
| 物理路径 | curl → finance-service:8085 → LoanController#approveLoan |
| 前置依赖 | S115 |
| 请求体 | `{"approvedAmount":50000,"interestRate":0.05}` |
| 产出变量 | - |

### S119 — 拒绝贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-081 (POST /api/v1/finance/loan/{id}/reject) |
| 物理路径 | curl → finance-service:8085 → LoanController#rejectLoan |
| 前置依赖 | S115 |
| Token | `${FINANCE_TOKEN}` |
| 请求体 | `{"reason":"测试拒绝"}` |
| 产出变量 | - |
| 说明 | 拒绝贷款要求status=PENDING(待审批)。注意：S118和S119是互斥操作（审批vs拒绝），同一贷款ID只能执行其中一个。如需同时测试两个分支，请使用不同的LOAN_ID |

### S120 — 取消贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-082 (POST /api/v1/finance/loan/{id}/cancel) |
| 物理路径 | curl → finance-service:8085 → LoanController#cancelLoan |
| 前置依赖 | S118 |
| Token | `${FINANCE_TOKEN}` |
| 请求体 | `{"reason":"测试取消"}` |
| 产出变量 | - |
| 说明 | 取消贷款可在PENDING、PENDING_DISBURSE、DISBURSED状态下执行。当前测试在S118审批后执行取消 |

### S121 — 发放贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-083 (POST /api/v1/finance/loan/{id}/disburse) |
| 物理路径 | curl → finance-service:8085 → LoanController#disburseLoan |
| 前置依赖 | S118 |
| Token | `${FINANCE_TOKEN}` |
| 请求体 | `{}` |
| 产出变量 | - |
| 说明 | 发放贷款要求status=PENDING_DISBURSE(待放款)。注意：S120和S121在同一贷款上顺序执行（先取消再放款会失败）。当前测试S120先取消，S121再放款会因状态不对而失败 |

### S122 — 偿还贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-084 (POST /api/v1/finance/loan/{id}/repay) |
| 物理路径 | curl → finance-service:8085 → LoanController#repayLoan |
| 前置依赖 | S121 |
| 请求体 | `{"repaymentType":"CASH","principalAmount":35000.00,"interestAmount":143.85,"penaltyAmount":0}` |
| 产出变量 | - |

### S123 — 获取还款计划
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-085 (GET /api/v1/finance/loan/{id}/repayments) |
| 物理路径 | curl → finance-service:8085 → LoanController#getRepayments |
| 前置依赖 | S122 |
| 产出变量 | - |

### S124 — 获取分期计划
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-086 (GET /api/v1/finance/loan/{id}/installments) |
| 物理路径 | curl → finance-service:8085 → LoanController#getInstallments |
| 前置依赖 | S115 |
| 产出变量 | - |

### S125 — 贷款计算器
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-087 (POST /api/v1/finance/loan/calculator) |
| 物理路径 | curl → finance-service:8085 → LoanController#calculate |
| 前置依赖 | 无 |
| 请求体 | `{"amount":50000,"term":30,"interestRate":0.05}` |
| 产出变量 | - |

### S126 — 获取我的贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-088 (GET /api/v1/finance/loan/my) |
| 物理路径 | curl → finance-service:8085 → LoanController#getMyLoans |
| 前置依赖 | S34 |
| 产出变量 | - |

### S127 — 获取待处理贷款
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-089 (GET /api/v1/finance/loan/pending) |
| 物理路径 | curl → finance-service:8085 → LoanController#getPendingLoans |
| 前置依赖 | S34 |
| 产出变量 | - |

---

## Phase 7: 信用域 (S128-S142)

### S128 — 获取企业信用档案
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-022 (GET /api/v1/credit/profile) |
| 物理路径 | curl → credit-service:8086 → CreditController#getProfile |
| 前置依赖 | S34 |
| 产出变量 | - |

### S129 — 获取本企业信用档案
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-023 (GET /api/v1/credit/profile/me) |
| 物理路径 | curl → credit-service:8086 → CreditController#getMyProfile |
| 前置依赖 | S34 |
| 产出变量 | - |

### S130 — 获取信用评分
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-024 (GET /api/v1/credit/score) |
| 物理路径 | curl → credit-service:8086 → CreditController#getScore |
| 前置依赖 | S34 |
| 产出变量 | - |

### S131 — 上报信用事件
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-025 (POST /api/v1/credit/event/report) |
| 物理路径 | curl → credit-service:8086 → CreditController#reportEvent |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID},"eventType":"ON_TIME_REPAY","eventLevel":"LOW"}` |
| 产出变量 | - |

### S132 — 上报物流偏航事件
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-026 (POST /api/v1/credit/event/logistics-deviation) |
| 物理路径 | curl → credit-service:8086 → CreditController#reportLogisticsDeviation |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID},"voucherNo":"${VOUCHER_NO}"}` |
| 产出变量 | - |

### S133 — 获取信用事件列表
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-027 (GET /api/v1/credit/events) |
| 物理路径 | curl → credit-service:8086 → CreditController#getEvents |
| 前置依赖 | S02 |
| 产出变量 | - |

### S134 — 调整授信额度
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-028 (PUT /api/v1/credit/limit) |
| 物理路径 | curl → credit-service:8086 → CreditController#updateLimit |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID},"limit":1000000}` |
| 产出变量 | - |

### S135 — 检查授信额度
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-029 (POST /api/v1/credit/limit/check) |
| 物理路径 | curl → credit-service:8086 → CreditController#checkLimit |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID},"amount":50000}` |
| 产出变量 | - |

### S136 — 锁定授信额度
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-030 (POST /api/v1/credit/limit/lock) |
| 物理路径 | curl → credit-service:8086 → CreditController#lockLimit |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID},"amount":50000,"loanId":${LOAN_ID}}` |
| 产出变量 | - |

### S137 — 获取可用授信额度
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-031 (GET /api/v1/credit/limit/available) |
| 物理路径 | curl → credit-service:8086 → CreditController#getAvailableLimit |
| 前置依赖 | S34 |
| 产出变量 | - |

### S138 — 重新评估信用
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-032 (PATCH /api/v1/credit/reevaluate) |
| 物理路径 | curl → credit-service:8086 → CreditController#reevaluate |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID}}` |
| 产出变量 | - |

### S139 — 批量重新评估信用
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-033 (PATCH /api/v1/credit/reevaluate/batch) |
| 物理路径 | curl → credit-service:8086 → CreditController#batchReevaluate |
| 前置依赖 | S02 |
| 请求体 | `{"entIds":[${ENT_ID}]}` |
| 产出变量 | - |

### S140 — 检查黑名单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-034 (GET /api/v1/credit/blacklist/check) |
| 物理路径 | curl → credit-service:8086 → CreditController#checkBlacklist |
| 前置依赖 | S02 |
| 产出变量 | - |

### S141 — 触发黑名单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-035 (POST /api/v1/credit/blacklist/trigger) |
| 物理路径 | curl → credit-service:8086 → CreditController#triggerBlacklist |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID},"reason":"测试加入黑名单"}` |
| 产出变量 | - |

### S142 — 移除黑名单
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-036 (DELETE /api/v1/credit/blacklist/remove) |
| 物理路径 | curl → credit-service:8086 → CreditController#removeBlacklist |
| 前置依赖 | S02 |
| 请求体 | `{"entId":${ENT_ID}}` |
| 产出变量 | - |

---

## Phase 8: 区块链网关接口 (S143-S196)

### S143-S149: 基础区块链查询
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S143 | API-091 | 获取区块链状态 |
| S144 | API-092 | 获取区块链健康状态 |
| S145 | API-093 | 获取最新区块高度 |
| S146 | API-094 | 获取区块信息 |
| S147 | API-095 | 获取区块哈希 |
| S148 | API-096 | 获取交易回执 |
| S149 | API-097 | 获取区块链账户 |

### S150 — 获取地址余额
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-098 (GET /api/v1/blockchain/balance/{address}) |
| 前置依赖 | S02 |
| 产出变量 | - |

### S151 — 调用合约（只读）
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-099 (POST /api/v1/blockchain/call) |
| 前置依赖 | S02 |
| 请求体 | `{"to":"0x...","data":"0x..."}` |
| 产出变量 | - |

### S152 — 发送交易
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-100 (POST /api/v1/blockchain/transaction) |
| 前置依赖 | S02 |
| 请求体 | `{"to":"0x...","data":"0x..."}` |
| 产出变量 | TX_HASH |

### S153-S154: 群组查询
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S153 | API-101 | 获取当前群组 |
| S154 | API-102 | 获取群组列表 |

### S155-S162: 企业区块链操作
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S155 | API-103 | 注册企业到区块链 |
| S156 | API-104 | 更新企业链上状态 |
| S157 | API-105 | 更新企业信用评级 |
| S158 | API-106 | 设置企业授信额度 |
| S159 | API-107 | 查询链上企业 |
| S160 | API-108 | 通过信用代码查询 |
| S161 | API-109 | 获取链上企业列表 |
| S162 | API-110 | 验证企业有效性 |

### S163-S170: 仓单区块链操作
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S163 | API-111 | 发行仓单到区块链 |
| S164 | API-112 | 发起仓单背书 |
| S165 | API-113 | 确认仓单背书 |
| S166 | API-114 | 拆分仓单 |
| S167 | API-115 | 合并仓单 |
| S168 | API-116 | 锁定仓单 |
| S169 | API-117 | 解锁仓单 |
| S170 | API-118 | 核销仓单 |

### S171-S179: 物流区块链操作
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S171 | API-119 | 创建物流委托上链 |
| S172 | API-120 | 提货上链 |
| S173 | API-121 | 到货上链 |
| S174 | API-122 | 指派承运方上链 |
| S175 | API-123 | 确认交付上链 |
| S176 | API-124 | 更新物流状态上链 |
| S177 | API-125 | 查询链上物流轨迹 |
| S178 | API-126 | 验证物流委托有效性 |
| S179 | API-127 | 作废物流委托上链 |

### S180-S192: 贷款区块链操作
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S180 | API-128 | 创建贷款上链 |
| S181 | API-129 | 审批贷款上链 |
| S182 | API-130 | 取消贷款上链 |
| S183 | API-131 | 发放贷款上链 |
| S184 | API-132 | 偿还贷款上链 |
| S185 | API-133 | 标记逾期上链 |
| S186 | API-134 | 标记违约上链 |
| S187 | API-135 | 设置贷款仓单 |
| S188 | API-136 | 更新贷款仓单 |
| S189 | API-137 | 查询核心贷款信息 |
| S190 | API-138 | 查询贷款状态 |
| S191 | API-139 | 通过仓单查询贷款 |
| S192 | API-140 | 验证贷款是否存在 |

### S193-S201: 应收款区块链操作
| Step ID | 覆盖接口 | 描述 |
|---------|---------|------|
| S193 | API-141 | 创建应收款上链 |
| S194 | API-142 | 确认应收款上链 |
| S195 | API-143 | 调整应收款上链 |
| S196 | API-144 | 应收款融资上链 |
| S197 | API-145 | 结算应收款上链 |
| S198 | API-146 | 查询应收款状态 |
| S199 | API-147 | 记录还款上链 |
| S200 | API-148 | 记录全额还款上链 |
| S201 | API-149 | 债务抵销上链 |

### S197 — 结算应收款上链
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-145 (POST /api/v1/blockchain/receivable/settle) |
| 物理路径 | curl → fisco-gateway-service:8087 → BlockchainDomainController#settleReceivable |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":"${RECEIVABLE_ID}"}` |
| 产出变量 | - |

### S198 — 查询应收款状态
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-146 (GET /api/v1/blockchain/receivable/status/{receivableId}) |
| 物理路径 | curl → fisco-gateway-service:8087 → BlockchainDomainController#getReceivableStatus |
| 前置依赖 | S102 |
| 产出变量 | - |

### S199 — 记录还款上链
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-147 (POST /api/v1/blockchain/receivable/record-repayment) |
| 物理路径 | curl → fisco-gateway-service:8087 → BlockchainDomainController#recordReceivableRepayment |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":"${RECEIVABLE_ID}","amount":50000}` |
| 产出变量 | - |

### S200 — 记录全额还款上链
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-148 (POST /api/v1/blockchain/receivable/record-full-repayment) |
| 物理路径 | curl → fisco-gateway-service:8087 → BlockchainDomainController#recordFullRepayment |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":"${RECEIVABLE_ID}"}` |
| 产出变量 | - |

### S201 — 债务抵销上链
| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-149 (POST /api/v1/blockchain/receivable/offset-debt) |
| 物理路径 | curl → fisco-gateway-service:8087 → BlockchainDomainController#offsetDebtWithCollateral |
| 前置依赖 | S102 |
| 请求体 | `{"receivableId":"${RECEIVABLE_ID}","collateralReceiptId":"${RECEIPT_ID}"}` |
| 产出变量 | - |

---

## 数据隔离要求

所有测试数据必须使用 `TEST_PREFIX` 前缀：
```bash
TEST_PREFIX="AUTO_TEST_$(date +%s%N)_$RANDOM_"
TRACE_PREFIX="TRACE_${TEST_PREFIX}"
```

### 脏数据清理顺序（按外键依赖反向）

1. t_logistics_track
2. t_loan_repayment
3. t_loan_installment
4. t_repayment_record
5. t_loan
6. t_receipt_endorsement
7. t_logistics_delegate
8. t_stock_order
9. t_receipt_operation_log
10. t_warehouse_receipt
11. t_receivable
12. t_credit_event
13. t_enterprise_credit_profile
14. t_invitation_code
15. t_login_transaction
16. blockchain_transaction_record
17. t_user
18. t_warehouse
19. t_enterprise

---

**测试计划生成时间: 2026-04-01**
**接口覆盖率目标: 100% (201/201)**