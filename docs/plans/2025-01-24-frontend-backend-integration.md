# 前后端一体化打包实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 将前端 Vue 3 应用与后端 Spring Boot 打包到单个可执行 JAR 中，通过 `java -jar` 启动完整应用

**架构:** 使用 Maven frontend-maven-plugin 在构建时执行 `npm run build`，将前端静态资源复制到 Spring Boot 的 `classpath:/static/`，由 Spring Boot 提供静态文件服务

**技术栈:** Maven, Node.js, Spring Boot, Vite

---

## Task 1: 创建打包脚本 package.sh

**文件:**
- Create: `package.sh`

**步骤 1: 创建打包脚本**

```bash
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
```

**步骤 2: 设置可执行权限**

```bash
chmod +x package.sh
```

**步骤 3: 验证脚本创建成功**

```bash
ls -la package.sh
```

预期输出: 脚本文件存在且具有可执行权限

**步骤 4: 提交**

```bash
git add package.sh
git commit -m "feat: 添加前后端一体化打包脚本"
```

---

## Task 2: 修改 pom.xml - 添加属性和插件依赖

**文件:**
- Modify: `pom.xml`

**步骤 1: 在 <properties> 部分添加 Node 版本和跳过构建选项**

在 `<properties>` 标签内的最后添加（约第 27 行后）:

```xml
        <node.version>v20.11.0</node.version>
        <npm.version>10.2.4</npm.version>
```

**步骤 2: 在 <build><plugins> 部分添加 frontend-maven-plugin**

在 `spring-boot-maven-plugin` 前添加（约第 190 行前）:

```xml
            <!-- Frontend Maven Plugin - 自动构建前端 -->
            <plugin>
                <groupId>com.github.eirslett</groupId>
                <artifactId>frontend-maven-plugin</artifactId>
                <version>1.15.0</version>
                <configuration>
                    <workingDirectory>frontend</workingDirectory>
                    <nodeVersion>${node.version}</nodeVersion>
                    <npmVersion>${npm.version}</npmVersion>
                    <skip>${skipFrontend}</skip>
                </configuration>
                <executions>
                    <execution>
                        <id>install node and npm</id>
                        <goals>
                            <goal>install-node-and-npm</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>npm install</id>
                        <goals>
                            <goal>npm</goal>
                        </goals>
                        <configuration>
                            <arguments>install --legacy-peer-deps</arguments>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm run build</id>
                        <goals>
                            <goal>npm</goal>
                        </goals>
                        <configuration>
                            <arguments>run build</arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

**步骤 3: 在 <build><plugins> 部分添加 maven-resources-plugin 复制前端资源**

在 `frontend-maven-plugin` 后添加（约第 225 行后）:

```xml
            <!-- Maven Resources Plugin - 复制前端构建产物到 static -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <executions>
                    <execution>
                        <id>copy-frontend-resources</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>frontend/dist</directory>
                                    <filtering>false</filtering>
                                </resource>
                            </resources>
                            <skip>${skipFrontend}</skip>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

**步骤 4: 在 <build><plugins> 部分添加 maven-clean-plugin 清理前端产物**

在 `maven-resources-plugin` 后添加（约第 250 行后）:

```xml
            <!-- Maven Clean Plugin - 清理时删除前端构建产物 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-clean-plugin</artifactId>
                <version>3.3.2</version>
                <configuration>
                    <filesets>
                        <fileset>
                            <directory>frontend/dist</directory>
                            <includes>
                                <include>**/*</include>
                            </includes>
                        </fileset>
                    </filesets>
                </configuration>
            </plugin>
```

**步骤 5: 验证 pom.xml 语法正确**

```bash
mvn validate
```

预期输出: `BUILD SUCCESS`

**步骤 6: 提交**

```bash
git add pom.xml
git commit -m "feat: 添加前端构建插件配置"
```

---

## Task 3: 修改 WebConfig.java - 添加 SPA 路由转发

**文件:**
- Modify: `src/main/java/com/mydotey/ai/studio/config/WebConfig.java`

**步骤 1: 添加必要的 import**

在现有 import 后添加（约第 12 行后）:

```java
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
```

**步骤 2: 让 WebConfig 实现 WebMvcConfigurer**

修改类声明（约第 14 行）:

```java
public class WebConfig implements WebMvcConfigurer {
```

**步骤 3: 添加静态资源处理方法**

在 `corsFilter()` 方法后添加（约第 44 行后）:

```java
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 注册静态资源处理器，支持 SPA 的 history 模式
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600) // 缓存 1 小时
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 如果请求的资源存在，则返回该资源
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 如果资源不存在，返回 index.html（用于 SPA 路由）
                        if (!resourcePath.endsWith(".html")
                                && !resourcePath.contains(".")) {
                            return location.createRelative("index.html");
                        }

                        // 其他情况返回 null（404）
                        return null;
                    }
                });
    }
```

**步骤 4: 验证代码编译通过**

```bash
mvn compile -DskipFrontend
```

预期输出: `BUILD SUCCESS`

**步骤 5: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/config/WebConfig.java
git commit -m "feat: 添加 SPA 路由转发支持"
```

---

## Task 4: 验证完整构建流程

**文件:**
- 无

**步骤 1: 清理并执行完整构建**

```bash
./package.sh
```

预期输出: `BUILD SUCCESS`，最终生成 `target/ai-studio-1.0.0.jar`

**步骤 2: 验证 JAR 内容包含静态资源**

```bash
jar tf target/ai-studio-1.0.0.jar | grep "static/index.html"
```

预期输出: `BOOT-INF/classes/static/index.html`

**步骤 3: 验证前端 JS 文件已打包**

```bash
jar tf target/ai-studio-1.0.0.jar | grep "static/js"
```

预期输出: 包含多个 `.js` 文件

**步骤 4: 启动应用验证**

```bash
java -jar target/ai-studio-1.0.0.jar &
sleep 5
curl -I http://localhost:8080/
```

预期输出: HTTP 200

**步骤 5: 验证 API 可用**

```bash
curl -I http://localhost:8080/actuator/health
```

预期输出: HTTP 200

**步骤 6: 停止应用**

```bash
pkill -f "ai-studio-1.0.0.jar"
```

**步骤 7: 提交（如需更新）**

```bash
git add .
git commit -m "test: 验证前后端一体化打包完成"
```

---

## Task 5: 验证跳过前端构建选项

**文件:**
- 无

**步骤 1: 清理并执行跳过前端构建**

```bash
mvn clean package -DskipFrontend
```

预期输出: `BUILD SUCCESS`，且不执行 npm 构建步骤

**步骤 2: 验证静态资源不存在**

```bash
jar tf target/ai-studio-1.0.0.jar | grep "static" || echo "No static resources (expected)"
```

预期输出: `No static resources (expected)`

**步骤 3: 使用脚本验证跳过选项**

```bash
./package.sh --skip-frontend
```

预期输出: 构建成功，输出显示 "跳过前端构建"

---

## Task 6: 创建使用文档

**文件:**
- Create: `docs/FRONTEND_BACKEND_INTEGRATION.md`

**步骤 1: 创建文档**

```markdown
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
```

**步骤 2: 验证文档创建成功**

```bash
cat docs/FRONTEND_BACKEND_INTEGRATION.md | head -20
```

预期输出: 文档前 20 行内容

**步骤 3: 提交**

```bash
git add docs/FRONTEND_BACKEND_INTEGRATION.md
git commit -m "docs: 添加前后端一体化打包使用文档"
```

---

## 验收标准

1. ✅ 执行 `./package.sh` 成功生成 JAR 文件
2. ✅ JAR 文件包含前端静态资源（`BOOT-INF/classes/static/`）
3. ✅ 执行 `java -jar ai-studio-1.0.0.jar` 启动应用
4. ✅ 访问 http://localhost:8080 返回前端页面
5. ✅ 访问 http://localhost:8080/api/* 正常调用后端 API
6. ✅ 前端路由（如 `/knowledge-base/123`）正确返回 `index.html`
7. ✅ `./package.sh --skip-frontend` 正确跳过前端构建
8. ✅ 所有变更已提交到 Git
