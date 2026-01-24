#!/bin/bash

# AI Studio 本机启动脚本 - 优化版
# 前后端分离架构：后端(8080) + 前端开发服务器(3000)
# 支持开发环境一键启动

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

log_cyan() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "命令 $1 未找到，请先安装"
        exit 1
    fi
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."
    check_command "java"
    check_command "mvn"
    check_command "node"
    check_command "npm"
    check_command "psql"
    check_command "redis-cli"
    log_success "依赖检查完成"
}

# 检查 Java 版本
check_java_version() {
    local java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 21 ]; then
        log_error "Java 版本过低，需要 Java 21 或更高版本（当前版本: $java_version）"
        exit 1
    fi
    log_success "Java 版本检查通过: $java_version"
}

# 检查数据库连接
check_postgres() {
    log_info "检查 PostgreSQL 连接..."
    local db_name="ai_studio"
    local db_user="postgres"
    local db_password="xx123456XX"
    local db_port="5432"
    local db_host="localhost"

    if PGPASSWORD="$db_password" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "postgres" -c "SELECT 1" &> /dev/null; then
        log_success "PostgreSQL 连接成功"
        # 检查数据库是否存在
        if ! PGPASSWORD="$db_password" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "postgres" -t -c "SELECT 1 FROM pg_database WHERE datname = '$db_name'" | grep -q 1; then
            log_warning "数据库 $db_name 不存在，正在创建..."
            PGPASSWORD="$db_password" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "postgres" -c "CREATE DATABASE \"$db_name\""
        fi
        log_success "数据库检查完成"
    else
        log_error "无法连接到 PostgreSQL，请检查服务是否启动"
        exit 1
    fi
}

# 检查 Redis 连接
check_redis() {
    log_info "检查 Redis 连接..."
    if redis-cli -h localhost -p 6379 ping &> /dev/null; then
        log_success "Redis 连接成功"
    else
        log_error "无法连接到 Redis，请检查服务是否启动"
        exit 1
    fi
}

# 设置环境变量
setup_env() {
    log_info "设置环境变量..."
    export SPRING_PROFILES_ACTIVE=dev
    export APP_PORT=8080
    export POSTGRES_DB=ai_studio
    export POSTGRES_USER=postgres
    export POSTGRES_PASSWORD=xx123456XX
    export POSTGRES_PORT=5432
    export POSTGRES_HOST=localhost
    export REDIS_HOST=localhost
    export REDIS_PORT=6379
    export JWT_SECRET=your-super-secret-key-change-in-production-minimum-256-bits-long
    export EMBEDDING_API_KEY=your-embedding-api-key-here
    export LLM_API_KEY=your-llm-api-key-here
    export UPLOAD_DIR="$PROJECT_DIR/uploads"

    # 确保上传目录存在
    mkdir -p "$UPLOAD_DIR"
}

# 编译后端
build_backend() {
    log_info "编译后端..."
    mvn clean package -DskipTests
    log_success "后端编译完成"
}

# 启动后端服务
start_backend() {
    log_info "启动后端服务..."
    # 检查后端是否已在运行
    if [ -f "backend.pid" ]; then
        local backend_pid=$(cat "backend.pid" 2>/dev/null)
        if [ -n "$backend_pid" ] && kill -0 $backend_pid 2>/dev/null; then
            log_warning "后端服务已在运行（PID: $backend_pid）"
            return
        fi
        rm "backend.pid" 2>/dev/null || true
    fi

    nohup mvn spring-boot:run > "$PROJECT_DIR/logs/backend.log" 2>&1 &
    echo $! > "$PROJECT_DIR/backend.pid"

    # 等待服务启动
    log_info "等待后端服务启动..."
    local timeout=60
    local count=0
    while [ $count -lt $timeout ]; do
        if curl -s http://localhost:8080/actuator/health &> /dev/null; then
            log_success "后端服务启动成功（PID: $(cat backend.pid)）"
            return
        fi
        count=$((count + 2))
        sleep 2
    done

    log_error "后端服务启动超时，请检查 logs/backend.log"
    cat "$PROJECT_DIR/logs/backend.log" | tail -50
    rm "backend.pid" 2>/dev/null || true
    exit 1
}

# 启动前端开发服务器
start_frontend_dev() {
    log_info "启动前端开发服务器..."
    # 检查前端是否已在运行
    if [ -f "frontend.pid" ]; then
        local frontend_pid=$(cat "frontend.pid" 2>/dev/null)
        if [ -n "$frontend_pid" ] && kill -0 $frontend_pid 2>/dev/null; then
            log_warning "前端服务已在运行（PID: $frontend_pid）"
            return
        fi
        rm "frontend.pid" 2>/dev/null || true
    fi

    cd "$PROJECT_DIR/frontend"
    if [ ! -d "node_modules" ]; then
        log_info "安装前端依赖..."
        npm install
    fi
    nohup npm run dev > "$PROJECT_DIR/logs/frontend.log" 2>&1 &
    echo $! > "$PROJECT_DIR/frontend.pid"

    # 等待前端服务启动
    log_info "等待前端开发服务器启动..."
    local timeout=30
    local count=0
    while [ $count -lt $timeout ]; do
        if curl -s http://localhost:3000 &> /dev/null; then
            log_success "前端开发服务器已启动（PID: $(cat ../frontend.pid)）"
            cd "$PROJECT_DIR"
            return
        fi
        count=$((count + 2))
        sleep 2
    done

    log_warning "前端服务器可能需要更长时间启动，请检查 logs/frontend.log"
    cd "$PROJECT_DIR"
}

# 检查服务状态
check_services() {
    log_info "检查服务状态..."

    local all_running=true

    if [ -f "backend.pid" ]; then
        local backend_pid=$(cat "backend.pid" 2>/dev/null)
        if [ -n "$backend_pid" ] && kill -0 $backend_pid 2>/dev/null; then
            log_success "后端服务正在运行（PID: $backend_pid）"
            if curl -s http://localhost:8080/actuator/health &> /dev/null; then
                log_cyan "  API 状态: 健康"
            else
                log_error "  API 状态: 异常"
                all_running=false
            fi
        else
            log_error "后端服务未运行"
            all_running=false
        fi
    else
        log_error "后端服务未运行"
        all_running=false
    fi

    if [ -f "frontend.pid" ]; then
        local frontend_pid=$(cat "frontend.pid" 2>/dev/null)
        if [ -n "$frontend_pid" ] && kill -0 $frontend_pid 2>/dev/null; then
            log_success "前端服务正在运行（PID: $frontend_pid）"
            if curl -s http://localhost:3000 &> /dev/null; then
                log_cyan "  页面状态: 可访问"
            else
                log_error "  页面状态: 异常"
                all_running=false
            fi
        else
            log_error "前端服务未运行"
            all_running=false
        fi
    else
        log_error "前端服务未运行"
        all_running=false
    fi

    if [ "$all_running" = true ]; then
        log_cyan "所有服务运行正常"
    fi
}

# 停止服务
stop_services() {
    log_info "停止服务..."

    if [ -f "backend.pid" ]; then
        local backend_pid=$(cat "backend.pid" 2>/dev/null)
        if [ -n "$backend_pid" ]; then
            log_info "停止后端服务（PID: $backend_pid）"
            kill $backend_pid 2>/dev/null || true
            sleep 2
            rm "backend.pid" 2>/dev/null || true
        fi
    fi

    if [ -f "frontend.pid" ]; then
        local frontend_pid=$(cat "frontend.pid" 2>/dev/null)
        if [ -n "$frontend_pid" ]; then
            log_info "停止前端服务（PID: $frontend_pid）"
            kill $frontend_pid 2>/dev/null || true
            sleep 2
            rm "frontend.pid" 2>/dev/null || true
        fi
    fi

    # 清理可能存在的子进程
    pkill -f "mvn spring-boot:run" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true

    log_success "所有服务已停止"
}

# 重启服务
restart_services() {
    log_cyan "正在重启 AI Studio 开发环境..."
    echo "==========================================="
    stop_services
    sleep 3
    check_dependencies
    check_java_version
    check_postgres
    check_redis
    setup_env
    start_backend
    start_frontend_dev
    show_access_info
}

# 重启后端服务
restart_backend() {
    log_cyan "正在重启后端服务..."
    # 停止后端
    if [ -f "backend.pid" ]; then
        local backend_pid=$(cat "backend.pid" 2>/dev/null)
        if [ -n "$backend_pid" ]; then
            log_info "停止后端服务（PID: $backend_pid）"
            kill $backend_pid 2>/dev/null || true
            sleep 2
            rm "backend.pid" 2>/dev/null || true
        fi
    fi
    pkill -f "mvn spring-boot:run" 2>/dev/null || true

    # 启动后端
    check_dependencies
    check_java_version
    check_postgres
    check_redis
    setup_env
    start_backend
    log_info "后端服务访问地址：http://localhost:8080"
}

# 重启前端服务
restart_frontend() {
    log_cyan "正在重启前端开发服务器..."
    # 停止前端
    if [ -f "frontend.pid" ]; then
        local frontend_pid=$(cat "frontend.pid" 2>/dev/null)
        if [ -n "$frontend_pid" ]; then
            log_info "停止前端服务（PID: $frontend_pid）"
            kill $frontend_pid 2>/dev/null || true
            sleep 2
            rm "frontend.pid" 2>/dev/null || true
        fi
    fi
    pkill -f "vite" 2>/dev/null || true

    # 启动前端
    check_dependencies
    setup_env
    start_frontend_dev
    log_info "前端访问地址：http://localhost:3000"
}

# 显示访问信息
show_access_info() {
    echo
    log_info "服务访问信息："
    echo "==========================================="
    log_success "后端 API:    http://localhost:8080"
    log_success "API 文档:    http://localhost:8080/swagger-ui.html"
    log_success "健康检查:    http://localhost:8080/actuator/health"
    log_success "前端页面:    http://localhost:3000"
    log_warning "注意：开发阶段使用前端开发服务器（3000端口）"
    log_warning "      前端会将 /api 请求代理到后端 8080 端口"
    echo
    log_info "默认登录账号："
    log_cyan "  用户名: admin"
    log_cyan "  密码: 123456"
    echo "==========================================="
    echo
}

# 显示帮助信息
show_help() {
    echo "AI Studio 本机启动脚本 - 优化版"
    echo
    echo "这是一个前后端分离架构的项目："
    echo "  - 后端: Spring Boot (端口 8080)"
    echo "  - 前端: Vue 3 + Vite (端口 3000)"
    echo "  - 前端代理: http://localhost:3000/api -> http://localhost:8080/api"
    echo
    echo "用法: $0 [选项]"
    echo
    echo "选项:"
    echo "  start            启动后端和前端开发服务器"
    echo "  start-backend    只启动后端服务"
    echo "  start-frontend   只启动前端开发服务器"
    echo "  restart          重启所有服务"
    echo "  restart-backend  只重启后端服务"
    echo "  restart-frontend 只重启前端开发服务器"
    echo "  build            编译后端（跳过测试）"
    echo "  stop             停止所有服务"
    echo "  status           检查服务状态"
    echo "  logs [backend|frontend|all] 查看服务日志"
    echo "  help             显示帮助信息"
    echo
    echo "示例:"
    echo "  $0 start              启动所有服务"
    echo "  $0 restart            重启所有服务"
    echo "  $0 restart-backend    只重启后端"
    echo "  $0 stop               停止所有服务"
    echo "  $0 logs backend       查看后端日志"
    echo "  $0 logs frontend      查看前端日志"
    echo "  $0 logs all           查看所有日志"
    echo "  $0 status             检查服务状态"
}

# 查看日志
show_logs() {
    local service="${1:-all}"
    case "$service" in
        backend)
            log_info "显示后端日志（实时）："
            tail -f "$PROJECT_DIR/logs/backend.log"
            ;;
        frontend)
            log_info "显示前端日志（实时）："
            tail -f "$PROJECT_DIR/logs/frontend.log"
            ;;
        all)
            log_info "显示后端日志："
            if [ -f "$PROJECT_DIR/logs/backend.log" ]; then
                tail -20 "$PROJECT_DIR/logs/backend.log"
                echo
            fi

            log_info "显示前端日志："
            if [ -f "$PROJECT_DIR/logs/frontend.log" ]; then
                tail -20 "$PROJECT_DIR/logs/frontend.log"
            fi
            ;;
        *)
            log_error "未知服务: $service，支持 backend、frontend 或 all"
            ;;
    esac
}

# 主函数
main() {
    case "${1:-help}" in
        start)
            log_cyan "正在启动 AI Studio 开发环境..."
            echo "==========================================="
            check_dependencies
            check_java_version
            check_postgres
            check_redis
            setup_env
            start_backend
            start_frontend_dev
            show_access_info
            ;;
        start-backend)
            log_cyan "正在启动后端服务..."
            check_dependencies
            check_java_version
            check_postgres
            check_redis
            setup_env
            start_backend
            log_info "后端服务访问地址：http://localhost:8080"
            ;;
        start-frontend)
            log_cyan "正在启动前端开发服务器..."
            check_dependencies
            setup_env
            start_frontend_dev
            log_info "前端访问地址：http://localhost:3000"
            ;;
        restart)
            restart_services
            ;;
        restart-backend)
            restart_backend
            ;;
        restart-frontend)
            restart_frontend
            ;;
        build)
            log_cyan "正在编译后端..."
            check_dependencies
            check_java_version
            setup_env
            build_backend
            ;;
        stop)
            stop_services
            ;;
        status)
            check_services
            ;;
        logs)
            show_logs "$2"
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

# 确保日志目录存在
mkdir -p "$PROJECT_DIR/logs"

# 执行主函数
main "$@"
