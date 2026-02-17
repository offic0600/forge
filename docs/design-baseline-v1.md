# Forge Web IDE — 设计基线 v1

> 基线日期: 2026-02-18 | Phase 1.5 E2E 验证通过后冻结
> 本文档冻结当前已验证的 UI/API/数据模型/架构设计细节，作为未来修改的对照基准。
> 任何对本文档覆盖范围的修改，必须先意识到偏离、再决定是否接受。

---

## 一、UI/UX 设计基线

### 1.1 页面路由结构

| 路由 | 页面 | 源文件 | 说明 |
|------|------|--------|------|
| `/` | Dashboard | `src/app/page.tsx` | 欢迎页 + 快捷操作 + 最近项目 + 活动动态 |
| `/workspace/[id]` | IDE Workspace | `src/app/workspace/[id]/page.tsx` | 三面板 IDE：文件树 + 编辑器 + AI 聊天 |
| `/knowledge` | Knowledge Base | `src/app/knowledge/page.tsx` | 四标签页：Docs / Architecture / Services / APIs |
| `/workflows` | Workflow Editor | `src/app/workflows/page.tsx` | ReactFlow 可视化工作流编辑器 |

**根布局** (`src/app/layout.tsx`):
- `QueryClientProvider` (React Query 服务端状态)
- 全局 Header + 可折叠 Sidebar
- 默认暗色主题 (`<html className="dark">`)

### 1.2 关键组件布局

#### IDE Workspace（`/workspace/[id]`）— 三面板布局

```
┌─────────────────────────────────────────────────────────────────────┐
│ Header (全局)                                                       │
├──────────┬──────────────────────────────────┬───────────────────────┤
│ File     │ Monaco Editor                    │ AI Chat Sidebar       │
│ Explorer │   - 多标签页文件编辑               │   - 消息列表 + 自动滚动 │
│          │   - 25+ 语言语法高亮              │   - @ 提及附加上下文    │
│ (可折叠)  │   - Minimap + 括号匹配           │   - 流式响应展示       │
│          │   - "AI Explain" 按钮             │   - Tool Call 展开    │
│          ├──────────────────────────────────┤   - 会话管理          │
│          │ Terminal Panel (可折叠底部)        │                       │
│          │   - WebSocket 终端连接            │ (可折叠)               │
│          │   - 彩色输出                      │                       │
└──────────┴──────────────────────────────────┴───────────────────────┘
```

#### Knowledge Base（`/knowledge`）— 四标签页

| 标签页 | 组件 | 功能 |
|--------|------|------|
| Docs | `KnowledgeSearch` + `DocViewer` | 文档搜索 + 按类型过滤(Wiki/ADR/Runbook/API Doc) + Markdown 渲染 + 动态目录 |
| Architecture | `ArchDiagramViewer` | Mermaid 图渲染 + 缩放(25%-300%) + 拖拽平移 + 导出 SVG |
| Services | `ServiceGraphViewer` | ReactFlow 服务依赖图 + 节点状态颜色(健康/退化/故障) + 影响分析模式 |
| APIs | `ApiExplorer` | API 目录 + 端点展开 + 参数文档 + "Try it out" 交互测试 |

### 1.3 交互流程

**核心用户旅程**：

```
Dashboard → 创建/选择 Workspace → 编辑文件（Monaco）
                                      │
                                      ├→ AI 对话（AiChatSidebar）
                                      │    ├→ WebSocket 优先连接
                                      │    ├→ 发送消息 + @ 附加上下文
                                      │    ├→ 流式接收 thinking/content/tool_use 事件
                                      │    └→ Tool Call 展示输入/输出
                                      │
                                      ├→ 知识浏览（Knowledge）
                                      │    ├→ 搜索文档 → 查看详情
                                      │    ├→ 浏览架构图 → 缩放/导出
                                      │    └→ 服务依赖图 → 影响分析
                                      │
                                      └→ 工作流编辑（Workflows）
                                           ├→ 拖放节点到画布
                                           ├→ 连线定义执行顺序
                                           └→ 运行 → 实时查看步骤执行
```

**组件间通信**：
- MonacoEditor → AiChatSidebar: 通过 `window.dispatchEvent(new CustomEvent('forge:ai-explain'))` 发送代码解释请求

### 1.4 样式系统

| 维度 | 选型 | 细节 |
|------|------|------|
| CSS 框架 | TailwindCSS 3.4+ | Utility-first, class-based dark mode |
| 组件模式 | CVA + tailwind-merge + clsx | class-variance-authority 管理组件变体 |
| 颜色体系 | HSL CSS 变量 | `--background`, `--foreground`, `--primary`, `--muted`, `--accent`, `--destructive` 等 |
| 品牌色 | `forge` 色阶 (50-950) | 蓝色渐变 |
| 正文字体 | Inter, system-ui | 无衬线 |
| 等宽字体 | JetBrains Mono, Fira Code | 代码编辑器 + 终端 |
| 自定义动画 | `animate-thinking-dot` | AI 思考指示器 (1.4s 循环) |
| 图标库 | lucide-react 0.460+ | 全局统一图标 |

### 1.5 关键前端依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Next.js | 15.1+ | App Router, SSR, standalone 输出 |
| React | 19.0+ | UI 核心 |
| @tanstack/react-query | 5.62+ | 服务端状态管理 + 缓存 |
| @monaco-editor/react | 4.6+ | 代码编辑器 |
| @xyflow/react | 12.3+ | 工作流画布 + 服务依赖图 |
| mermaid | 11.4+ | 架构图渲染 |
| zustand | 5.0+ | 客户端状态管理 |

---

## 二、API 契约基线

### 2.1 REST API 端点清单

#### Chat API (`AiChatController` → `/api/chat`)

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/api/chat/sessions` | `CreateChatSessionRequest { workspaceId }` | `ChatSession { id, workspaceId, userId, createdAt }` | 创建聊天会话 |
| GET | `/api/chat/sessions/{sessionId}/messages` | — | `List<ChatMessage>` | 获取历史消息 |
| POST | `/api/chat/sessions/{sessionId}/messages` | `ChatStreamMessage { type, content, contexts? }` | `ChatMessage` | 同步发送消息（非流式） |
| POST | `/api/chat/sessions/{sessionId}/stream` | `ChatStreamMessage { type, content, contexts? }` | SSE stream | 流式发送消息 |

#### Workspace API (`WorkspaceController` → `/api/workspaces`)

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/api/workspaces` | `CreateWorkspaceRequest { name, description?, repository?, branch?, template? }` | `Workspace` | 创建工作空间 |
| GET | `/api/workspaces` | — | `List<Workspace>` | 列出工作空间 |
| GET | `/api/workspaces/{id}` | — | `Workspace` | 获取工作空间 |
| DELETE | `/api/workspaces/{id}` | — | 204 | 删除工作空间 |
| POST | `/api/workspaces/{id}/activate` | — | `Workspace` | 激活工作空间 |
| POST | `/api/workspaces/{id}/suspend` | — | `Workspace` | 暂停工作空间 |
| GET | `/api/workspaces/{id}/files` | — | `List<FileNode>` | 获取文件树 |
| GET | `/api/workspaces/{id}/files/content?path=` | — | `String` | 读取文件内容 |
| PUT | `/api/workspaces/{id}/files/content` | `FileContentRequest { path, content }` | 200 | 保存文件 |
| POST | `/api/workspaces/{id}/files` | `FileContentRequest { path, content }` | 201 | 创建文件 |
| DELETE | `/api/workspaces/{id}/files?path=` | — | 204 | 删除文件 |

#### Knowledge API (`KnowledgeController` → `/api/knowledge`)

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| GET | `/api/knowledge/search?q=&type=&limit=` | — | `List<KnowledgeDocument>` | 搜索知识文档 |
| GET | `/api/knowledge/docs/{id}` | — | `KnowledgeDocument` | 获取文档详情 |
| GET | `/api/knowledge/services` | — | `ServiceGraph` | 获取服务依赖图 |
| GET | `/api/knowledge/apis` | — | `List<ApiService>` | 获取 API 目录 |
| POST | `/api/knowledge/apis/try` | `ApiTryRequest { method, url, headers?, body? }` | `Any` | 测试 API 调用 |
| GET | `/api/knowledge/diagrams` | — | `List<ArchDiagram>` | 获取架构图列表 |

#### MCP API (`McpController` → `/api/mcp`)

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| GET | `/api/mcp/tools` | — | `List<McpTool>` | 列出可用工具 |
| POST | `/api/mcp/tools/call` | `McpToolCallRequest { name, arguments }` | `McpToolCallResponse` | 调用工具 |
| POST | `/api/mcp/tools/cache/invalidate` | — | `{ status: "cache_invalidated" }` | 清除工具缓存 |

#### Workflow API (`WorkflowController` → `/api/workflows`)

| 方法 | 路径 | 请求体 | 响应体 | 说明 |
|------|------|--------|--------|------|
| POST | `/api/workflows` | `CreateWorkflowRequest { name, description?, nodes?, edges? }` | `Workflow` | 创建工作流 |
| GET | `/api/workflows` | — | `List<Workflow>` | 列出工作流 |
| GET | `/api/workflows/{id}` | — | `Workflow` | 获取工作流 |
| PUT | `/api/workflows/{id}` | `UpdateWorkflowRequest { name?, description?, nodes?, edges? }` | `Workflow` | 更新工作流 |
| DELETE | `/api/workflows/{id}` | — | 204 | 删除工作流 |
| POST | `/api/workflows/{id}/run` | — | `WorkflowExecutionResult` | 运行工作流 |
| GET | `/api/workflows/{id}/runs` | — | `List<WorkflowExecutionResult>` | 查看运行历史 |

### 2.2 WebSocket 协议

| WebSocket | 路径 | 方向 | 消息格式 |
|-----------|------|------|---------|
| AI Chat | `/ws/chat/{sessionId}` | Client→Server | `{ type: "message", content: string, contexts: ContextReference[] }` |
| | | Server→Client | `StreamEvent` JSON (每行一个事件) |
| Terminal | `/ws/terminal/{workspaceId}` | 双向 | 终端输入/输出文本 |
| Workflow | `/ws/workflow/{id}` | Server→Client | 工作流执行步骤事件 |

### 2.3 SSE 事件格式

SSE 端点: `POST /api/chat/sessions/{sessionId}/stream`

**StreamEvent 类型**:

```typescript
type StreamEvent =
  | { type: "thinking", content?: string }        // AI 思考过程
  | { type: "content", content?: string }          // 文本输出增量
  | { type: "tool_use_start", toolCallId?: string, toolName?: string }  // Tool 调用开始
  | { type: "tool_use", toolCallId?: string, toolName?: string, toolInput?: object }  // Tool 调用完整
  | { type: "tool_result", toolCallId?: string, content?: string, durationMs?: number }  // Tool 执行结果
  | { type: "error", content?: string }            // 错误
  | { type: "done" }                               // 流结束
```

**传输方式**:
- SSE: `data: {JSON}\n\n` 格式，`data: [DONE]` 结束
- WebSocket: 每行一个 JSON 对象

### 2.4 关键 DTO 结构

源文件: `web-ide/backend/src/main/kotlin/com/forge/webide/model/Models.kt`

**核心 DTO**:

```kotlin
// Chat
data class ChatStreamMessage(val type: String, val content: String, val contexts: List<ContextReference>?)
data class ContextReference(val type: String, val id: String, val content: String?)
data class ToolCallRecord(val id: String, val name: String, val input: Map<String, Any?>, val output: String?, val status: String)

// Workspace
data class Workspace(val id: String, val name: String, val description: String, val status: WorkspaceStatus, val owner: String, val repository: String?, val branch: String?)
enum class WorkspaceStatus { CREATING, ACTIVE, SUSPENDED, ERROR }
data class FileNode(val name: String, val path: String, val type: FileType, val size: Long?, val children: List<FileNode>?)

// Knowledge
data class KnowledgeDocument(val id: String, val title: String, val type: DocumentType, val content: String, val snippet: String, val author: String, val tags: List<String>)
enum class DocumentType { WIKI, ADR, RUNBOOK, API_DOC }

// MCP
data class McpTool(val name: String, val description: String, val inputSchema: Map<String, Any?>)
data class McpToolCallRequest(val name: String, val arguments: Map<String, Any?>)
data class McpToolCallResponse(val content: List<McpContent>, val isError: Boolean)
```

---

## 三、数据模型基线

### 3.1 JPA Entity 结构

源文件: `web-ide/backend/src/main/kotlin/com/forge/webide/entity/`

#### ChatSessionEntity

```kotlin
@Entity @Table(name = "chat_sessions")
class ChatSessionEntity(
    @Id val id: String,                              // UUID
    @Column(name = "workspace_id") val workspaceId: String,
    @Column(name = "user_id") val userId: String,
    @Column(name = "created_at") val createdAt: Instant,
    @Column(name = "updated_at") var updatedAt: Instant,
    @OneToMany(mappedBy = "sessionId", cascade = ALL, fetch = LAZY)
    val messages: MutableList<ChatMessageEntity>
)
```

#### ChatMessageEntity

```kotlin
@Entity @Table(name = "chat_messages")
class ChatMessageEntity(
    @Id val id: String,                              // UUID
    @Column(name = "session_id") val sessionId: String,  // FK → chat_sessions
    @Column(name = "role", length = 20) val role: String,  // "user" | "assistant" | "tool" | "system"
    @Column(name = "content", length = 1_000_000) val content: String,
    @Column(name = "created_at") val createdAt: Instant,
    @OneToMany(mappedBy = "messageId", cascade = ALL, fetch = LAZY)
    val toolCalls: MutableList<ToolCallEntity>
)
```

#### ToolCallEntity

```kotlin
@Entity @Table(name = "tool_calls")
class ToolCallEntity(
    @Id val id: String,                              // UUID
    @Column(name = "message_id") val messageId: String,   // FK → chat_messages
    @Column(name = "tool_name") val toolName: String,
    @Column(name = "input", length = 1_000_000) val input: String?,
    @Column(name = "output", length = 1_000_000) val output: String?,
    @Column(name = "status", length = 20) val status: String,  // "complete" | "error"
    @Column(name = "duration_ms") val durationMs: Long?
)
```

### 3.2 关系图

```
chat_sessions (1) ──→ (N) chat_messages (1) ──→ (N) tool_calls
     │                        │                        │
     ├─ id (PK)              ├─ id (PK)              ├─ id (PK)
     ├─ workspace_id         ├─ session_id (FK)      ├─ message_id (FK)
     ├─ user_id              ├─ role                 ├─ tool_name
     ├─ created_at           ├─ content (TEXT)       ├─ input (TEXT)
     └─ updated_at           └─ created_at           ├─ output (TEXT)
                                                      ├─ status
                                                      └─ duration_ms
```

### 3.3 Flyway 迁移

| 版本 | 文件 | 内容 |
|------|------|------|
| V1 | `V1__create_chat_tables.sql` | 创建 `chat_sessions` + `chat_messages` + `tool_calls` 三张表 + 索引 |

**索引**:
- `idx_sessions_workspace` → `chat_sessions(workspace_id)`
- `idx_messages_session` → `chat_messages(session_id)`
- `idx_tool_calls_message` → `tool_calls(message_id)`

### 3.4 数据库配置

- 开发/试用: H2 内存数据库 (Spring Boot 默认)
- 生产就绪: PostgreSQL (已声明 `runtimeOnly("org.postgresql:postgresql")`)
- ORM: Spring Data JPA + Hibernate
- 迁移: Flyway Core

---

## 四、架构决策基线

### 4.1 部署架构

```
┌─────────────────────────────────────────────────────┐
│                    用户浏览器                         │
│              http://localhost:9000                    │
└───────────────────────┬─────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────┐
│                  Nginx (:9000)                        │
│  反向代理 — 统一入口                                   │
│                                                       │
│  /api/*      → backend:8080    (REST + SSE)          │
│  /ws/*       → backend:8080    (WebSocket Upgrade)   │
│  /actuator/* → backend:8080    (健康检查)             │
│  /h2-console/* → backend:8080  (数据库控制台)         │
│  /*          → frontend:3000   (Next.js, catch-all)  │
└─────────┬──────────────────────┬────────────────────┘
          │                      │
┌─────────▼──────────┐ ┌────────▼──────────┐
│ Backend (:8080)    │ │ Frontend (:3000)  │
│ Spring Boot 3      │ │ Next.js 15        │
│ Kotlin + JDK 21    │ │ standalone mode   │
│ H2 / PostgreSQL    │ │ React 19          │
│ WebSocket + SSE    │ │ Monaco Editor     │
└────────────────────┘ └───────────────────┘
```

### 4.2 Nginx 路由规则（5 条）

源文件: `infrastructure/docker/nginx-trial.conf`

| # | 路径匹配 | 目标 | 特殊配置 |
|---|---------|------|---------|
| 1 | `/api/` | `backend:8080` | `proxy_buffering off` (SSE), `proxy_read_timeout 3600s` |
| 2 | `/ws/` | `backend:8080` | `proxy_http_version 1.1`, `Upgrade` + `Connection` headers |
| 3 | `/actuator/` | `backend:8080` | 标准代理 |
| 4 | `/h2-console/` | `backend:8080` | 标准代理 |
| 5 | `/` (catch-all) | `frontend:3000` | 标准代理 |

### 4.3 Docker 部署策略

源文件: `infrastructure/docker/docker-compose.trial.yml`

**策略**: 本地构建 → Docker 只打包运行

```bash
# 1. 本地构建（避免 Docker 内 TLS/网络问题）
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew :web-ide:backend:bootJar -x test --no-daemon
cd web-ide/frontend && npm install && npm run build

# 2. Docker 打包运行
docker compose -f docker-compose.trial.yml up --build
```

**三容器**:
- `backend`: Spring Boot JAR on JRE Alpine
- `frontend`: Next.js standalone on Node 20
- `nginx`: Alpine, 挂载 nginx-trial.conf

**健康检查**: `wget --spider http://localhost:8080/api/knowledge/search` (15s 间隔, 5 次重试, 30s 启动等待)

### 4.4 前后端通信方式

**优先级**: WebSocket → SSE fallback → HTTP 同步兜底

```
前端 ClaudeClient.streamMessage()
    │
    ├─ 尝试 WebSocket (ws://host/ws/chat/{sessionId})
    │    ├─ 成功: 全双工实时通信
    │    └─ 失败: 降级到 SSE
    │
    ├─ SSE Fallback (POST /api/chat/sessions/{sessionId}/stream)
    │    ├─ 成功: Server-Sent Events 流
    │    └─ 失败: 降级到 HTTP
    │
    └─ HTTP 同步 (POST /api/chat/sessions/{sessionId}/messages)
         └─ 等待完整响应返回
```

### 4.5 后端 Agentic Loop

源文件: `web-ide/backend/src/main/kotlin/com/forge/webide/service/ClaudeAgentService.kt`

```
用户消息 → 构建上下文 (contextual message + history)
    │
    ▼
agenticStream() — 最多 MAX_AGENTIC_TURNS 轮
    │
    ├─ 每轮: ClaudeAdapter.streamWithTools() → 实时发送事件到客户端
    │
    ├─ 如果 stop_reason == TOOL_USE:
    │    ├─ 执行工具 (McpProxyService.callTool)
    │    ├─ 发送 tool_result 事件
    │    └─ 继续下一轮
    │
    └─ 如果 stop_reason == END_TURN:
         ├─ 持久化消息和 tool calls
         ├─ 知识空白检测 (KnowledgeGapDetectorService)
         └─ 发送 done 事件
```

### 4.6 Spring Security 配置

**当前状态**: 试用阶段安全功能已禁用

- `forge.security.enabled: false` (环境变量 `FORGE_SECURITY_ENABLED`)
- 所有端点无需认证即可访问
- CORS 允许来源: `http://localhost:3000,http://localhost:9000` (可配置)
- OAuth2 Resource Server 依赖已引入，待激活

### 4.7 后端技术栈

| 层 | 技术 | 版本 |
|---|------|------|
| 运行时 | JDK | 21 (Eclipse Temurin) |
| 框架 | Spring Boot | 3.3+ |
| 语言 | Kotlin | 1.9.25 |
| 协程 | kotlinx-coroutines | 1.7.3 |
| ORM | Spring Data JPA + Hibernate | (Spring Boot managed) |
| 数据库迁移 | Flyway | (Spring Boot managed) |
| HTTP 客户端 | Spring WebFlux (WebClient) | (Spring Boot managed) |
| 安全 | Spring Security + OAuth2 Resource Server | (试用阶段禁用) |
| 序列化 | Jackson + Kotlin Module | (Spring Boot managed) |
| 测试 | JUnit 5 + MockK 1.13 + AssertJ | 37 tests passing |

---

## 五、验证状态

> Phase 1.5 E2E 验证结果 (2026-02-18)

| # | 验证项 | 状态 | 说明 |
|---|--------|------|------|
| 1 | Docker 镜像构建 | ✅ | 3 个镜像全部构建成功 |
| 2 | 容器启动 | ✅ | 3 容器 running, backend healthy |
| 3 | Nginx 路由 | ✅ | 前端 200, API 正常返回 |
| 4 | 前端页面加载 | ✅ | `http://localhost:9000` 返回 200 |
| 5 | 后端 API | ✅ | `/api/knowledge/search` 返回数据 |
| 6 | AI Chat 流式响应 | ⏳ | 待 API Key 配置后验证 |
| 7 | Tool Call / Agentic Loop | ⏳ | 待 API Key 配置后验证 |

---

## 六、变更规则

1. **修改前**：查阅本文档对应基线节，确认当前设计
2. **评估影响**：判断变更是否为"非预期退化"还是"有意演进"
3. **有意演进**：更新本文档对应节 + 更新 `design-regression-baseline.sh` 快照
4. **非预期退化**：回退变更，或经 Review 后接受并更新基线
5. **新增设计**：在对应维度添加新节，标注引入日期和原因

---

> 基线版本: v1
> 冻结日期: 2026-02-18
> 下次评审: Phase 2 开始前
