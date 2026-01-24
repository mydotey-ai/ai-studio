# 前后端一体化打包

## 概述

AI Studio 支持将前端 Vue 3 应用与后端 Spring Boot 打包到单个可执行 JAR 文件中。

## 构建命令

### 完整构建（包含前端）

```bash
./package.sh
```

或

```bash
mvn clean package
```

### 跳过前端构建（仅后端）

```bash
./package.sh --skip-frontend
```

或

```bash
mvn clean package -DskipFrontend
```

### 传递 Maven 选项

```bash
./package.sh -DskipTests
./package.sh --skip-frontend -DskipTests
```

## 运行应用

```bash
java -jar target/ai-studio-1.0.0.jar
```

### 指定端口

```bash
java -jar target/ai-studio-1.0.0.jar --server.port=9090
```

### 指定配置文件

```bash
java -jar target/ai-studio-1.0.0.jar --spring.profiles.active=prod
```

## 访问地址

- **前端应用**: http://localhost:8080
- **API 接口**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health

## 构建原理

1. Maven 构建时触发 `frontend-maven-plugin`
2. 下载指定版本的 Node.js 和 npm
3. 执行 `npm install` 安装前端依赖
4. 执行 `npm run build` 构建前端到 `frontend/dist`
5. 使用 `maven-resources-plugin` 将 `frontend/dist` 复制到 `target/classes/static`
6. Spring Boot 打包时包含 `static` 目录
7. 运行时 Spring Boot 从 `classpath:/static/` 提供静态文件服务

## SPA 路由支持

前端使用 Vue Router 的 history 模式，后端配置了 SPA 路由转发：
- 访问 `/knowledge-base/123` 等前端路由时，返回 `index.html`
- 静态资源（CSS、JS、图片等）直接返回
- API 请求（`/api/*`）由 Spring Boot Controller 处理

## 开发模式

开发模式下仍可使用前后端分离启动：

```bash
# 终端 1: 启动后端
mvn spring-boot:run

# 终端 2: 启动前端
cd frontend && npm run dev
```

前端开发服务器代理 API 请求到后端。
