# ============================================================
# SSL 证书目录
# ============================================================
# 生产环境需要将以下文件放入此目录:
#   - server.crt  (SSL 证书)
#   - server.key  (SSL 私钥)
#
# 开发环境可以使用自签名证书:
#   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
#     -keyout server.key -out server.crt \
#     -subj "/C=CN/ST=Beijing/L=Beijing/O=FISCO/OU=Blockchain/CN=localhost"
# ============================================================
