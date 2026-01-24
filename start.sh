#!/bin/bash

# AI Studio 一键启动脚本
# 支持开发环境和生产环境启动

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "命令 $1 未找到，请先安装"
        exit 1
    fi
}

# 检查 Docker 和 Docker Compose
check_dependencies() {
    log_info "检查依赖..."
    check_command "docker"
    check_command "docker-compose"
    log_success "依赖检查完成"
}

# 检查 .env 文件
check_env_file() {
    log_info "检查环境配置..."
    if [ ! -f "$PROJECT_DIR/.env" ]; then
        log_warning ".env 文件不存在，正在从 .env.example 复制..."
        cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
        log_info "已创建 .env 文件，请根据需要修改配置（特别是 JWT_SECRET、EMBEDDING_API_KEY、LLM_API_KEY）"
    else
        log_success ".env 文件已存在"
    fi
}

# 启动服务
start_services() {
    local env="${1:-dev}"
    log_info "启动 $env 环境服务..."

    local compose_files="-f docker-compose.yml"
    if [ "$env" = "dev" ]; then
        compose_files="$compose_files -f docker-compose.dev.yml"
    elif [ "$env" = "prod" ]; then
        compose_files="$compose_files -f docker-compose.prod.yml"
    else
        log_error "不支持的环境: $env，只支持 dev 或 prod"
        exit 1
    fi

    # 启动服务
    docker-compose $compose_files up -d

    # 等待服务启动
    log_info "等待服务启动..."
    sleep 5

    # 检查服务状态
    if check_service_status; then
        log_success "所有服务启动成功！"
        show_access_info "$env"
    else
        log_error "服务启动失败，请检查日志"
        docker-compose $compose_files logs
        exit 1
    fi
}

# 检查服务状态
check_service_status() {
    local status=$(docker-compose ps -q | xargs docker inspect -f '{{.State.Running}}' 2>/dev/null)
    if [ -z "$status" ]; then
        return 1
    fi

    while read -r running; do
        if [ "$running" != "true" ]; then
            return 1
        fi
    done <<< "$status"

    return 0
}

# 显示访问信息
show_access_info() {
    local env="$1"
    echo
    log_info "服务访问信息："
    echo "==========================================="
    log_info "应用地址:     http://localhost:8080"
    log_info "API 文档:     http://localhost:8080/swagger-ui.html"
    log_info "健康检查:     http://localhost:8080/actuator/health"
    log_info "Prometheus:   http://localhost:9090"
    log_info "Grafana:      http://localhost:3000 (admin/admin)"
    echo "==========================================="
    echo
}

# 显示帮助信息
show_help() {
    echo "AI Studio 一键启动脚本"
    echo
    echo "用法: $0 [选项]"
    echo
    echo "选项:"
    echo "  start [dev|prod]  启动服务（默认 dev 环境）"
    echo "  stop [dev|prod]   停止服务（默认 dev 环境）"
    echo "  restart [dev|prod]  重启服务（默认 dev 环境）"
    echo "  status            检查服务状态"
    echo "  logs              查看服务日志"
    echo "  build             构建生产镜像"
    echo "  help              显示帮助信息"
    echo
    echo "示例:"
    echo "  $0 start          启动开发环境"
    echo "  $0 start prod     启动生产环境"
    echo "  $0 stop           停止所有服务"
    echo "  $0 logs           查看所有服务日志"
    echo "  $0 logs ai-studio 查看 ai-studio 服务日志"
}

# 停止服务
stop_services() {
    local env="${1:-dev}"
    log_info "停止 $env 环境服务..."

    local compose_files="-f docker-compose.yml"
    if [ "$env" = "dev" ]; then
        compose_files="$compose_files -f docker-compose.dev.yml"
    elif [ "$env" = "prod" ]; then
        compose_files="$compose_files -f docker-compose.prod.yml"
    else
        log_error "不支持的环境: $env，只支持 dev 或 prod"
        exit 1
    fi

    docker-compose $compose_files down
    log_success "服务已停止"
}

# 重启服务
restart_services() {
    local env="${1:-dev}"
    log_info "重启 $env 环境服务..."
    stop_services
    sleep 3
    start_services "$env"
}

# 检查服务状态
show_status() {
    log_info "服务状态："
    docker-compose ps
}

# 查看日志
show_logs() {
    local service="$1"
    if [ -z "$service" ]; then
        log_info "查看所有服务日志（按 Ctrl+C 停止）"
        docker-compose logs -f
    else
        log_info "查看 $service 服务日志（按 Ctrl+C 停止）"
        docker-compose logs -f "$service"
    fi
}

# 构建生产镜像
build_image() {
    log_info "构建生产镜像..."
    docker build -t ai-studio:latest .
    log_success "镜像构建成功"
}

# 主函数
main() {
    case "${1:-help}" in
        start)
            check_dependencies
            check_env_file
            start_services "${2:-dev}"
            ;;
        stop)
            check_dependencies
            stop_services "${2:-dev}"
            ;;
        restart)
            check_dependencies
            check_env_file
            restart_services "${2:-dev}"
            ;;
        status)
            check_dependencies
            show_status
            ;;
        logs)
            check_dependencies
            show_logs "$2"
            ;;
        build)
            check_dependencies
            build_image
            ;;
        help)
            show_help
            ;;
        *)
            log_error "未知命令: $1"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
