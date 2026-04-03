# 📋 接口原始清单（覆盖率计算基准，锁定后不可更改）

## 扫描基础信息
| 项目 | 内容 |
|------|------|
| 代码分支 | main |
| Commit Hash | 785e6a4e2e1166a6e9ea6763b18083674028961a |
| 扫描时间 | 2026-04-01 21:50:00 |
| 环境标识 | **开发/测试环境**（非生产） |

## 扫描统计
| 服务名 | Controller 文件数 | 有效接口数 |
|--------|-----------------|--------|
| auth-service | 4 | 21 |
| credit-service | 2 | 15 |
| enterprise-service | 2 | 26 |
| finance-service | 3 | 28 |
| fisco-gateway-service | 3 | 59 |
| logistics-service | 2 | 15 |
| warehouse-service | 2 | 32 |
| **合计** | **18** | **N = 201** |

## 弃用接口清单
**扫描结果: 项目中未发现任何使用 @Deprecated 注解标记的接口**

## 区块链合约地址
| 合约 | 地址 |
|------|------|
| CONTRACT_ENTERPRISE | 0xe3fffb217e885578f75e1ac07f1fbff859171fe3 |
| CONTRACT_ENTERPRISE_AUTH | 0xc860ab27901b3c2b810165a6096c64d88763617f |
| CONTRACT_WAREHOUSE_CORE | 0x5e0aa2793a9db58513610d8ff35aa877cee75b8e |
| CONTRACT_WAREHOUSE_OPS | 0xa26565f61568353af17f8ce9beeb8e685140d6fe |
| CONTRACT_LOGISTICS_CORE | 0x3d06c6a1df7d56effec855f813f797a64ea1cee5 |
| CONTRACT_LOGISTICS_OPS | 0x525f4e5362d8b15e5d4aa0335b2d153c70aa5eca |
| CONTRACT_LOAN_CORE | 0xbf14744175b48ac9a2e1fc4ebc6c0a5f4afd0ad2 |
| CONTRACT_LOAN_REPAYMENT | 0xd688eabe4597d2d23180045a4444d0c3450b6ab9 |
| CONTRACT_RECEIVABLE_CORE | 0xb31661caf079ddd45d5ed8af7becc220199fab29 |
| CONTRACT_RECEIVABLE_REPAYMENT | 0x1d38f5d0c8c1ae7ed63a2d0ec905b9e9a17e70cf |
| CONTRACT_CREDIT_CORE | 0xafcdafa5be0a0e2c34328adf10d893a591b5e774 |
| CONTRACT_CREDIT_SCORE | 0x6ea6907f036ff456d2f0f0a858afa9807ff4b788 |

---

**接口总数已锁定：N = 201**
**测试计划步骤数下限：≥ 201**