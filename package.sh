#!/bin/bash

# 前后端一体化打包脚本
# 用法: ./package.sh [--skip-frontend] [maven-options]

set -e

# 解析参数
SKIP_FRONTEND=false
MAVEN_OPTS=""

for arg in "$@"; do
    case $arg in
        --skip-frontend)
            SKIP_FRONTEND=true
            shift
            ;;
        *)
            MAVEN_OPTS="$MAVEN_OPTS $arg"
            shift
            ;;
    esac
done

echo "======================================"
echo "  AI Studio 一体化打包"
echo "======================================"
echo ""

# 构建 JAR
echo "[1/2] 开始构建 Spring Boot JAR..."
if [ "$SKIP_FRONTEND" = true ]; then
    echo "  跳过前端构建 (-DskipFrontend)"
    mvn clean package -DskipFrontend $MAVEN_OPTS
else
    mvn clean package $MAVEN_OPTS
fi

echo ""
echo "[2/2] 构建完成！"
echo ""
echo "输出文件: target/ai-studio-1.0.0.jar"
echo ""
echo "运行应用:"
echo "  java -jar target/ai-studio-1.0.0.jar"
echo ""
echo "访问地址:"
echo "  前端: http://localhost:8080"
echo "  API:  http://localhost:8080/api"
echo ""
