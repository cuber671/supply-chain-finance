# ============================================================
# Finance Service Dockerfile
# 直接使用预构建 JAR
# ============================================================
FROM eclipse-temurin:11-jre-focal

# 安装基础工具
RUN apt-get update && apt-get install -y \
    curl tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs

# 直接从宿主机构建目录复制 JAR
COPY services/finance-service/target/*.jar app.jar

# 设置环境变量
ENV HOME=/root
ENV LANG=zh_CN.UTF-8
ENV LC_ALL=zh_CN.UTF-8

# 暴露端口
EXPOSE 8085

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/ || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar app.jar"]