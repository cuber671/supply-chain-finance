# 供应链金融微服务项目功能修复计划

**项目**: FISCO BCOS Supply Chain Finance Platform
**当前版本**: 微服务架构 (supply-chain-finance)
**基准版本**: 单体应用 (my-bcos-app)
**制定日期**: 2026-03-24
**优先级**: 高 → 中 → 低

---

## 一、问题汇总

### 1.1 高优先级问题 🔴

| # | 模块 | 问题描述 | 影响 |
|---|------|----------|------|
| P1 | User | 注销申请/撤回/审核流程完全缺失 | 员工无法完成离职流程 |
| P2 | User | 用户资料查询/修改API缺失 | 用户无法查看/修改个人信息 |
| P3 | User | 密码修改API缺失 | 用户无法修改登录密码 |
| P4 | User | 企业员工列表查询缺失 | 企业管理员无法查看员工 |
| P5 | User | 用户审核流程缺失 | 新用户无法完成注册审核 |
| P6 | Enterprise | 区块链查询API全部缺失 (6个) | 无法从链上验证企业信息 |
| P7 | Credit | @RequireRole 授权注解全部缺失 | 安全风险，任意用户可调用敏感API |
| P8 | Logistics | 区块链失败不回滚事务 | 数据一致性风险 |

### 1.2 中优先级问题 🟡

| # | 模块 | 问题描述 |
|---|------|----------|
| M1 | Enterprise | 系统管理员登录API缺失 |
| M2 | Enterprise | 企业用户管理API缺失 (info_user, get_user, users/{userId}) |
| M3 | Logistics | 电子围栏(Geofencing)功能缺失 |
| M4 | Finance | 金融机构验证逻辑缺失 |
| M5 | Warehouse | StockOrder Hash计算功能缺失 |

---

## 二、修复计划

### Phase 1: User 模块功能修复 (高优先级)

#### 目标: 将 User API 从 7 个恢复到 16 个

**涉及文件:**
- `services/auth-service/src/main/java/com/fisco/app/controller/UserController.java`
- `services/auth-service/src/main/java/com/fisco/app/service/UserService.java`
- `services/auth-service/src/main/java/com/fisco/app/service/UserServiceImpl.java`
- `services/auth-service/src/main/java/com/fisco/app/entity/User.java`

#### 1.1 新增/恢复 API 端点

| # | 方法 | 路径 | 功能 | 状态 |
|---|------|------|------|------|
| 1 | GET | /api/v1/auth/users/profile | 获取当前用户资料 | 新增 |
| 2 | PUT | /api/v1/auth/users/profile | 修改当前用户资料 | 新增 |
| 3 | POST | /api/v1/auth/users/password | 修改密码 | 新增 |
| 4 | GET | /api/v1/auth/users/enterprise/{entId}/list | 获取企业员工列表 | 新增 |
| 5 | GET | /api/v1/auth/users/pending | 获取待审核用户列表 | 新增 |
| 6 | POST | /api/v1/auth/users/{userId}/audit | 审核用户注册 | 新增 |
| 7 | PUT | /api/v1/auth/users/{userId}/disable | 强制禁用用户 | 新增 |
| 8 | DELETE | /api/v1/auth/users/{userId} | 删除用户 | 新增 |
| 9 | POST | /api/v1/auth/users/cancel/apply | 发起注销申请 | 新增 |
| 10 | POST | /api/v1/auth/users/cancel/revoke | 撤回注销申请 | 新增 |
| 11 | GET | /api/v1/auth/users/cancel/pending | 获取待审核注销列表 | 新增 |
| 12 | POST | /api/v1/auth/users/{userId}/cancel/audit | 审核注销申请 | 新增 |

#### 1.2 UserService 接口新增方法

```java
// 个人信息管理
User getProfile(Long userId);
boolean updateProfile(User user);

// 密码管理
boolean changePassword(Long userId, String oldPassword, String newPassword);

// 企业员工管理
List<User> getEnterpriseUsers(Long enterpriseId);

// 用户审核
List<User> getPendingUsers(Long enterpriseId);
boolean auditUser(Long userId, boolean approved);

// 注销流程
CancellationResult applyCancellation(Long userId, String reason, String password);
boolean revokeCancellation(Long userId);
List<User> getPendingCancellationUsers(Long enterpriseId);
boolean auditCancellation(Long userId, boolean approved);
```

#### 1.3 实现要点

1. **认证方式**: 从 HttpServletRequest 获取当前登录用户的 user_id
2. **企业隔离**: 所有查询必须带上 enterpriseId 条件，防止越权
3. **密码验证**: 修改密码前验证旧密码
4. **状态机**: 注销申请遵循 NORMAL → CANCELLING → CANCELLED 流程
5. **审核日志**: 所有审核操作记录审计日志

---

### Phase 2: Enterprise 模块功能修复 (高优先级)

#### 目标: 恢复区块链查询功能和管理员功能

**涉及文件:**
- `services/enterprise-service/src/main/java/com/fisco/app/controller/EnterpriseController.java`
- `services/enterprise-service/src/main/java/com/fisco/app/service/EnterpriseService.java`
- `services/enterprise-service/src/main/java/com/fisco/app/service/EnterpriseServiceImpl.java`
- `services/enterprise-service/src/main/java/com/fisco/app/feign/BlockchainFeignClient.java`

#### 2.1 新增 API 端点

| # | 方法 | 路径 | 功能 |
|---|------|------|------|
| 1 | GET | /api/v1/enterprise/chain/{address} | 通过区块链地址查询企业 |
| 2 | GET | /api/v1/enterprise/chain/code/{creditCode} | 通过信用代码查询链上企业 |
| 3 | GET | /api/v1/enterprise/chain/list | 查询链上企业列表 |
| 4 | PUT | /api/v1/enterprise/{entId}/rating | 更新企业信用评级上链 |
| 5 | PUT | /api/v1/enterprise/{entId}/credit-limit | 设置企业信用额度上链 |
| 6 | POST | /api/v1/enterprise/admin/login | 系统管理员登录 |
| 7 | GET | /api/v1/enterprise/info_user | 获取企业员工列表 |
| 8 | GET | /api/v1/enterprise/get_user | 获取指定用户信息 |
| 9 | PUT | /api/v1/enterprise/users/{userId} | 更新企业用户信息 |

#### 2.2 实现方案

1. **区块链查询**: 通过 `BlockchainFeignClient` 调用 fisco-gateway-service
2. **管理员登录**: 验证管理员账号密码，生成JWT
3. **用户管理**: 调用 auth-service 的用户管理接口

---

### Phase 3: Credit 模块授权修复 (高优先级)

#### 目标: 恢复 @RequireRole 注解授权

**涉及文件:**
- `services/credit-service/src/main/java/com/fisco/app/controller/CreditController.java`
- `common-api/src/main/java/com/fisco/app/enums/UserRoleEnum.java`

#### 3.1 需要添加授权的端点

| 端点 | 需要的角色 |
|------|----------|
| PUT /credit/limit | ADMIN |
| PUT /credit/limit/lock | ADMIN |
| POST /credit/blacklist/trigger | ADMIN |
| DELETE /credit/blacklist/remove | ADMIN |
| PATCH /credit/reevaluate | ADMIN |
| PATCH /credit/reevaluate/batch | ADMIN |

#### 3.2 实现方案

方案A: 引入 `@RequireRole` 注解 (推荐)
```java
@RequireRole(value = {"ADMIN"}, adminBypass = true)
@PutMapping("/limit")
public Result<Map<String, Object>> setCreditLimit(...)
```

方案B: 使用 SecurityContext 获取当前用户角色进行校验

---

### Phase 4: Logistics 模块修复 (高优先级)

#### 目标: 修复区块链失败时的事务回滚问题

**涉及文件:**
- `services/logistics-service/src/main/java/com/fisco/app/service/impl/LogisticsServiceImpl.java`

#### 4.1 问题代码位置

以下方法在区块链调用失败时仅记录日志，未抛出异常：
- `assignDriver()`
- `confirmPickup()`
- `confirmDelivery()`
- `invalidate()`

#### 4.2 修复方案

```java
// 修复前
try {
    blockchainFeignClient.assignCarrier(...);
} catch (Exception e) {
    log.error("区块链调用失败", e);
}

// 修复后
try {
    blockchainFeignClient.assignCarrier(...);
} catch (Exception e) {
    log.error("区块链调用失败", e);
    throw new RuntimeException("区块链操作失败，交易已回滚", e);
}
```

#### 4.3 新增: Geofencing 功能 (中优先级)

**位置**: `LogisticsServiceImpl`

```java
/**
 * 验证位置是否在仓库500米范围内
 * @param warehouseId 仓库ID
 * @param latitude 当前位置纬度
 * @param longitude 当前位置经度
 * @return 是否在范围内
 */
private boolean validateGeofence(Long warehouseId, BigDecimal latitude, BigDecimal longitude) {
    // 1. 获取仓库坐标
    Warehouse warehouse = warehouseFeignClient.getWarehouseById(warehouseId);
    if (warehouse == null) return false;

    BigDecimal wLat = warehouse.getLatitude();
    BigDecimal wLon = warehouse.getLongitude();

    // 2. 计算Haversine距离
    double distance = calculateHaversineDistance(
        latitude.doubleValue(), longitude.doubleValue(),
        wLat.doubleValue(), wLon.doubleValue()
    );

    // 3. 500米范围内
    return distance <= 500;
}

private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
    // Haversine公式实现
    ...
}
```

---

### Phase 5: Finance 模块修复 (中优先级)

#### 目标: 恢复区块链操作和金融机构验证

**涉及文件:**
- `services/finance-service/src/main/java/com/fisco/app/service/LoanServiceImpl.java`
- `services/finance-service/src/main/java/com/fisco/app/service/ReceivableServiceImpl.java`

#### 5.1 恢复区块链操作

需要恢复以下 ContractService:
- `ReceivableContractService` - 应收账款上链操作
- `LoanContractService` - 贷款上链操作

#### 5.2 恢复金融机构验证

在 `approveLoan()`, `rejectLoan()`, `disburseLoan()` 方法中添加:

```java
// 验证是否为金融机构
if (!enterpriseFeignClient.isFinancialInstitution(enterpriseId)) {
    throw new BusinessException("只有金融机构才能执行此操作");
}
```

---

### Phase 6: Warehouse 模块增强 (低优先级)

#### 目标: 增强数据完整性和追溯能力

**涉及文件:**
- `services/warehouse-service/src/main/java/com/fisco/app/service/impl/WarehouseServiceImpl.java`

#### 6.1 恢复 StockOrder Hash 计算

```java
/**
 * 计算入库单数据哈希用于区块链存证
 */
public String calculateStockOrderHash(StockOrder stockOrder) {
    String data = String.format("%s|%s|%s|%s|%s|%s",
        stockOrder.getStockNo(),
        stockOrder.getEnterpriseId(),
        stockOrder.getGoodsName(),
        stockOrder.getQuantity(),
        stockOrder.getWarehouseId(),
        stockOrder.getCreateTime()
    );
    return DigestUtils.sha256Hex(data);
}
```

---

## 三、修复任务清单

### Task 1: User 模块完整功能恢复

| 子任务 | 负责人 | 预估时间 | 依赖 |
|--------|--------|----------|------|
| T1.1 UserController 新增9个端点 | - | 2h | - |
| T1.2 UserService 接口新增方法 | - | 1h | T1.1 |
| T1.3 UserServiceImpl 实现逻辑 | - | 4h | T1.2 |
| T1.4 状态机验证逻辑 | - | 2h | T1.3 |
| T1.5 单元测试 | - | 2h | T1.4 |

### Task 2: Enterprise 模块区块链功能

| 子任务 | 负责人 | 预估时间 | 依赖 |
|--------|--------|----------|------|
| T2.1 BlockchainFeignClient 新增查询方法 | - | 1h | - |
| T2.2 EnterpriseController 新增6个端点 | - | 2h | T2.1 |
| T2.3 EnterpriseService 实现区块链查询 | - | 3h | T2.2 |
| T2.4 管理员登录功能 | - | 2h | - |
| T2.5 用户管理API | - | 2h | - |

### Task 3: Credit 模块授权

| 子任务 | 负责人 | 预估时间 | 依赖 |
|--------|--------|----------|------|
| T3.1 引入 RequireRole 注解 | - | 1h | - |
| T3.2 CreditController 添加授权 | - | 1h | T3.1 |
| T3.3 测试授权是否生效 | - | 1h | T3.2 |

### Task 4: Logistics 事务修复

| 子任务 | 负责人 | 预估时间 | 依赖 |
|--------|--------|----------|------|
| T4.1 修复 assignDriver 异常处理 | - | 0.5h | - |
| T4.2 修复 confirmPickup 异常处理 | - | 0.5h | - |
| T4.3 修复 confirmDelivery 异常处理 | - | 0.5h | - |
| T4.4 修复 invalidate 异常处理 | - | 0.5h | - |
| T4.5 实现 Geofencing | - | 3h | - |

### Task 5: Finance 模块

| 子任务 | 负责人 | 预估时间 | 依赖 |
|--------|--------|----------|------|
| T5.1 恢复 ReceivableContractService | - | 4h | - |
| T5.2 恢复 LoanContractService | - | 4h | - |
| T5.3 添加金融机构验证 | - | 1h | T5.2 |

### Task 6: Warehouse 模块增强

| 子任务 | 负责人 | 预估时间 | 依赖 |
|--------|--------|----------|------|
| T6.1 恢复 StockOrder Hash 计算 | - | 2h | - |

---

## 四、验证计划

### 4.1 单元测试

每个修复的模块需要通过以下测试用例:

**User 模块测试用例:**
- [ ] 注册用户 → 待审核状态
- [ ] 管理员审核 → 正常状态
- [ ] 用户修改资料 → 验证字段更新
- [ ] 用户修改密码 → 旧密码验证
- [ ] 发起注销申请 → 状态变为 CANCELLING
- [ ] 审核注销通过 → 状态变为 CANCELLED
- [ ] 企业管理员查看员工列表 → 仅本企业员工

**Enterprise 模块测试用例:**
- [ ] 管理员登录成功
- [ ] 区块链地址查询企业
- [ ] 信用代码查询链上企业
- [ ] 更新信用评级上链

**Credit 模块测试用例:**
- [ ] 非ADMIN用户调用 limit API → 403 Forbidden
- [ ] ADMIN用户调用 limit API → 成功

**Logistics 模块测试用例:**
- [ ] 区块链失败 → 本地事务回滚
- [ ] Geofencing 500m内 → 允许操作
- [ ] Geofencing 500m外 → 拒绝操作

### 4.2 集成测试

```bash
# 启动所有服务后执行
mvn clean package -DskipTests
docker-compose up -d

# 测试User完整流程
curl -X POST http://localhost:8081/api/v1/auth/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","realName":"测试"}'

# 用管理员账号审核
curl -X POST http://localhost:8081/api/v1/auth/users/{userId}/audit \
  -H "Authorization: Bearer {admin_token}" \
  -d '{"approved":true}'

# 测试区块链上链
curl -X PUT http://localhost:8082/api/v1/enterprise/1/rating \
  -H "Authorization: Bearer {admin_token}" \
  -d '{"rating":"AAA"}'
```

---

## 五、风险评估

### 5.1 高风险项

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 区块链兼容性 | fisco-gateway-service API 可能与原ContractService接口不同 | 先验证FeignClient接口匹配性 |
| 分布式事务 | 跨服务调用可能失败 | 添加重试机制和补偿事务 |
| 数据迁移 | 用户状态在两个系统间可能不一致 | 使用统一的状态机 |

### 5.2 中风险项

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 性能影响 | 分页查询可能影响响应时间 | 添加索引 |
| Feign超时 | 区块链查询可能超时 | 配置合理的超时时间 |

---

## 六、里程碑

| 里程碑 | 完成内容 | 目标日期 |
|--------|----------|----------|
| M1 | Phase 1 (User模块) | Day 1-2 |
| M2 | Phase 2 (Enterprise区块链) | Day 3 |
| M3 | Phase 3 (Credit授权) | Day 3 |
| M4 | Phase 4 (Logistics事务) | Day 4 |
| M5 | Phase 5 (Finance区块链) | Day 5 |
| M6 | 全部测试验证 | Day 6 |

---

## 七、附录

### A. 相关文件路径

**单体项目:**
```
/home/llm_rca/fisco/my-bcos-app/src/main/java/com/fisco/app/Modules/
```

**微服务项目:**
```
/home/llm_rca/fisco/supply-chain-finance/services/
├── auth-service/
├── enterprise-service/
├── warehouse-service/
├── logistics-service/
├── finance-service/
├── credit-service/
└── fisco-gateway-service/
```

### B. API 路径变更对照

| 单体路径 | 微服务路径 |
|----------|-----------|
| /api/v1/user/* | /api/v1/auth/users/* |
| /api/v1/enterprise/chain/* | /api/v1/enterprise/chain/* (缺失) |

### C. 数据库表

主要表结构 (通过 Flyway 迁移):
- `t_user` - 用户表
- `t_enterprise` - 企业表
- `t_enterprise_invite_code` - 邀请码表
- `t_enterprise_cancellation` - 企业注销表
