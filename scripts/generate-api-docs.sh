#!/bin/bash
#
# API 文档生成脚本
# 使用方法: ./scripts/generate-api-docs.sh [service_name]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCS_DIR="$PROJECT_DIR/docs/api"

# 服务配置: 服务名:端口:文档标题
SERVICES=(
    "auth-service:8081:用户认证服务"
    "enterprise-service:8082:企业管理服务"
    "warehouse-service:8083:仓单管理服务"
    "logistics-service:8084:物流管理服务"
    "finance-service:8085:金融管理服务"
    "credit-service:8086:信用评估服务"
    "fisco-gateway-service:8087:FISCO区块链网关服务"
)

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 创建文档目录
mkdir -p "$DOCS_DIR"

# 检查服务是否运行
check_service() {
    local host=$1
    local port=$2
    if timeout 2 bash -c "cat < /dev/null > /dev/tcp/$host/$port" 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

# 生成单个服务的文档
generate_service_doc() {
    local service=$1
    local port=$2
    local title=$3
    local output_file="$DOCS_DIR/${service}-api.md"

    log_info "正在生成 $title ($service) 的 API 文档..."

    # 尝试获取 OpenAPI JSON
    local api_docs_url="http://localhost:$port/v3/api-docs"

    if curl -s -f "$api_docs_url" -o "$DOCS_DIR/${service}-openapi.json" 2>/dev/null; then
        log_info "  - 已下载 OpenAPI 规范: ${service}-openapi.json"

        # 检查是否有 jq 和 npm 包来转换
        if command -v npm &> /dev/null && npm list -g @apidevtools/swagger-markdown &> /dev/null; then
            npm exec -y @apidevtools/swagger-markdown -- \
                -i "$DOCS_DIR/${service}-openapi.json" \
                -o "$output_file"
            log_info "  - 已生成 Markdown 文档: ${service}-api.md"
        elif command -v jq &> /dev/null; then
            # 手动转换简单的 Markdown（基础版本）
            convert_to_markdown "$DOCS_DIR/${service}-openapi.json" "$output_file" "$title"
            log_info "  - 已生成 Markdown 文档: ${service}-api.md"
        else
            # 仅保存 JSON
            log_warn "  - 未安装 jq，无法转换为 Markdown，请手动使用 Swagger UI 下载 JSON 后转换"
            log_warn "  - 或安装: npm install -g @apidevtools/swagger-markdown"
        fi
    else
        log_error "  - 无法连接到 $service (端口 $port)，服务是否已启动？"
        return 1
    fi
}

# 简单的 JSON to Markdown 转换（基础实现）
convert_to_markdown() {
    local json_file=$1
    local md_file=$2
    local title=$3

    {
        echo "# $title"
        echo ""
        echo "> **注意**: 此文档由 OpenAPI 规范自动生成"
        echo ""
        echo "## 基本信息"
        echo ""
        echo "- **服务地址**: http://localhost:${json_file##*-}" # 提取端口
        echo "- **API 规范**: http://localhost:${json_file##*-}/v3/api-docs"
        echo "- **Swagger UI**: http://localhost:${json_file##*-}/swagger-ui.html"
        echo ""
    } > "$md_file"

    # 使用 jq 提取更多信息（如果可用）
    if command -v jq &> /dev/null; then
        local desc=$(jq -r '.info.description // empty' "$json_file" 2>/dev/null)
        if [[ -n "$desc" ]]; then
            echo "## 接口描述" >> "$md_file"
            echo "" >> "$md_file"
            echo "$desc" | head -20 >> "$md_file"
            echo "" >> "$md_file"
        fi

        # 提取 paths
        echo "## 接口列表" >> "$md_file"
        echo "" >> "$md_file"

        jq -r '.paths | to_entries[] | "### `\(.key)`\n\n\(.value | to_entries[] | "- **\(.key)**: \(.value.summary // "无描述")\n" | join(""))"' \
            "$json_file" 2>/dev/null >> "$md_file" || true

        echo "" >> "$md_file"
        echo "## 完整 API 规范" >> "$md_file"
        echo "" >> "$md_file"
        echo "请参考 Swagger UI 获取完整的 API 文档: http://localhost:${json_file##*-}/swagger-ui.html" >> "$md_file"
    fi
}

# 生成汇总文档
generate_summary() {
    log_info "正在生成汇总文档..."

    {
        echo "# FISCO BCOS 供应链金融平台 API 文档"
        echo ""
        echo "> **生成时间**: $(date '+%Y-%m-%d %H:%M:%S')"
        echo ""
        echo "## 服务列表"
        echo ""
        echo "| 服务 | 端口 | Swagger UI | API 规范 |"
        echo "|------|------|------------|----------|"
    } > "$DOCS_DIR/SUMMARY.md"

    for svc in "${SERVICES[@]}"; do
        IFS=':' read -r name port title <<< "$svc"
        echo "| $title | $port | [Swagger UI](http://localhost:$port/swagger-ui.html) | [openapi.json](${name}-openapi.json) |" >> "$DOCS_DIR/SUMMARY.md"
    done

    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo "## 快速开始" >> "$DOCS_DIR/SUMMARY.md"
    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo "1. **启动服务**: \`docker compose up -d\`" >> "$DOCS_DIR/SUMMARY.md"
    echo "2. **访问 Swagger UI**: 打开上表中的 Swagger UI 地址" >> "$DOCS_DIR/SUMMARY.md"
    echo "3. **在线测试 API**: 在 Swagger UI 中点击 Authorize 按钮，输入 JWT Token" >> "$DOCS_DIR/SUMMARY.md"
    echo "4. **生成本地文档**: 运行 \`./scripts/generate-api-docs.sh\`" >> "$DOCS_DIR/SUMMARY.md"
    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo "## 认证说明" >> "$DOCS_DIR/SUMMARY.md"
    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo "所有接口均需要 JWT Bearer Token 认证，格式如下:" >> "$DOCS_DIR/SUMMARY.md"
    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo '```'"'"'http'"'"'" >> "$DOCS_DIR/SUMMARY.md"
    echo "Authorization: Bearer {access_token}" >> "$DOCS_DIR/SUMMARY.md"
    echo '```'"'"' >> "$DOCS_DIR/SUMMARY.md"
    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo "## 服务端口映射" >> "$DOCS_DIR/SUMMARY.md"
    echo "" >> "$DOCS_DIR/SUMMARY.md"
    echo "```" >> "$DOCS_DIR/SUMMARY.md"
    echo "auth-service:          8081" >> "$DOCS_DIR/SUMMARY.md"
    echo "enterprise-service:    8082" >> "$DOCS_DIR/SUMMARY.md"
    echo "warehouse-service:     8083" >> "$DOCS_DIR/SUMMARY.md"
    echo "logistics-service:     8084" >> "$DOCS_DIR/SUMMARY.md"
    echo "finance-service:       8085" >> "$DOCS_DIR/SUMMARY.md"
    echo "credit-service:        8086" >> "$DOCS_DIR/SUMMARY.md"
    echo "fisco-gateway-service: 8087" >> "$DOCS_DIR/SUMMARY.md"
    echo "```" >> "$DOCS_DIR/SUMMARY.md"

    log_info "汇总文档已生成: $DOCS_DIR/SUMMARY.md"
}

# 主流程
main() {
    log_info "FISCO BCOS 供应链金融平台 API 文档生成工具"
    log_info "=============================================="
    echo ""

    # 检查 Docker 是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker 未运行，请先启动 Docker"
        exit 1
    fi

    # 检查服务是否运行
    local running_services=0
    for svc in "${SERVICES[@]}"; do
        IFS=':' read -r name port title <<< "$svc"
        if check_service localhost "$port"; then
            ((running_services++))
        fi
    done

    if [[ $running_services -eq 0 ]]; then
        log_warn "未检测到运行中的服务，正在启动 Docker 服务..."
        docker compose up -d
        log_info "等待服务启动..."
        sleep 15
    fi

    # 生成各服务文档
    for svc in "${SERVICES[@]}"; do
        IFS=':' read -r name port title <<< "$svc"
        if check_service localhost "$port"; then
            generate_service_doc "$name" "$port" "$title" || true
        fi
    done

    # 生成汇总文档
    generate_summary

    echo ""
    log_info "文档生成完成！"
    log_info "文档目录: $DOCS_DIR"
    echo ""
    ls -la "$DOCS_DIR"
}

# 显示帮助
show_help() {
    echo "FISCO BCOS 供应链金融平台 API 文档生成工具"
    echo ""
    echo "使用方法:"
    echo "  $0              # 生成所有服务的 API 文档"
    echo "  $0 --help       # 显示帮助信息"
    echo ""
    echo "前提条件:"
    echo "  - Docker 和 docker compose 已安装"
    echo "  - 服务已启动 (或脚本会自动启动)"
    echo ""
    echo "可选工具 (用于 Markdown 转换):"
    echo "  - npm install -g @apidevtools/swagger-markdown"
    echo "  - jq (命令行 JSON 处理工具)"
}

case "${1:-}" in
    --help|-h)
        show_help
        exit 0
        ;;
    *)
        main
        ;;
esac