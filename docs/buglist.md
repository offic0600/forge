# Forge Platform Bug 追踪清单

> 持久化记录所有发现的 Bug，包括根因分析和修复方案。
> 格式：Bug ID | 发现时间 | 严重等级 | 状态

---

## Bug 汇总

| ID | 严重等级 | 状态 | 简述 |
|----|---------|------|------|
| BUG-001 | P1 | ✅ 已修复 | FileExplorer 右键菜单不显示（空白区域无 onContextMenu） |
| BUG-002 | P2 | ✅ 已修复 | 右键文件夹时上下文菜单路径被覆盖为空（事件冒泡） |
| BUG-003 | P2 | ✅ 已修复 | 文件/文件夹可重名创建，无唯一性校验 |
| BUG-004 | P2 | ✅ 已修复 | 重名创建时无用户反馈（静默失败） |
| BUG-005 | P1 | ✅ 已修复 | 无法删除文件夹（后端 deleteFile 只删单个路径） |
| BUG-006 | P2 | ✅ 已修复 | 重名校验为全局级别，应改为同级校验 |
| BUG-007 | P2 | ✅ 已修复 | 右键文件时 New File/New Folder 创建到根目录而非父目录 |
| BUG-008 | P0 | ✅ 已修复 | 文件树不显示层级结构（枚举大小写序列化不匹配） |
| BUG-009 | P2 | ✅ 已修复 | rebuildFileTree 只支持 2 层扁平结构，不支持多级嵌套 |
| BUG-010 | P2 | ✅ 已修复 | McpControllerTest mock 签名不匹配（2参数→3参数） |
| BUG-011 | P2 | ✅ 已修复 | handleFileSelect 声明顺序错误导致 TypeScript 编译失败 |
| BUG-012 | P0 | ✅ 已修复 | AI 不写文件到 workspace（WebSocket 未传 workspaceId） |

---

## 详细记录

### BUG-001: FileExplorer 右键菜单不显示
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 右键点击文件树空白区域，只弹出浏览器原生菜单，不显示 New File/New Folder
- **根因**: 文件树容器 `<div>` 缺少 `onContextMenu` 事件处理
- **修复**: 在文件树容器 div 添加 `onContextMenu` handler，显示 New File / New Folder 选项
- **文件**: `web-ide/frontend/src/components/editor/FileExplorer.tsx`

### BUG-002: 右键文件夹时上下文菜单路径被覆盖
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 右键文件夹弹出菜单但 path 为空，无法在该文件夹下创建子项
- **根因**: TreeNode 的 `onContextMenu` 没有调用 `e.stopPropagation()`，事件冒泡到容器后容器的 handler 将 path 覆盖为 `""`
- **修复**: TreeNode 的 `onContextMenu` 添加 `e.stopPropagation()`
- **文件**: `web-ide/frontend/src/components/editor/FileExplorer.tsx`

### BUG-003: 文件/文件夹可重名创建
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 在同一目录下可以创建同名的文件或文件夹
- **根因**: `handleNewFile` 和 `handleNewFolder` 没有唯一性校验逻辑
- **修复**: 添加 `collectPaths()` 工具函数进行创建前校验（后续升级为 `hasDuplicateSibling` 同级校验）
- **文件**: `web-ide/frontend/src/components/editor/FileExplorer.tsx`

### BUG-004: 重名创建时无用户反馈
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 创建同名文件时无任何提示，用户以为操作失败
- **根因**: 校验失败后直接 `return`，没有 UI 反馈
- **修复**: 添加 `window.alert()` 提示 "already exists in this directory"
- **文件**: `web-ide/frontend/src/components/editor/FileExplorer.tsx`

### BUG-005: 无法删除文件夹
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 点击 Delete 删除文件夹时，文件夹不消失
- **根因**: 后端 `WorkspaceService.deleteFile()` 只调用 `fileContents.remove(path)` 删除精确匹配的 key。文件夹 `src` 不是 fileContents 的 key（key 是 `src/index.ts` 等），所以删除无效
- **修复**: `deleteFile` 增加前缀匹配删除：`files.keys.removeIf { it.startsWith("$path/") }`
- **文件**: `web-ide/backend/src/main/kotlin/com/forge/webide/service/WorkspaceService.kt`

### BUG-006: 重名校验为全局级别
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: `src/index.ts` 存在时，无法在 `lib/` 下创建 `index.ts`（全局路径去重）
- **根因**: `collectPaths()` 收集整棵树所有路径做去重，应只检查同一父目录下的兄弟节点
- **修复**: 替换为 `hasDuplicateSibling()` 函数，按路径层级导航到父目录后只检查同级节点的 name
- **文件**: `web-ide/frontend/src/components/editor/FileExplorer.tsx`

### BUG-007: 右键文件时 New File 创建到根目录
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 右键 `src/index.ts` → New File，默认路径是根目录而非 `src/`
- **根因**: 上下文菜单 New File/New Folder 按钮只在 `isDirectory` 时传递 parentPath，文件节点直接传 `undefined`
- **修复**: 新增 `getParentPath()` 方法，右键文件时自动推断其父目录（提取 lastIndexOf("/") 前的路径）
- **文件**: `web-ide/frontend/src/components/editor/FileExplorer.tsx`

### BUG-008: 文件树不显示层级结构（枚举序列化）
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: 文件树所有节点（包括文件夹）都显示为文件，没有展开箭头，无法展示嵌套目录
- **根因**: 后端 Kotlin 枚举 `FileType.DIRECTORY` 被 Jackson 序列化为 `"DIRECTORY"`（大写），但前端 TypeScript 类型定义为 `"directory"`（小写）。`node.type === "directory"` 永远为 false，所有节点都被当作文件渲染
- **修复**: 在 `FileType` 和 `WorkspaceStatus` 枚举上添加 `@JsonValue fun toValue() = name.lowercase()`，确保序列化为小写
- **文件**: `web-ide/backend/src/main/kotlin/com/forge/webide/model/Models.kt`

### BUG-009: rebuildFileTree 只支持 2 层扁平结构
- **发现**: Session 15, Phase 1.6 验收测试 场景 D
- **症状**: `src/utils/helper.ts` 在文件树中显示为 `src/` 下的扁平文件 `utils/helper.ts`，不是嵌套目录
- **根因**: 后端 `rebuildFileTree` 只按第一级目录分组，所有子路径拼接为单个文件名
- **修复**: 重写 `rebuildFileTree`，使用递归 MutableNode 树构建算法，支持任意深度的嵌套目录
- **文件**: `web-ide/backend/src/main/kotlin/com/forge/webide/service/WorkspaceService.kt`

### BUG-010: McpControllerTest mock 签名不匹配
- **发现**: Session 15, 自动化验收测试
- **症状**: `McpControllerTest` 编译失败，`callTool` mock 使用 2 参数但实际方法是 3 参数
- **根因**: Session 14 将 `callTool` 从 2 参数升级为 3 参数（增加 workspaceId），测试未同步更新
- **修复**: mock 表达式添加第三个 `any()` 参数
- **文件**: `web-ide/backend/src/test/kotlin/com/forge/webide/controller/McpControllerTest.kt`

### BUG-011: handleFileSelect 声明顺序错误
- **发现**: Session 15, 前端构建
- **症状**: `npm run build` 报 TypeScript 错误：`handleFileSelect` used before declaration
- **根因**: `useEffect`（第 52 行）引用了在第 70 行才声明的 `handleFileSelect`
- **修复**: 将 `handleFileSelect` 的 `useCallback` 声明移到 `useEffect` 之前
- **文件**: `web-ide/frontend/src/app/workspace/[id]/page.tsx`

---

## 统计

- **总计**: 12 个 Bug
- **已修复**: 12 个
- **待修复**: 0 个
- **P0 (阻塞)**: 2 个 (BUG-008, BUG-012)
- **P1 (严重)**: 2 个 (BUG-001, BUG-005)
- **P2 (一般)**: 8 个

## 影响文件

| 文件 | 涉及 Bug |
|------|---------|
| `web-ide/frontend/src/components/editor/FileExplorer.tsx` | BUG-001~004, 006, 007 |
| `web-ide/backend/src/main/kotlin/com/forge/webide/service/WorkspaceService.kt` | BUG-005, 009 |
| `web-ide/backend/src/main/kotlin/com/forge/webide/model/Models.kt` | BUG-008 |
| `web-ide/frontend/src/app/workspace/[id]/page.tsx` | BUG-011 |
| `web-ide/backend/src/test/kotlin/com/forge/webide/controller/McpControllerTest.kt` | BUG-010 |
| `web-ide/backend/src/main/kotlin/com/forge/webide/websocket/ChatWebSocketHandler.kt` | BUG-012 |
| `web-ide/frontend/src/lib/claude-client.ts` | BUG-012 |
| `web-ide/frontend/src/components/chat/AiChatSidebar.tsx` | BUG-012 |

---

### BUG-012: AI 不写文件到 workspace（WebSocket 未传 workspaceId）
- **发现**: Session 15, Phase 1.6 验收测试 场景 2 / 场景 B
- **症状**: AI 能调用 workspace_list_files、search_knowledge 等读取工具，但从不调用 workspace_write_file 写入文件。代码仅在聊天中展示，不写入 workspace
- **根因**: 三层断链：
  1. 前端 `claude-client.ts` 的 `streamMessage()` 不接受也不传递 workspaceId 参数
  2. WebSocket 消息 payload 中不包含 workspaceId
  3. 后端 `ChatWebSocketHandler.kt:113` 硬编码 `workspaceId = ""`，导致 `McpProxyService.callTool()` 中 workspace 工具路由失败（需要非空 workspaceId）
- **修复**:
  1. `claude-client.ts`: `streamMessage()` 新增 `workspaceId` 参数，WebSocket 消息中包含 workspaceId
  2. `AiChatSidebar.tsx`: 调用时传入 `workspaceId`
  3. `ChatWebSocketHandler.kt`: 从 payload 中提取 `workspaceId` 并传给 `claudeAgentService.streamMessage()`
- **文件**: `claude-client.ts`, `AiChatSidebar.tsx`, `ChatWebSocketHandler.kt`
