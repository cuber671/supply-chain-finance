# ============================================================
# FISCO Gateway Service Dockerfile
# 区块链网关服务 - 直接使用预构建 JAR
# ============================================================
FROM eclipse-temurin:11-jre-focal

# 安装 libssl (FISCO SDK 依赖) 和基础工具
RUN apt-get update && apt-get install -y \
    curl tzdata libssl-dev \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 创建目录
RUN mkdir -p /app/logs /app/sdk

# 直接从宿主机构建目录复制 JAR
COPY services/fisco-gateway-service/target/*.jar app.jar

# 设置环境变量
ENV HOME=/root
ENV LANG=zh_CN.UTF-8
ENV LC_ALL=zh_CN.UTF-8
# JAVA_OPTS 由 docker-compose 传入，不再硬编码

# FISCO SDK 配置
ENV FISCO_CONFIG_FILE=/app/sdk/config.toml
ENV FISCO_GROUP=group0

# 暴露端口
EXPOSE 8087

# 健康检查 (较长的 start-period 因为需要连接 FISCO 节点)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8087/ || exit 1

# 启动命令 - 创建符号链接解决 SDK 路径问题
ENTRYPOINT ["sh", "-c", "ln -sf /app/sdk/account /app/account 2>/dev/null || true && java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar app.jar"]