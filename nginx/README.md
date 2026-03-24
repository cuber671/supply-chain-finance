# Nginx API 网关配置

> FISCO BCOS 供应链金融平台 - 微服务统一入口

## 目录结构

```
nginx/
├── nginx.conf              # 主配置文件（包含基础路由）
├── conf.d/
│   ├── upstream.conf       # 上游服务定义
│   ├── ratelimit.conf      # 限流策略配置
│   ├── blockchain.conf     # 区块链服务特殊配置
│   ├── ssl.conf            # HTTPS/SSL 配置模板
│   ├── logging.conf        # 日志配置
│   └── cors.conf           # 跨域配置
└── README.md               # 本文档
```

## 快速开始

### 1. 基础配置

```bash
# 复制配置到 Nginx 配置目录
cp nginx.conf /etc/nginx/nginx.conf
cp -r conf.d /etc/nginx/

# 测试配置语法
nginx -t

# 重载 Nginx 配置
nginx -s reload
```

### 2. Docker Compose 集成

在 `docker-compose.yml` 中添加 Nginx 服务：

```yaml
nginx:
  image: nginx:1.25-alpine
  container_name: supply-chain-nginx
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ./nginx/conf.d:/etc/nginx/conf.d:ro
    - ./nginx/ssl:/etc/nginx/ssl:ro  # SSL证书目录
  depends_on:
    - auth-service
    - enterprise-service
    - warehouse-service
    - logistics-service
    - finance-service
    - credit-service
    - fisco-gateway-service
  networks:
    - app-net
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost/health"]
    interval: 30s
    timeout: 10s
    retries: 3
```

## 路由配置

| 路由前缀 | 服务 | 端口 | 说明 |
|----------|------|------|------|
| `/api/v1/auth/*` | auth-service | 8081 | 认证服务 |
| `/api/v1/enterprise/*` | enterprise-service | 8082 | 企业服务 |
| `/api/v1/warehouse/*` | warehouse-service | 8083 | 仓单服务 |
| `/api/v1/logistics/*` | logistics-service | 8084 | 物流服务 |
| `/api/v1/finance/*` | finance-service | 8085 | 金融服务 |
| `/api/v1/credit/*` | credit-service | 8086 | 信用服务 |
| `/api/v1/blockchain/*` | fisco-gateway-service | 8087 | 区块链网关 |

## 限流策略

### 三层限流机制

1. **全局限流** (`global_limit`): 200 请求/秒
2. **服务限流**: 各服务独立限流
   - 认证服务: 50 请求/秒
   - 企业服务: 100 请求/秒
   - 仓单服务: 100 请求/秒
   - 物流服务: 100 请求/秒
   - 金融服务: 80 请求/秒
   - 信用服务: 80 请求/秒
3. **区块链限流** (`blockchain_limit`): 30 请求/秒（长耗时操作）

### 突发处理

- 普通服务: 200 请求突发
- 区块链服务: 100 请求突发

## 超时配置

| 服务类型 | connect_timeout | send_timeout | read_timeout |
|----------|-----------------|--------------|---------------|
| 普通服务 | 10s | 30s | 30s |
| 区块链服务 | 60s | 300s | 300s |

## JWT Token 转发

Nginx 自动转发以下 Header：

```
Authorization: Bearer <token>
X-Real-IP: <client_ip>
X-Forwarded-For: <client_ip, proxy1_ip, proxy2_ip>
X-Forwarded-Proto: <http/https>
```

## SSL/HTTPS 配置

1. 准备 SSL 证书文件：
   - `server.crt` - 证书文件
   - `server.key` - 私钥文件

2. 复制到 `nginx/ssl/` 目录

3. 编辑 `conf.d/ssl.conf`，取消注释证书配置

4. 重载 Nginx

## 日志分析

### 日志文件位置

```
/var/log/nginx/access-auth.log      # 认证服务
/var/log/nginx/access-enterprise.log # 企业服务
/var/log/nginx/access-warehouse.log # 仓单服务
/var/log/nginx/access-logistics.log # 物流服务
/var/log/nginx/access-finance.log   # 金融服务
/var/log/nginx/access-credit.log    # 信用服务
/var/log/nginx/access-blockchain.log # 区块链服务
/var/log/nginx/error.log            # 错误日志
```

### 日志格式说明

```
$remote_addr - $remote_user [时间] $status $body_bytes_sent "$request" rt=请求时间 uct=连接时间 uht=头部时间 urt=响应时间
```

### 查看实时日志

```bash
# 查看所有请求
tail -f /var/log/nginx/access.log

# 查看特定服务请求
tail -f /var/log/nginx/access-blockchain.log | grep ERROR

# 按状态码筛选
awk '$9 == 500' /var/log/nginx/access.log
```

## 健康检查

```bash
# Nginx 健康检查端点
curl http://localhost/health

# 预期响应: nginx healthy
```

## 常见问题

### 1. 502 Bad Gateway

- 检查上游服务是否启动
- 检查 Docker 网络连通性
- 检查防火墙规则

```bash
# 测试服务连通性
docker exec supply-chain-nginx curl -v http://auth-service:8081/actuator/health
```

### 2. 504 Gateway Timeout

- 区块链交易超时，检查 FISCO 节点状态
- 普通请求超时，检查服务负载

### 3. 429 Too Many Requests

限流触发，降低请求频率或调整限流阈值

### 4. CORS 跨域问题

检查 `conf.d/cors.conf` 配置，确保前端域名在允许列表中

## 性能调优

### 连接池优化

```nginx
# nginx.conf
upstream xxx_service {
    server xxx-service:8080;
    keepalive 64;  # 长连接数量
}
```

### 缓存配置（可选）

```nginx
location /api/v1/enterprise/list {
    proxy_cache_valid 200 5m;  # 缓存5分钟
    proxy_cache_use_stale error timeout updating;
    add_header X-Cache-Status $upstream_cache_status;
}
```

## 部署检查清单

- [ ] Nginx 配置语法检查 `nginx -t`
- [ ] 所有上游服务健康状态确认
- [ ] SSL 证书有效性验证
- [ ] 限流阈值根据实际负载调整
- [ ] 日志轮转配置 (`logrotate`)
- [ ] 监控告警配置（错误率、响应时间）

## 参考文档

- [Nginx 官方文档](https://nginx.org/en/docs/)
- [Nginx限流配置](https://nginx.org/en/docs/http/ngx_http_limit_req_module.html)
- [Docker Compose 网络](https://docs.docker.com/compose/networking/)
