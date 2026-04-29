# 企业角色类型

定义在 `Enterprise.java`：

| 角色常量                       | 值 | 说明         |
| ------------------------------ | -- | ------------ |
| `ROLE_CORE_ENTERPRISE`       | 1  | 核心企业     |
| `ROLE_TRADING_PLATFORM`      | 2  | 现货交易平台 |
| `ROLE_SUPPLIER`              | 3  | 供应商       |
| `ROLE_FINANCIAL_INSTITUTION` | 6  | 金融机构     |
| `ROLE_WAREHOUSE`             | 9  | 仓储方       |
| `ROLE_LOGISTICS`             | 12 | 物流方       |

| 值 | Java (EnterpriseRoleEnum)  | Solidity (EnterpriseRole) | 说明         |
| -- | -------------------------- | ------------------------- | ------------ |
| 0  | -                          | Spare                     | 保留，不用   |
| 1  | CORE(1, "核心企业")        | CoreEnterprise            | 核心企业     |
| 2  | TRADING(2, "现货交易平台") | SpotPlatform              | 现货交易平台 |
| 3  | SUPPLIER(3, "供应商")      | Supplier                  | 供应商       |
| 6  | INSTITUTION(6, "金融机构") | FinancialInstitution      | 金融机构     |
| 9  | WAREHOUSE(9, "仓储方")     | Warehouse                 | 仓储方       |
| 12 | LOGISTICS(12, "物流方")    | Logistics                 | 物流方       |

# 用户角色类型

定义在 `User.java`：

| 角色常量          | 值             | 说明   |
| ----------------- | -------------- | ------ |
| `ROLE_ADMIN`    | `"ADMIN"`    | 管理员 |
| `ROLE_FINANCE`  | `"FINANCE"`  | 财务   |
| `ROLE_OPERATOR` | `"OPERATOR"` | 操作员 |

# 用户状态类型

定义在 `UserStatusEnum`：

| 状态               | 值 | 说明       |
| ------------------ | -- | ---------- |
| `PENDING`        | 0  | 待审核     |
| `NORMAL`         | 1  | 正常       |
| `FROZEN`         | 2  | 冻结       |
| `CANCELLED`      | 3  | 已注销     |
| `CANCELLING`     | 4  | 注销中     |
| `PENDING_CANCEL` | 6  | 注销待审核 |

# 企业及其用户注册

## 注册企业-**POST**[/api/v1/enterprise/register](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/registerEnterprise)注册企业

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/register' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjAiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiNWQ2YWRhZDUtYzBkMS00MWZkLWIzMjAtMzQ2ZGZiODk2NmZjIiwic3ViIjoiMTAwMDAwMDAwMDAwMDAwMDAwMSIsImlhdCI6MTc3NzIxNTcxOCwiZXhwIjoxNzc3MjIyOTE4fQ.oobBZ1pU3IlnZzjAkfzyVAXhErZBlSxB_JlbvevE0V_S45H8aU6lfpS50oHWQH4wjJ_b_dTWkHqRnT-cBbBgIA' \
  -H 'Content-Type: application/json' \
  -d '  {
    "username": "core_auto_mfg",
    "password": "Auto2024@123",
    "payPassword": "Pay2024@123",
    "enterpriseName": "华泰汽车制造有限公司",
    "orgCode": "91110000123456789A",
    "entRole": 1,
    "localAddress": "上海市浦东新区汽车工业园88号",
    "contactPhone": "021-88888888"
  }'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "entId": "2048418344776249345",
    "entRole": 1,
    "orgCode": "91110000123456789A",
    "enterpriseName": "华泰汽车制造有限公司",
    "blockchainAddress": "0x64a67ee4d5d839d2fc73addfaac092ee941d86f5",
    "username": "core_auto_mfg",
    "status": 0
  },
  "timestamp": 1777215965864,
  "txHash": null,
  "errorStack": null
}
```

## 管理员登录-**POST**[/api/v1/auth/admin/login](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E8%AE%A4%E8%AF%81/adminLogin)管理员登录

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/login' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "username": "user_logistics_admin",
  "password": "User2024@1234",
  "loginType": "USER"
}'
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiNzBiYjgxYmUtZTNmZC00YjhjLTlmODEtNWUzNTJlYjAwNjdkIiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzMzODM0NiwiZXhwIjoxNzc3MzQ1NTQ2fQ.Ola1DNvksvnsU2Ap2BX6XVMaUaChN9_cARAeZepWDmecwe6yZ6Kab7gZYTozvy1uv7peFQ5YmgHC-oem4y2Ftg",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoicmVmcmVzaCIsImp0aSI6IjcwYmI4MWJlLWUzZmQtNGI4Yy05ZjgxLTVlMzUyZWIwMDY3ZCIsInN1YiI6IjIwNDg0Mjg2MDUwMDcyNzgwODIiLCJpYXQiOjE3NzczMzgzNDYsImV4cCI6MTc3Nzk0MzE0Nn0.3BnFsAKor3dnSeAWfBS4_Yqpekc0h4A4ax8GYGEskZMoDdLaohXwOU-9N-jQSLbE0Otnej5vtY05BXpjflm96w",
    "expirationSeconds": "7200",
    "userId": "2048428605007278082",
    "entId": "2048418344776249345"
  },
  "message": "登录成功"
}
```

## 审核企业申请（管理员操作）-**POST**[/api/v1/enterprise//audit](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/auditEnterprise)审核企业申请

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/2048418344776249345/audit' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjAiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiNWQ2YWRhZDUtYzBkMS00MWZkLWIzMjAtMzQ2ZGZiODk2NmZjIiwic3ViIjoiMTAwMDAwMDAwMDAwMDAwMDAwMSIsImlhdCI6MTc3NzIxNTcxOCwiZXhwIjoxNzc3MjIyOTE4fQ.oobBZ1pU3IlnZzjAkfzyVAXhErZBlSxB_JlbvevE0V_S45H8aU6lfpS50oHWQH4wjJ_b_dTWkHqRnT-cBbBgIA' \
  -H 'Content-Type: application/json' \
  -d '{
  "approved": true
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "newStatus": 1,
    "registerTxHash": "0xd503a843a97a910b98363e4d2a351c34aa31ecec5234b51287ad1ade9669fe35",
    "action": "通过",
    "statusTxHash": "0x0d43774a55ae0a219194403ca64676106190d6b5c957602b7464518846f82f16",
    "dbStatus": "success",
    "enterpriseId": "2048418344776249345",
    "enterpriseName": "华泰汽车制造有限公司"
  },
  "timestamp": 1777216008015,
  "txHash": null,
  "errorStack": null
}
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/2048419643689283585/audit' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjAiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiNWQ2YWRhZDUtYzBkMS00MWZkLWIzMjAtMzQ2ZGZiODk2NmZjIiwic3ViIjoiMTAwMDAwMDAwMDAwMDAwMDAwMSIsImlhdCI6MTc3NzIxNTcxOCwiZXhwIjoxNzc3MjIyOTE4fQ.oobBZ1pU3IlnZzjAkfzyVAXhErZBlSxB_JlbvevE0V_S45H8aU6lfpS50oHWQH4wjJ_b_dTWkHqRnT-cBbBgIA' \
  -H 'Content-Type: application/json' \
  -d '{
  "approved": true
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "newStatus": 1,
    "registerTxHash": "0xaad21c2ebf8df51d949c890e7d309942e82f0fc632b1b969a1e13c8f669e55dc",
    "action": "通过",
    "statusTxHash": "0xa802a6e95c52fb05d11bdfed66d2fb690e002b3c0c214635bd1cfdf96d7da00c",
    "dbStatus": "success",
    "enterpriseId": "2048419643689283585",
    "enterpriseName": "上海华东仓储物流有限公司"
  },
  "timestamp": 1777216362789,
  "txHash": null,
  "errorStack": null
}
```

## 注册企业登录-**POST**[/api/v1/enterprise/login](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/login)企业登录

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/login' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "core_auto_mfg",
    "password": "Auto2024@123"
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "expiresIn": null,
    "entId": "2048418344776249345",
    "entRole": 1,
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiJmZTQyNjE1NS1mMDI4LTRkYzItYjNiMS0yOWMwMmQwYjhiZGMiLCJzdWIiOiIyMDQ4NDE4MzQ0Nzc2MjQ5MzQ1IiwiaWF0IjoxNzc3MjE2ODc1LCJleHAiOjE3NzcyMjQwNzV9.9U8OeAptDdvsl8lR7kJoP7-1hM9-FgJsU7ycof4b_Oo2SKv0FClaNUSxtyRMojIq5MdtI9pjNyXGDX-vCh8Apg",
    "enterpriseName": "华泰汽车制造有限公司",
    "blockchainAddress": "0x64a67ee4d5d839d2fc73addfaac092ee941d86f5",
    "username": "core_auto_mfg",
    "status": 1,
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJyZWZyZXNoIiwianRpIjoiZmU0MjYxNTUtZjAyOC00ZGMyLWIzYjEtMjljMDJkMGI4YmRjIiwic3ViIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImlhdCI6MTc3NzIxNjg3NSwiZXhwIjoxNzc3ODIxNjc1fQ.qz5G3j53zC5S1Q9gKwpM7stmsdZr8hBvTG2xOR_P-QJaCt2JfbnK0GPhUeBpBOJTxCVKXWwGQgfKnQhHriZNWQ"
  },
  "timestamp": 1777216876006,
  "txHash": null,
  "errorStack": null
}
```

## 生成邀请码-**GET**[/api/v1/enterprise/invite-codes](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/generateInvitationCode)生成邀请码

```bash
curl -X 'GET' \
  'http://localhost:8082/api/v1/enterprise/invite-codes?maxUses=10&expireDays=30' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiJmZTQyNjE1NS1mMDI4LTRkYzItYjNiMS0yOWMwMmQwYjhiZGMiLCJzdWIiOiIyMDQ4NDE4MzQ0Nzc2MjQ5MzQ1IiwiaWF0IjoxNzc3MjE2ODc1LCJleHAiOjE3NzcyMjQwNzV9.9U8OeAptDdvsl8lR7kJoP7-1hM9-FgJsU7ycof4b_Oo2SKv0FClaNUSxtyRMojIq5MdtI9pjNyXGDX-vCh8Apg'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "code": "BRN5TWHY",
    "maxUses": 10,
    "expireDays": 30,
    "remark": null
  },
  "timestamp": 1777217211221,
  "txHash": null,
  "errorStack": null
}
```

## 用户注册（使用企业的邀请码）-**POST**[/api/v1/auth/users/register](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/register)用户注册

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/register' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '  {
    "username": "user_logistics_fin",
    "password": "User2024@123",
    "realName": "财务",
    "phone": "13900000001",
    "email": "admin@zhongtong.com",
    "userRole": "FINANCE",
    "inviteCode": "BRN5TWHY"
  }'
{
  "code": 200,
  "data": {
    "userId": 2048428605007278000
  },
  "message": "注册成功，请等待审核"
}
```

## 企业登录-**POST**[/api/v1/enterprise/login](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/login)企业登录

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/login' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiJmZTQyNjE1NS1mMDI4LTRkYzItYjNiMS0yOWMwMmQwYjhiZGMiLCJzdWIiOiIyMDQ4NDE4MzQ0Nzc2MjQ5MzQ1IiwiaWF0IjoxNzc3MjE2ODc1LCJleHAiOjE3NzcyMjQwNzV9.9U8OeAptDdvsl8lR7kJoP7-1hM9-FgJsU7ycof4b_Oo2SKv0FClaNUSxtyRMojIq5MdtI9pjNyXGDX-vCh8Apg' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "core_auto_mfg",
    "password": "Auto2024@123"
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "expiresIn": null,
    "entId": "2048418344776249345",
    "entRole": 1,
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiIyZTMxZmM0ZC1kNTM0LTQ2MmQtOTQ1Zi03ZjA0NDFmM2I2MmMiLCJzdWIiOiIyMDQ4NDE4MzQ0Nzc2MjQ5MzQ1IiwiaWF0IjoxNzc3MjE4NDYwLCJleHAiOjE3NzcyMjU2NjB9.nhObOwNibzwOYY1rHcyL1LE8Bgaqh9k-5-ZDHbPgQZlFKe1CXzd8RHM79hVRmWtEr64sTP4y-Hw8xDl1pYfcEw",
    "enterpriseName": "华泰汽车制造有限公司",
    "blockchainAddress": "0x64a67ee4d5d839d2fc73addfaac092ee941d86f5",
    "username": "core_auto_mfg",
    "status": 1,
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJyZWZyZXNoIiwianRpIjoiMmUzMWZjNGQtZDUzNC00NjJkLTk0NWYtN2YwNDQxZjNiNjJjIiwic3ViIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImlhdCI6MTc3NzIxODQ2MCwiZXhwIjoxNzc3ODIzMjYwfQ.kGRzKCRSd47F1AzV78SkevMyjxnMCMzKWY5iMswvq57m_6LziyOyhDi3-Oo-GijMfIRjunM5sCB3HSJYZyYbrg"
  },
  "timestamp": 1777218460697,
  "txHash": null,
  "errorStack": null
}
```

## 审核用户注册申请-**POST**[/api/v1/auth/users//audit](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/auditUser)审核用户注册申请

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/2048428605007278082/audit' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODQxODM0NDc3NjI0OTM0NSIsImVudFJvbGUiOjEsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiI2ODE0YTFkMS0wZjhmLTQ2MTUtYjk1YS02ZjhmYjA5NDZiOWIiLCJzdWIiOiIyMDQ4NDE4MzQ0Nzc2MjQ5MzQ1IiwiaWF0IjoxNzc3MjcyMDI1LCJleHAiOjE3NzcyNzkyMjV9.rH-xrs5oKuZHaI7_v2PTsZKLulm-zv50l2RbU-AQN90vWU_g2-sAeRlaO3hQqGGA_IbADOo331TPiWJS-f2Evw' \
  -H 'Content-Type: application/json' \
  -d '{
  "approved": true
}'
{
  "code": 200,
  "message": "审核通过"
}
```

## 用户登录-**POST**[/api/v1/auth/login](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E8%AE%A4%E8%AF%81/login)用户登录

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/login' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "username": "user_logistics_admin",
  "password": "User2024@1234",
  "loginType": "USER"
}'
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiNzBiYjgxYmUtZTNmZC00YjhjLTlmODEtNWUzNTJlYjAwNjdkIiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzMzODM0NiwiZXhwIjoxNzc3MzQ1NTQ2fQ.Ola1DNvksvnsU2Ap2BX6XVMaUaChN9_cARAeZepWDmecwe6yZ6Kab7gZYTozvy1uv7peFQ5YmgHC-oem4y2Ftg",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoicmVmcmVzaCIsImp0aSI6IjcwYmI4MWJlLWUzZmQtNGI4Yy05ZjgxLTVlMzUyZWIwMDY3ZCIsInN1YiI6IjIwNDg0Mjg2MDUwMDcyNzgwODIiLCJpYXQiOjE3NzczMzgzNDYsImV4cCI6MTc3Nzk0MzE0Nn0.3BnFsAKor3dnSeAWfBS4_Yqpekc0h4A4ax8GYGEskZMoDdLaohXwOU-9N-jQSLbE0Otnej5vtY05BXpjflm96w",
    "expirationSeconds": "7200",
    "userId": "2048428605007278082",
    "entId": "2048418344776249345"
  },
  "message": "登录成功"
}
```

## 用户修改密码-**POST**[/api/v1/auth/users/password](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/updatePassword)修改密码

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/password' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiN2Q3NWYzMjItZTJmOS00ZjZhLTg1OWYtMjIzMDI5N2U2NmI1Iiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzI3MzM3OCwiZXhwIjoxNzc3MjgwNTc4fQ.Qi-Vcpxu3m58Qmgn7NsPudeOui89iZx23t2LoWpnKj6R6ELD6EiAC9dmsaQc-pF2LeYGWAgmMdtjEg5bBOB3OQ' \
  -H 'Content-Type: application/json' \
  -d '{
  "oldPassword": "User2024@123",
  "newPassword": "User2024@1234"
}'
{
  "code": 200,
  "message": "密码修改成功"
}
```

## 用户登录-**POST**[/api/v1/auth/login](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E8%AE%A4%E8%AF%81/login)用户登录

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/login' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "username": "user_logistics_admin",
  "password": "User2024@1234",
  "loginType": "USER"
}'
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiOWI2ODg0YWYtN2IyNy00NGUxLTg4NjYtZTY0NTllMzJlMzFmIiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzMzOTMwNiwiZXhwIjoxNzc3MzQ2NTA2fQ.xBIM0V_G8gtNfk76EwZ6N91TyGrbny188MpTwRboxsOifAjX8h56bnWQsswFn6xDdNcO9tuAvz3yk-jsmfiqCQ",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoicmVmcmVzaCIsImp0aSI6IjliNjg4NGFmLTdiMjctNDRlMS04ODY2LWU2NDU5ZTMyZTMxZiIsInN1YiI6IjIwNDg0Mjg2MDUwMDcyNzgwODIiLCJpYXQiOjE3NzczMzkzMDYsImV4cCI6MTc3Nzk0NDEwNn0.oNy4XVJ8Dxj3b0ndknClbfktzgkXPwYkNLkCJCQqrnicRe_Jf_rSZJbIfd_eme2yZN3CoeNiUJyQbMjBTaDbcw",
    "expirationSeconds": "7200",
    "userId": "2048428605007278082",
    "entId": "2048418344776249345"
  },
  "message": "登录成功"
}
```

## 变更用户角色-**PUT**[/api/v1/auth/users//role](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/updateUserRole)变更用户角色

```bash
curl -X 'PUT' \
  'http://localhost:8081/api/v1/auth/users/2048664044272848898/role' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiMTM1OWRhY2UtODkxNi00ZjFhLTg1ZTUtYjQ0OTY2MGU1NGQ5Iiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzI3NDc5NiwiZXhwIjoxNzc3MjgxOTk2fQ.6FKQUrCPmY6itHJyvPtLEnAlRaOET9mtjusK0U33el4THLGg9C9FnUkheQxVQj5I0c3VchTu841M2QHp-GNTJA' \
  -H 'Content-Type: application/json' \
  -d '{
  "role": "OPERATOR"
}'
{
  "code": 200,
  "message": "角色更新成功"
}
```

## 禁用企业用户-**PUT**[/api/v1/auth/users/disable/](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/disableUser)强制禁用用户

```bash
curl -X 'PUT' \
  'http://localhost:8081/api/v1/auth/users/disable/2048664044272848898' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiMmExYTVjM2ItOGFlMi00YzQ0LWI0ZTItMmQxNTJhYzNiMjI0Iiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzI3OTQzNywiZXhwIjoxNzc3Mjg2NjM3fQ.IaEuWyTOnUoslb2G0snQM1koyeGmD-FK9awuL8XIf4HfZtKjFecaq1UNzw2RQVG6ykncpKOP-DfO7jxho59VRA'
{
  "code": 200,
  "message": "用户已禁用"
}
```

## 变更用户状态-**PUT**[/api/v1/auth/users//status](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/updateUserStatus)变更用户状态

```bash
curl -X 'PUT' \
  'http://localhost:8081/api/v1/auth/users/2048664044272848898/status' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiYWFmNDhlODUtODY2Yi00OTBmLTgxNTktYTE0NDYyZWNiZjU2Iiwic3ViIjoiMjA0ODQyODYwNTAwNzI3ODA4MiIsImlhdCI6MTc3NzI5NTUwNSwiZXhwIjoxNzc3MzAyNzA1fQ.JPtHl4MttG73MiYYKyGFPcVyzLmpjls0_Bf0PUEyZohrfbfch2ssmYiHEacmXMrb_1BujlYdTTPmwxEvIDdBVA' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": 2
}'
{
  "code": 200,
  "message": "状态更新成功"
}
```

## 用户申请注销账户-**POST**[/api/v1/auth/users/cancel/aply](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/applyCancellation)申请注销账号

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/cancel/apply' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiT1BFUkFUT1IiLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiNTg2YjEwNjAtZDA1My00NzZhLWIzZmYtZTQ1MjljN2Q1MzAxIiwic3ViIjoiMjA0ODY2NDA0NDI3Mjg0ODg5OCIsImlhdCI6MTc3NzI5NzE2NCwiZXhwIjoxNzc3MzA0MzY0fQ.Yjhk8nPeGsg6RdZCLDv4eGcFbNVgPqo-vVi3pf761j2quZJ5rrODWys3EH4YBZhyuMqolke4k8p_W6ZEz8XCdg' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "User2024@123",
  "reason": "不再使用该账号"
}'
{
  "code": 200,
  "message": "注销申请已提交，等待管理员审核"
}
```

## 用户撤回注销申请-**POST**[/api/v1/auth/users/cancel/revoke](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/revokeCancellation)撤回注销申请

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/cancel/revoke' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiT1BFUkFUT1IiLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiOGVmMTRkMTMtYjBlNC00MDFhLTgzNjktMzUzNzM3OTA1MTQyIiwic3ViIjoiMjA0ODY2NDA0NDI3Mjg0ODg5OCIsImlhdCI6MTc3NzI5ODYyNCwiZXhwIjoxNzc3MzA1ODI0fQ.FZkSvxEvKALMfPuU34sth1tleI8VC4h1iqmV4BJZempmzvIEl372AaduGfUkVeVc62MXIoXrPgcVnUlt2unsCg' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "User2024@123"
}'
{
  "code": 200,
  "message": "注销申请已撤回"
}
```

## 用户申请注销账户-**POST**[/api/v1/auth/users/cancel/apply](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/applyCancellation)申请注销账号

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/cancel/apply' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiT1BFUkFUT1IiLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiOGVmMTRkMTMtYjBlNC00MDFhLTgzNjktMzUzNzM3OTA1MTQyIiwic3ViIjoiMjA0ODY2NDA0NDI3Mjg0ODg5OCIsImlhdCI6MTc3NzI5ODYyNCwiZXhwIjoxNzc3MzA1ODI0fQ.FZkSvxEvKALMfPuU34sth1tleI8VC4h1iqmV4BJZempmzvIEl372AaduGfUkVeVc62MXIoXrPgcVnUlt2unsCg' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "User2024@123",
  "reason": "不再使用该账号"
}'
{
  "code": 200,
  "message": "注销申请已提交，等待管理员审核"
}
```

## 审核用户注销申请-**POST**[/api/v1/auth/users/cancel/audit](http://localhost:8081/swagger-ui/index.html#/%E7%94%A8%E6%88%B7%E7%AE%A1%E7%90%86/auditCancellation)审核注销申请

```bash
curl -X 'POST' \
  'http://localhost:8081/api/v1/auth/users/2048664044272848898/cancel/audit' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiT1BFUkFUT1IiLCJlbnRJZCI6IjIwNDg0MTgzNDQ3NzYyNDkzNDUiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiOGVmMTRkMTMtYjBlNC00MDFhLTgzNjktMzUzNzM3OTA1MTQyIiwic3ViIjoiMjA0ODY2NDA0NDI3Mjg0ODg5OCIsImlhdCI6MTc3NzI5ODYyNCwiZXhwIjoxNzc3MzA1ODI0fQ.FZkSvxEvKALMfPuU34sth1tleI8VC4h1iqmV4BJZempmzvIEl372AaduGfUkVeVc62MXIoXrPgcVnUlt2unsCg' \
  -H 'Content-Type: application/json' \
  -d '{
  "approved": true
}'
{
  "code": 200,
  "message": "注销审核通过"
}
```

## 企业注销申请-**POST**[/api/v1/enterprise/cancellation/apply](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/applyCancellation)发起注销申请

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/cancellation/apply' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODk2NjA2NjAwNTY5MjQxNyIsImVudFJvbGUiOjMsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiI1OWI5YmJlZi00YWQ4LTQ2ODUtYmFiMC03MDc3Mjg4ZmViZjMiLCJzdWIiOiIyMDQ4OTY2MDY2MDA1NjkyNDE3IiwiaWF0IjoxNzc3MzQ3MDgyLCJleHAiOjE3NzczNTQyODJ9.CaBTTGiQJD7MYcCHhTbj4kxwkOTw9LQfigKFhQCIgZsGuoNq85xuwP1B0NhQHi_N0AZfx39skELbt_KbmyAXrw' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "Test2024@123",
  "reason": "不再使用该账号"
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "success": true,
    "message": "注销申请已提交，等待管理员审核",
    "entId": "2048966066005692417",
    "reason": "不再使用该账号",
    "applyTime": "2026-04-28T11:32:35.754535"
  },
  "timestamp": "1777347155754",
  "txHash": null,
  "errorStack": null
}
```

## 企业撤销注销申请-**POST**[/api/v1/enterprise/cancellation/revoke](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/revokeCancellation)撤回注销申请

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/cancellation/revoke' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODk2NjA2NjAwNTY5MjQxNyIsImVudFJvbGUiOjMsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiI1MWQzMzY3Ni0wOWVkLTQ1NTUtYTIyNC0zYzE5ODBlN2NmMzYiLCJzdWIiOiIyMDQ4OTY2MDY2MDA1NjkyNDE3IiwiaWF0IjoxNzc3MzYxMjM4LCJleHAiOjE3NzczNjg0Mzh9.QkFDuA9VVBlJdSjSoHf3aESEwUIWqj36BA7dfc27pISzgRHFckNa1m-39Q84XdCbHlrXI25qLonizzqss-sq1w' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "Test2024@123"
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": null,
  "timestamp": "1777361300572",
  "txHash": null,
  "errorStack": null
}
```

## 企业注销申请-**POST**[/api/v1/enterprise/cancellation/apply](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/applyCancellation)发起注销申请

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/cancellation/apply' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiRU5URVJQUklTRSIsImVudElkIjoiMjA0ODk2NjA2NjAwNTY5MjQxNyIsImVudFJvbGUiOjMsInNjb3BlIjo1LCJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJqdGkiOiI1MWQzMzY3Ni0wOWVkLTQ1NTUtYTIyNC0zYzE5ODBlN2NmMzYiLCJzdWIiOiIyMDQ4OTY2MDY2MDA1NjkyNDE3IiwiaWF0IjoxNzc3MzYxMjM4LCJleHAiOjE3NzczNjg0Mzh9.QkFDuA9VVBlJdSjSoHf3aESEwUIWqj36BA7dfc27pISzgRHFckNa1m-39Q84XdCbHlrXI25qLonizzqss-sq1w' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "Test2024@123",
  "reason": "不再使用该账号"
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "success": true,
    "message": "注销申请已提交，等待管理员审核",
    "entId": "2048966066005692417",
    "reason": "不再使用该账号",
    "applyTime": "2026-04-28T15:29:01.613788"
  },
  "timestamp": "1777361341614",
  "txHash": null,
  "errorStack": null
}
```

## 审核注销申请-**POST**[/api/v1/enterprise//cancellation/audit](http://localhost:8082/swagger-ui/index.html#/%E4%BC%81%E4%B8%9A%E7%AE%A1%E7%90%86/auditCancellation)审核企业注销申请

```bash
curl -X 'POST' \
  'http://localhost:8082/api/v1/enterprise/2048966066005692417/cancellation/audit' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiQURNSU4iLCJlbnRJZCI6IjAiLCJzY29wZSI6MSwidG9rZW5UeXBlIjoiYWNjZXNzIiwianRpIjoiYmEzYTc4ZWQtZDAyZi00YjlhLWJhYmYtMmNkY2IxMmE2YWFmIiwic3ViIjoiMTAwMDAwMDAwMDAwMDAwMDAwMSIsImlhdCI6MTc3NzM2MzUyNSwiZXhwIjoxNzc3MzcwNzI1fQ.6svXsEXP84LTsjsOzBVwRvEsl5Wudo9SU3O_sgNmf15cdOBsY3Fq2YltfdhiFrWIxRDjbuIsK3BY8w5CYLG8TQ' \
  -H 'Content-Type: application/json' \
  -d '{
  "approved": true
}'
{
  "code": 0,
  "msg": "操作成功",
  "data": null,
  "timestamp": "1777363551247",
  "txHash": null,
  "errorStack": null
}
```
