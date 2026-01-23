# 前端缓存使用指南

## 概述

AI Studio 前端实现了多层缓存策略以提升性能：

1. **构建优化**：代码分割、懒加载、vendor chunks
2. **API 响应缓存**：内存缓存，减少重复请求
3. **浏览器缓存**：hash 文件名，支持长期缓存

## 构建优化

### 代码分割

项目使用 Vite 进行代码分割，将应用拆分为多个小 chunks：

- **主入口**: `index-[hash].js` - 仅包含应用初始化代码（~45KB）
- **Vue 生态**: `vue-vendor-[hash].js` - Vue、Vue Router、Pinia（~108KB）
- **Element Plus**: `element-plus-[hash].js` - UI 组件库（~1MB）
- **ECharts**: `echarts-[hash].js` - 图表库（~500KB）
- **页面组件**: 每个路由页面独立打包

### 懒加载

所有路由组件使用动态导入，实现按需加载：

```typescript
component: () => import('@/views/DashboardView.vue')
```

### 构建配置

Vite 配置文件：`frontend/vite.config.ts`

主要优化：
- Manual chunks 分离 vendor 代码
- Terser 压缩和移除 console
- 文件名 hash 化支持长期缓存
- Chunk 大小警告阈值：1000KB

## API 响应缓存

### 工作原理

前端使用内存缓存存储 API 响应：

- **缓存位置**: `frontend/src/utils/cache.ts`
- **默认 TTL**: 5 分钟
- **缓存对象**: 仅 GET 请求
- **缓存键**: URL + params

### 自动缓存

GET 请求自动缓存，无需额外配置：

```typescript
// 自动缓存，默认 5 分钟
const data = await api.getKnowledgeBases()
```

### 自定义缓存时间

通过 `cacheTTL` 配置自定义缓存时间：

```typescript
const data = await api.getKnowledgeBases(params, {
  cacheTTL: 10 * 60 * 1000 // 10 分钟
})
```

### 跳过缓存

使用 `X-Skip-Cache` header 强制跳过缓存：

```typescript
const data = await api.getKnowledgeBases(params, {
  headers: { 'X-Skip-Cache': 'true' }
})
```

### 手动清除缓存

使用 `apiCache` 工具手动管理缓存：

```typescript
import { apiCache } from '@/utils/cache'

// 清除所有缓存
apiCache.clear()

// 清除特定模式的缓存
apiCache.clear('/api/knowledge-bases')
```

## 缓存配置文件

### API 缓存配置

文件：`frontend/src/api/request.ts`

拦截器配置：
- 请求拦截器：检查缓存，命中则返回
- 响应拦截器：存储 GET 请求响应
- 错误拦截器：处理缓存响应

### 缓存工具类

文件：`frontend/src/utils/cache.ts`

提供方法：
- `get(key)`: 获取缓存
- `set(key, data, ttl)`: 设置缓存
- `clear(pattern)`: 清除缓存
- `clearExpired()`: 清理过期缓存

## 最佳实践

### 何时使用缓存

**适合缓存的数据：**
- ✅ 用户信息、角色（不常变化）
- ✅ 知识库、Agent、Chatbot 列表（配置数据）
- ✅ 系统配置、选项（静态数据）

**不适合缓存的数据：**
- ❌ 聊天消息（实时更新）
- ❌ Agent 执行结果（动态生成）
- ❌ 文件上传进度（实时变化）

### TTL 设置建议

- 静态数据：10-15 分钟
- 配置数据：5-10 分钟
- 列表数据：3-5 分钟
- 用户数据：5 分钟

### 缓存失效策略

以下操作会自动跳过缓存：
- POST/PUT/DELETE 请求
- 带 `X-Skip-Cache` header 的请求
- 缓存过期（TTL 超时）

手动清除缓存的场景：
- 用户更新数据后
- 切换账户后
- 检测到数据更新时

## 性能优化效果

### 构建优化成果

| 指标 | 优化前 | 优化后 | 减少 |
|------|--------|--------|------|
| 主入口大小 | 1,218 KB | 45.69 KB | 96.2% |
| Dashboard 大小 | 510 KB | 10.33 KB | 97.9% |
| 首屏加载体积 | ~1.2 MB | ~45 KB | 96% |

### API 缓存收益

- **减少网络请求**：重复请求直接返回缓存
- **降低服务器负载**：减少后端查询
- **提升响应速度**：内存缓存 ~1ms vs 网络 ~100-500ms
- **改善用户体验**：页面切换更流畅

## 监控和调试

### 查看缓存命中率

在浏览器开发工具中：
1. 打开 Network 标签
2. 观察 API 请求
3. 缓存命中：请求被拦截，无网络流量
4. 缓存未命中：正常发送请求

### 调试缓存行为

在 `frontend/src/api/request.ts` 中添加日志：

```typescript
// 缓存命中
if (cachedData) {
  console.log('[Cache Hit]', cacheKey)
  // ...
}

// 缓存未命中
console.log('[Cache Miss]', cacheKey)
```

### 清除缓存测试

在浏览器控制台：

```javascript
// 刷新页面清除所有缓存
location.reload()

// 手动调用清除方法（需要在应用上下文中）
window.apiCache?.clear()
```

## 故障排查

### 缓存未生效

**检查项：**
1. 确认是 GET 请求
2. 检查是否有 `X-Skip-Cache` header
3. 确认缓存未过期（TTL）
4. 查看控制台是否有错误

### 数据未更新

**解决方案：**
1. 使用 `X-Skip-Cache` header 跳过缓存
2. 手动清除缓存：`apiCache.clear()`
3. 刷新页面
4. 等待 TTL 过期

### 内存占用过高

**检查项：**
1. 缓存数据量是否过大
2. TTL 设置是否过长
3. 是否有内存泄漏

**解决方案：**
1. 减少缓存数据量
2. 缩短 TTL
3. 定期清理过期缓存（已自动执行）

## 相关文件

- `frontend/vite.config.ts` - 构建优化配置
- `frontend/src/utils/cache.ts` - 缓存工具类
- `frontend/src/api/request.ts` - API 拦截器
- `frontend/src/router/index.ts` - 路由懒加载
- `frontend/docs/performance-analysis.md` - 性能分析报告

## 参考资源

- [Vite 构建优化](https://vitejs.dev/guide/build.html)
- [Vue Router 懒加载](https://router.vuejs.org/guide/advanced/lazy-loading.html)
- [Axios 拦截器](https://axios-http.com/docs/interceptors)
