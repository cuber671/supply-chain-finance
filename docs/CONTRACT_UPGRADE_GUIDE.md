# 智能合约升级指南

## 当前版本状态

| 合约 | 当前版本 | 合约地址 |
|------|---------|---------|
| EnterpriseRegistryV2 | v2 | 见docker-compose.yml |
| WarehouseReceiptCore | v2 | 见docker-compose.yml |
| WarehouseReceiptCoreExt | v1 | 见docker-compose.yml |
| LoanCore | v1 | 见docker-compose.yml |
| LoanRepayment | v1 | 见docker-compose.yml |
| ReceivableCore | v2 | 见docker-compose.yml |
| CreditLimitCore | v2 | 见docker-compose.yml |

## 升级流程

### 阶段1：准备

1. **确定升级范围**
   - 需要升级哪些合约？
   - 是否有破坏性变更？
   - 是否需要数据迁移？

2. **编写新版本合约**
   ```solidity
   // 新版本示例
   contract LoanCoreV2 {
       uint256 public constant VERSION = 2;
       // ... 新功能
   }
   ```

3. **本地测试**
   - 使用Truffle/Foundry测试新合约
   - 确保所有单元测试通过
   - 测试与现有合约的兼容性

### 阶段2：部署

1. **编译新合约**
   ```bash
   forge build
   # 或
   truffle compile
   ```

2. **部署到测试网**
   - 使用多签账户部署
   - 记录新合约地址

3. **验证部署**
   - 检查VERSION常量
   - 验证基础功能

### 阶段3：数据迁移（如需要）

1. **编写迁移脚本**
   ```solidity
   contract DataMigration {
       function migrate(address oldContract, address newContract) public {
           // 迁移逻辑
       }
   }
   ```

2. **执行迁移**
   - 暂停旧合约（如需要）
   - 执行迁移
   - 验证数据完整性
   - 暂停迁移合约

### 阶段4：切换

1. **更新配置**
   ```bash
   # 更新docker-compose.yml中的CONTRACT_*变量
   CONTRACT_LOAN_CORE=0xNewAddress
   ```

2. **重启服务**
   ```bash
   docker compose restart fisco-gateway-service
   ```

3. **验证功能**
   - 端到端测试
   - 检查日志
   - 监控系统指标

## 重要注意事项

### 破坏性变更

如果新版本包含破坏性变更（如更改存储结构），必须：

1. **数据迁移**是必须的
2. 可能需要**双写**策略（同时写入新旧合约）
3. 建议有**回滚计划**

### 联盟链特殊考虑

1. **多签部署**: 所有部署操作需要多方签名确认
2. **协调时间**: 与所有参与方协调升级时间窗口
3. **回滚共识**: 如果需要回滚，也需要多方共识

### 推荐实践

1. **增量升级**: 尽量只做增量变更，避免大规模重构
2. **向后兼容**: 新合约应能读取旧合约的数据格式
3. **充分测试**: 在测试环境充分验证后再上生产
4. **灰度发布**: 先让部分节点升级，观察无异常后再全面升级

---

*本文档为QS-09修复的一部分 - 记录合约升级流程和最佳实践*
