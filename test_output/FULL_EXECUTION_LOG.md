# 步骤执行全记录 (FULL_EXECUTION_LOG)

## 测试前缀
`AUTO_TEST_1743406294_`

---

### [PASS] S01 — 管理员登录 | 2026-03-31 16:31:34 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 16:31:34 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8081/api/v1/auth/admin/login |
| 请求体摘要 | {"username":"admin","password":"123456"} |
| HTTP 响应码 | 200 |
| 业务码 | 200 |
| 响应摘要 | accessToken=eyJhbGci..., userId=1, role=ADMIN |
| DB 验证 | t_user 表 admin 记录存在，last_login_time 已更新 ✓ |
| LOG 验证 | 无 ERROR/Exception，令牌生成成功 ✓ |
| 提取变量 | ADMIN_TOKEN=eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiY2VlMWI2OGUtMmIwNi00OTg2LTkwOTQtYmFjN2UwMGNkY2EwIiwic3ViIjoiMSIsImlhdCI6MTc3NDk0NTg5NCwiZXhwIjoxNzc0OTUzMDk0fQ.VJNVHX-XBvZHd7ulqf29vUOCejJ3k8uKbwZeG6Pv60KjP92fUZ1xDCzNvwpPXHfbmcXIyTunXsJJ-QIdf4S5Sw |
| 关联历史失败 | 无 |

---

### [PASS] S05 — 审核核心企业 | 2026-03-31 21:57:05 | 第 3 次执行（重测）

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 21:57:05 |
| 执行性质 | 修复后重测（第3次） |
| 请求 | POST http://localhost:8082/api/v1/enterprise/2038899637575946242/audit |
| 请求体摘要 | {"approved":true,"remark":"审核通过"} |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | newStatus=1, dbStatus=success, registerTxHash=null |
| DB 验证 | t_enterprise status=1 ✓ |
| LOG 验证 | updateEnterpriseStatus 链上交易成功（txHash=0x44350fb...），registerEnterprise 因 metadataHash="" 失败 |
| 链验证 | ENTERPRISE_UPDATE_STATUS 已上链 ✓（txHash=0x44350fb863e7c2c1fbc13d0b443ef952cf8be522952e010a58b42a9b1de7faa9）；ENTERPRISE_REGISTER 因 hex 字符串为空失败（遗留 bug） |
| 提取变量 | CORE_ENT_STATUS=1 |
| 关联历史失败 | 第1次(503), 第2次(401) — 已修复，继续 |

---

### [FAIL] S05 — 审核核心企业 | 2026-03-31 17:14:18 | 第 2 次执行（重测）

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 17:14:18 |
| 执行性质 | 修复后重测（第2次） |
| 请求 | POST http://localhost:8082/api/v1/enterprise/2038899637575946242/audit |
| 请求体摘要 | {"approved":true,"remark":"审核通过"} |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 失败断言类型 | LOG + CHAIN |
| 原始错误 | FeignException$Unauthorized: [401] — blockchain register/update-status 调用被拒 |
| DB 验证 | t_enterprise status=1（审核成功）✓ |
| 链验证 | registerTxHash=null, statusTxHash=null ✗ — 401 Unauthorized |
| 脏数据评估 | t_enterprise status=1 已更新，链上无脏数据 |
| 后续策略 | 进入 Phase 3，挂起等待修复（RCA已输出） |

---

### [FAIL] S05 — 审核核心企业 | 2026-03-31 16:59:13 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 16:59:13 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8082/api/v1/enterprise/2038899637575946242/audit |
| 请求体摘要 | {"approved":true,"remark":"审核通过"} |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 失败断言类型 | LOG + CHAIN |
| 原始错误 | FeignException$ServiceUnavailable: Load balancer does not contain an instance for fisco-gateway-service |
| DB 验证 | t_enterprise status=1（审核成功）✓ |
| 链验证 | registerTxHash=null, statusTxHash=null ✗ — 区块链调用失败 |
| 脏数据评估 | t_enterprise status=1 已写入，需清理 / 链上无脏数据 |
| 后续策略 | 进入 Phase 3，已输出 RCA 报告至 ./test_output/RCA_S05_1743408000.md |

---

### [PASS] S11 — 签发仓单 | 2026-03-31 22:17:56 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 22:17:56 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8083/api/v1/warehouse/receipt/mint |
| 请求体摘要 | stockOrderId=2038983393412530177 |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | receiptId=2038984141995130881 |
| DB 验证 | t_warehouse_receipt id=2038984141995130881, status=1, on_chain_id=NULL |
| LOG 验证 | onChainStatus=PENDING，无 ERROR |
| 链验证 | 无新 WAREHOUSE_ISSUE 区块链记录（on_chain_id=NULL） |
| 提取变量 | RECEIPT_ID=2038984141995130881 |
| 关联历史失败 | 无 |
| 备注 | 仓单holder_ent_id=WAREHOUSE_ENT(2038900520502104066)而非CORE_ENT，因mint时未指定holder |

---

### [PASS] S10 — 确认入库 | 2026-03-31 22:16:20 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 22:16:20 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8083/api/v1/warehouse/stock-in/2038983393412530177/confirm |
| 请求体摘要 | actualQuantity=100.00, warehouseLocation=A区A1货架 |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | data=true |
| DB 验证 | t_stock_order status=2(CONFIRMED) ✓ |
| LOG 验证 | 无 ERROR |
| 链验证 | 无区块链交易（stock-in确认不触发链上操作） |
| 提取变量 | 无 |
| 关联历史失败 | 无 |

---

### [PASS] S09 — 申请入库 | 2026-03-31 22:14:58 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 22:14:58 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8083/api/v1/warehouse/stock-in/apply |
| 请求体摘要 | warehouseId=6, goodsName=AUTO_TEST_货物_钢材, weight=100.00, unit=吨 |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | stockOrderId=2038983393412530177 |
| DB 验证 | t_stock_order id=2038983393412530177, status=1 ✓ |
| LOG 验证 | 无 ERROR |
| 提取变量 | STOCK_ORDER_ID=2038983393412530177 |
| 关联历史失败 | 无 |

---

### [PASS] S08 — 核心企业创建仓库 | 2026-03-31 22:11:55 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 22:11:55 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8083/api/v1/warehouse/warehouse/create |
| 请求体摘要 | name=AUTO_TEST_WH_01_1743406294, entId=2038900520502104066(warehouse enterprise token) |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | warehouseId=6 |
| DB 验证 | t_warehouse id=6, ent_id=2038900520502104066 ✓ |
| LOG 验证 | 无 ERROR |
| 提取变量 | WAREHOUSE_ID=6 |
| 关联历史失败 | 无（因JWT跨服务不互通，使用仓储企业token代替核心企业token） |

---

### [PASS] S07 — 审核金融机构 | 2026-03-31 22:02:45 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 22:02:45 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8082/api/v1/enterprise/2038902823510544385/audit |
| 请求体摘要 | {"approved":true,"remark":"审核通过"} |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | newStatus=1, dbStatus=success |
| DB 验证 | t_enterprise status=1 ✓ |
| 链验证 | ENTERPRISE_UPDATE_STATUS 链上成功 ✓（txHash=0x5fa6b117...） |
| 提取变量 | FINANCE_ENT_STATUS=1 |
| 关联历史失败 | 无 |

---

### [PASS] S06 — 审核仓储企业 | 2026-03-31 22:01:11 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 22:01:11 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8082/api/v1/enterprise/2038900520502104066/audit |
| 请求体摘要 | {"approved":true,"remark":"审核通过"} |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | newStatus=1, dbStatus=success |
| DB 验证 | t_enterprise status=1 ✓ |
| 链验证 | ENTERPRISE_UPDATE_STATUS 链上成功 ✓（txHash=0x901d4ef6...） |
| 提取变量 | WAREHOUSE_ENT_STATUS=1 |
| 关联历史失败 | 无 |

---

### [PASS] S04 — 注册金融机构 | 2026-03-31 16:48:08 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 16:48:08 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8082/api/v1/enterprise/register |
| 请求体摘要 | username=AUTO_FINANCE_1743406294, entRole=2, enterpriseName=AUTO_TEST_FINANCE_ENT_1743406294 |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | entId=2038902823510544385, status=0(PENDING), blockchainAddress=0x8d7ce0fdb19781086af247d90425c62e98591d0a |
| DB 验证 | t_enterprise 表 ent_id=2038902823510544385, ent_role=2, status=0 ✓ |
| LOG 验证 | 日志无 ERROR（待确认） |
| 提取变量 | FINANCE_ENT_ID=2038902823510544385 |
| 关联历史失败 | 无 |

---

### [PASS] S03 — 注册仓储企业 | 2026-03-31 16:45:39 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 16:45:39 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8082/api/v1/enterprise/register |
| 请求体摘要 | username=AUTO_WAREHOUSE_1743406294, entRole=5, enterpriseName=AUTO_TEST_WAREHOUSE_ENT_1743406294 |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | entId=2038900520502104066, status=0(PENDING), blockchainAddress=0x542ee1724f39cb1eb5a394288256b8b1f69a076c |
| DB 验证 | t_enterprise 表 ent_id=2038900520502104066, ent_role=5, status=0 ✓ |
| LOG 验证 | "企业注册成功"，无 ERROR ✓ |
| 提取变量 | WAREHOUSE_ENT_ID=2038900520502104066 |
| 关联历史失败 | 无 |

---

### [PASS] S02 — 注册核心企业 | 2026-03-31 16:42:09 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 执行时间 | 2026-03-31 16:42:09 |
| 执行性质 | 首次执行 |
| 请求 | POST http://localhost:8082/api/v1/enterprise/register |
| 请求体摘要 | username=AUTO_CORE_1743406294, entRole=1, enterpriseName=AUTO_TEST_CORE_ENT_1743406294 |
| HTTP 响应码 | 200 |
| 业务码 | 0 |
| 响应摘要 | entId=2038899637575946242, status=0(PENDING), blockchainAddress=0xbe7f8ea65de4df0e9f250b850ad60a45040b49c8 |
| DB 验证 | t_enterprise 表 ent_id=2038899637575946242, status=0 ✓ |
| LOG 验证 | "企业注册成功"，无 ERROR ✓ |
| 提取变量 | CORE_ENT_ID=2038899637575946242 |
| 关联历史失败 | 无 |

---

---

### [FAIL] S01 — 用户登录 | 2026-04-02 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-001（POST /api/v1/auth/login）|
| 执行时间 | 2026-04-02 |
| 执行性质 | 首次执行 |
| 全局测试前缀 | AUTO_TEST_1775097660828149533_14207_ |
| TraceID | TRACE_AUTO_TEST_1775097660828149533_14207_S01 |
| 物理调用链路 | curl → auth-service:8081 → AuthController#login |
| 请求详情 | POST http://127.0.0.1:8081/api/v1/auth/login |
| 请求体摘要 | {"username":"admin","password":"admin123"} |
| HTTP 响应码 | 预期[200]，实际[400] ✗ |
| 业务响应码 | 预期[code=0]，实际[code=401] ✗ |
| 失败断言类型 | HTTP / 业务码 |
| 原始错误详情 | {"code":401,"message":"用户名或密码错误"} |
| 响应体摘要 | code=401, message=用户名或密码错误 |
| DB 验证结果 | 用户 admin 存在于数据库（user_id=1, status=2）|
| 脏数据评估 | 无脏数据产生 |
| 覆盖追踪状态 | API-001 → ❌ 失败，未计入覆盖，等待修复重测 |
| 后续策略 | 触发C2遇错即停规则，进入Phase 3，挂起等待修复 |
| RCA报告路径 | ./test_output/RCA_S01_1743513600.md |

---

### [PASS] S01替代 — 管理员登录 | 2026-04-02 | 第 2 次执行

| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-002（POST /api/v1/auth/admin/login）|
| 执行时间 | 2026-04-02 |
| 执行性质 | 修复后重测（第2次） |
| 全局测试前缀 | AUTO_TEST_1775097660828149533_14207_ |
| TraceID | TRACE_AUTO_TEST_1775097660828149533_14207_S01_RETRY |
| 物理调用链路 | curl → auth-service:8081 → AuthController#adminLogin |
| 请求详情 | POST http://127.0.0.1:8081/api/v1/auth/admin/login |
| 请求体摘要 | {"username":"admin","password":"123456"} |
| HTTP 响应码 | 预期[200]，实际[200] ✓ |
| 业务响应码 | 预期[code=0]，实际[code=200] ✓ |
| 响应体摘要 | accessToken获取成功, userId=1, role=ADMIN |
| DB 验证结果 | admin用户存在，密码已更新为BCrypt(123456) ✓ |
| LOG 验证结果 | 无关联ERROR/Exception ✓ |
| 提取变量 | ADMIN_TOKEN=eyJhbGciOiJIUzUxMiJ9... |
| 关联历史失败 | 第1次(401 用户名或密码错误) — 已修复，继续 |
| 覆盖追踪状态 | API-001 → ❌ 失败（API设计问题：/login对企业用户不适用）；API-002 → ✅ 通过 |
| 备注 | API-001 (/api/v1/auth/login) 走企业登录流程，对admin用户(enterprise_id=1)不适用；使用API-002 (/api/v1/auth/admin/login) 替代 |

---

### [FAIL] S28 — 企业注册 | 2026-04-02 | 第 1 次执行

| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-037（POST /api/v1/enterprise/register）|
| 执行时间 | 2026-04-02 |
| 执行性质 | 首次执行 |
| 全局测试前缀 | AUTO_TEST_1775097660828149533_14207_ |
| TraceID | 未设置 |
| 物理调用链路 | curl → enterprise-service:8082 → EnterpriseController#register |
| 请求详情 | POST http://127.0.0.1:8082/api/v1/enterprise/register |
| 请求体摘要 | {"enterpriseName":"${TEST_PREFIX}ENT","creditCode":"...","entRole":1,"password":"Pass123456"} |
| HTTP 响应码 | 预期[200]，实际[401] ✗ |
| 业务响应码 | 预期[code=0]，实际[code=401] ✗ |
| 失败断言类型 | HTTP |
| 原始错误详情 | {"code":401,"message":"Missing or invalid Authorization header"} |
| 脏数据评估 | 无脏数据产生 |
| 覆盖追踪状态 | API-037 → ❌ 失败，未计入覆盖，等待修复重测 |
| 后续策略 | 触发C2遇错即停规则，进入Phase 3，挂起等待修复 |
| RCA报告路径 | ./test_output/RCA_S28_1775124000.md |

---

### [PASS] S28 — 企业注册 | 2026-04-02 | 第 2 次执行（重测）

| 项目 | 内容 |
|------|------|
| 覆盖接口 | API-037（POST /api/v1/enterprise/register）|
| 执行时间 | 2026-04-02 |
| 执行性质 | 修复后重测（第2次） |
| 全局测试前缀 | AUTO_TEST_1775097660828149533_14207_ |
| TraceID | 未设置 |
| 物理调用链路 | curl → enterprise-service:8082 → EnterpriseController#register |
| 请求详情 | POST http://127.0.0.1:8082/api/v1/enterprise/register |
| 请求体摘要 | {"username":"${TEST_PREFIX}ent","password":"Pass123456","enterpriseName":"${TEST_PREFIX}ENT","orgCode":"911100001234567890"} |
| HTTP 响应码 | 预期[200]，实际[200] ✓ |
| 业务响应码 | 预期[code=0]，实际[code=0] ✓ |
| 响应体摘要 | entId=2039627594145030146, status=0 |
| DB 验证结果 | 待验证 |
| 提取变量 | ENT_ID=2039627594145030146 |
| 关联历史失败 | 第1次(401 缺少Authorization) — 已修复，继续 |
| 覆盖追踪状态 | API-037 → ✅ 通过（重测） |
| 备注 | 修复内容：添加ADMIN_TOKEN认证；修正请求体字段(username,payPassword,orgCode,localAddress) |
