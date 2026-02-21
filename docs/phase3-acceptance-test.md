# Phase 3 验收测试 — 人机协作闭环

> **测试环境**：`docker compose -f docker-compose.trial.yml --env-file .env up --build`（6 容器：backend + frontend + nginx + keycloak + knowledge-mcp + database-mcp）
> **访问地址**：http://localhost:9000 | Keycloak：http://localhost:8180
> **测试结果**：24 用例，0 通过（0%），待执行
> **依赖**：Phase 2 全部完成，`phase3-hitl-collaboration` 分支

---

## 一、执行透明度（场景 1，4 用例）

### 场景 1：AI 执行过程实时可见

> 验证 sub_step 事件流、活动日志面板、OODA 轮次显示、Baseline 结果卡片。

#### TC-1.1 sub_step 事件实时推送

**操作**：在 AI Chat 输入 `@开发 帮我写一个 Kotlin 的 REST Controller`，观察 WebSocket 消息流

**预期**：
- [ ] 收到 `sub_step` 类型事件 ≥ 5 条（解析意图、路由 Profile、组装 prompt、工具调用前/后）
- [ ] 每条 sub_step 包含 `message` 和 `timestamp` 字段
- [ ] 工具调用事件包含工具名称和耗时

#### TC-1.2 OODA 指示器增强 — Turn 计数 + 工具名

**操作**：发送需要工具调用的消息（如 `@开发 实现一个订单服务`），观察 OODA 指示器

**预期**：
- [ ] OODA 指示器显示 `Turn X/8`（X 从 1 开始递增）
- [ ] Act 阶段显示当前工具名称（如 `workspace_write_file`）
- [ ] 每轮结束后 Turn 计数递增

#### TC-1.3 可折叠活动日志面板

**操作**：发送消息后，点击 OODA 指示器下方的「活动日志」展开按钮

**预期**：
- [ ] 活动日志默认折叠，点击可展开
- [ ] 展开后显示最近 50 条 sub_step 条目（时间 + 消息）
- [ ] 实时追加新条目，最新条目在底部
- [ ] 再次点击可折叠

#### TC-1.4 Baseline 结果徽标

**操作**：发送触发 baseline 检查的消息（如 `@开发 写一个 Service 类`），观察消息尾部

**预期**：
- [ ] 收到 `baseline_check` 类型事件
- [ ] OODA 区域显示 baseline 结果徽标（✅ 通过 / ❌ 失败）
- [ ] 徽标包含通过/失败的 baseline 名称

---

## 二、HITL 全量暂停点（场景 2，6 用例）

### 场景 2：5 Profile 人工审批闭环

> 验证 5 个 Profile 的 HITL 暂停、approve/reject/modify 三种操作、超时处理、断线重连恢复。

#### TC-2.1 Development Profile — 代码审查暂停

**操作**：输入 `@开发 实现一个用户注册 Controller`，等待 AI 写入文件后观察

**预期**：
- [ ] AI 调用 `workspace_write_file` 写入代码后暂停执行
- [ ] 出现橙色边框的 HITL 审批面板
- [ ] 面板显示：Profile 名称（development）、Checkpoint 描述（代码审查）、已生成文件列表
- [ ] 面板显示 5 分钟倒计时
- [ ] 点击「✅ 批准继续」→ AI 继续执行并输出总结

#### TC-2.2 Planning Profile — PRD 确认暂停

**操作**：输入 `@规划 写一个用户管理模块的 PRD`，等待 AI 输出完成

**预期**：
- [ ] AI 输出完整 PRD 后暂停
- [ ] 审批面板显示 Profile（planning）、Checkpoint（PRD 确认）
- [ ] 点击「✅ 批准继续」→ AI 输出确认消息

#### TC-2.3 Reject 操作 — 拒绝停止

**操作**：触发任意 Profile 的 HITL 暂停后，点击「❌ 拒绝停止」

**预期**：
- [ ] AI 停止当前执行
- [ ] 收到 `hitl_checkpoint` 事件，status 为 `rejected`
- [ ] AI 发送总结消息说明被拒绝
- [ ] HITL 审批面板消失，对话恢复可输入状态

#### TC-2.4 Modify 操作 — 修改指令重入

**操作**：触发 HITL 暂停后，点击「✏️ 修改指令」，输入新的指示（如 "请增加参数校验"），提交

**预期**：
- [ ] 收到 `hitl_checkpoint` 事件，status 为 `modified`
- [ ] AI 从 Orient 阶段重新开始执行，使用修改后的指令
- [ ] 新一轮执行的 OODA 指示器正常显示

#### TC-2.5 超时处理 — 5 分钟自动继续

**操作**：触发 HITL 暂停后，不做任何操作，等待 5 分钟（或后端配置缩短超时时间验证）

**预期**：
- [ ] 倒计时归零后收到 `hitl_checkpoint` 事件，status 为 `timeout`
- [ ] AI 自动继续执行
- [ ] HITL 审批面板消失

#### TC-2.6 断线重连恢复

**操作**：触发 HITL 暂停后，刷新浏览器页面重新连接 WebSocket

**预期**：
- [ ] 重新连接后收到 `hitl_checkpoint` 事件（后端检测到 PENDING 状态的 checkpoint）
- [ ] 审批面板重新出现，倒计时从剩余时间继续
- [ ] 可正常执行 approve/reject/modify 操作

---

## 三、编译/测试管道（场景 3，4 用例）

### 场景 3：完整编译测试管道

> 验证 workspace_compile 和 workspace_test 工具注册、语法分析执行、失败重试流程。

#### TC-3.1 workspace_compile 工具可用

**操作**：
```bash
curl -s http://localhost:9000/api/mcp/tools | python3 -m json.tool | grep workspace_compile
```

**预期**：
- [ ] 工具列表中包含 `workspace_compile`
- [ ] 工具 schema 包含 `workspaceId` 参数

#### TC-3.2 workspace_test 工具可用

**操作**：
```bash
curl -s http://localhost:9000/api/mcp/tools | python3 -m json.tool | grep workspace_test
```

**预期**：
- [ ] 工具列表中包含 `workspace_test`
- [ ] 工具 schema 包含 `workspaceId` 和 `testPattern` 参数

#### TC-3.3 编译执行 — 语法分析

**操作**：在 workspace 中写入一个 Kotlin 文件，然后输入 `@开发 编译检查当前项目`（或 AI 自动调用 workspace_compile）

**预期**：
- [ ] AI 调用 `workspace_compile` 工具
- [ ] 返回结构化结果：`success`（boolean）、`fileCount`、`errors`（数组）、`warnings`（数组）
- [ ] 对于语法正确的文件返回 `success: true`
- [ ] 对于语法错误的文件返回 `success: false` + 错误详情

#### TC-3.4 测试分析

**操作**：在 workspace 中写入包含测试函数的文件（如含 `@Test` 注解的 Kotlin 文件），然后调用 workspace_test

**预期**：
- [ ] AI 调用 `workspace_test` 工具
- [ ] 返回结构化结果：`testFiles`（数量）、`testFunctions`（数量）、`assertions`（数量）
- [ ] 正确识别测试文件（*Test.kt、*Spec.ts、test_*.py 等）

---

## 四、质量度量面板（场景 4，4 用例）

### 场景 4：Dashboard API + 前端质量面板

> 验证 Dashboard 3 个 API 端点、前端 4 区域渲染、Tab 切换。

#### TC-4.1 Dashboard Metrics API

**操作**：
```bash
curl -s http://localhost:9000/api/dashboard/metrics | python3 -m json.tool
```

**预期**：
- [ ] 返回 JSON 包含 `profileStats`（数组）
- [ ] 返回 JSON 包含 `toolCallStats`（数组，Top 10 工具）
- [ ] 返回 JSON 包含 `hitlStats`（对象：total/approved/rejected/timeout/modified/pending）
- [ ] 返回 JSON 包含 `totalSessions`（整数）和 `avgDurationMs`（整数）

#### TC-4.2 Dashboard Executions API

**操作**：
```bash
curl -s "http://localhost:9000/api/dashboard/executions?limit=5" | python3 -m json.tool
```

**预期**：
- [ ] 返回最近 5 条执行记录数组
- [ ] 每条记录包含：id, sessionId, profile, skillsLoaded, totalDurationMs, totalTurns, createdAt
- [ ] 记录按 createdAt 降序排列

#### TC-4.3 Dashboard Trends API

**操作**：
```bash
curl -s "http://localhost:9000/api/dashboard/trends?days=7" | python3 -m json.tool
```

**预期**：
- [ ] 返回 7 天趋势数据数组（7 个元素）
- [ ] 每个元素包含：date, sessions, avgDurationMs
- [ ] 日期按升序排列

#### TC-4.4 前端质量面板 Tab 切换

**操作**：在 AI Chat 侧边栏顶部点击「质量面板」Tab

**预期**：
- [ ] Tab 切换到质量面板视图
- [ ] 显示 3 个统计卡片（总会话、平均耗时、HITL 审批）
- [ ] 显示工具调用排行（横向柱状图）
- [ ] 显示 7 日趋势柱状图
- [ ] 显示最近执行记录表格
- [ ] 点击「对话」Tab 切换回对话视图

---

## 五、学习循环（场景 5，3 用例）

### 场景 5：执行记录持久化 + Skill 分析

> 验证执行记录保存到 DB 和文件系统、Skill 反馈分析报告生成。

#### TC-5.1 执行记录 DB 持久化

**操作**：发送一条 AI 对话消息后，查询 H2 数据库

```bash
curl -s "http://localhost:9000/api/dashboard/executions?limit=1" | python3 -m json.tool
```

**预期**：
- [ ] 返回至少 1 条记录
- [ ] 记录包含 profile、totalDurationMs、totalTurns 等字段
- [ ] totalDurationMs > 0

#### TC-5.2 执行记录文件系统日志

**操作**：在 backend 容器中检查日志目录

```bash
docker compose -f docker-compose.trial.yml exec backend ls -la /app/logs/
```

**预期**：
- [ ] 存在日期目录（如 `2026-02-21/`）
- [ ] 目录中包含 `exec-*.json` 文件
- [ ] JSON 文件包含完整的执行记录（id, sessionId, profile, toolCalls 等）

#### TC-5.3 Skill 反馈分析服务

**操作**：检查 SkillFeedbackService 是否注册为 Spring Bean

```bash
curl -s http://localhost:9000/actuator/beans | python3 -c "
import sys, json
beans = json.load(sys.stdin)
for ctx in beans.get('contexts', {}).values():
    for name, info in ctx.get('beans', {}).items():
        if 'feedback' in name.lower() or 'logger' in name.lower():
            print(f'{name}: {info.get(\"type\", \"\")}')
"
```

**预期**：
- [ ] 输出包含 `skillFeedbackService`
- [ ] 输出包含 `executionLoggerService`
- [ ] 两个 Service 均为 Spring managed bean

---

## 六、端到端闭环（场景 6，3 用例）

### 场景 6：完整交付闭环

> 验证从编码到度量的完整流程：编码 → 编译 → 测试 → 底线 → HITL 审批 → 度量记录。

#### TC-6.1 Development Profile 完整管道

**操作**：输入 `@开发 实现一个完整的 Kotlin 订单服务，包括 Controller、Service、Entity`

**预期**：
- [ ] AI 依次调用 workspace_write_file 写入多个文件
- [ ] 活动日志实时显示写入进度（sub_step 事件）
- [ ] Baseline 自动检查运行
- [ ] HITL 暂停点触发，审批面板出现
- [ ] 点击批准后 AI 输出执行报告（文件清单 + 底线结果）

#### TC-6.2 度量数据完整性

**操作**：TC-6.1 完成后，查询 Dashboard Metrics

```bash
curl -s http://localhost:9000/api/dashboard/metrics | python3 -m json.tool
```

**预期**：
- [ ] `totalSessions` 计数增加
- [ ] `profileStats` 中包含 `development` Profile 的统计
- [ ] `toolCallStats` 中包含 `workspace_write_file` 的调用计数
- [ ] `hitlStats.total` 计数增加

#### TC-6.3 跨 Profile 切换 + 度量

**操作**：依次发送 `@规划 写个简单 PRD` → `@设计 设计一个 API` → `@开发 实现这个 API`，完成后查看质量面板

**预期**：
- [ ] 前端质量面板「Profile 使用」区域显示 3 个 Profile 的使用统计
- [ ] 每个 Profile 显示执行次数和平均耗时
- [ ] 执行记录表格包含 3 条记录，分别对应不同 Profile

---

## 汇总

| 场景 | 用例数 | 通过 | 备注 |
|------|-------|------|------|
| 场景 1：执行透明度 | 4 | 0 | 待执行 |
| 场景 2：HITL 暂停点 | 6 | 0 | 待执行 |
| 场景 3：编译/测试管道 | 4 | 0 | 待执行 |
| 场景 4：质量度量面板 | 4 | 0 | 待执行 |
| 场景 5：学习循环 | 3 | 0 | 待执行 |
| 场景 6：端到端闭环 | 3 | 0 | 待执行 |
| **合计** | **24** | **0** | **0%** |

---

## 启动命令

```bash
# 1. 确保 JDK 21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21

# 2. 构建后端
./gradlew :web-ide:backend:bootJar -x test --no-daemon

# 3. 构建前端
cd web-ide/frontend && npm install && npm run build && cd ../..

# 4. Docker 启动
cd infrastructure/docker
docker compose -f docker-compose.trial.yml --env-file .env up --build -d

# 5. 等待容器 healthy（约 60s）
docker compose -f docker-compose.trial.yml ps

# 6. 访问 http://localhost:9000
```

## 关键观察点

1. **WebSocket 消息流**：浏览器 DevTools → Network → WS → 观察 `sub_step`、`ooda_phase`、`hitl_checkpoint` 事件
2. **后端日志**：`docker compose logs -f backend` 观察 HITL 状态机日志（`SkillFeedbackService`、`ExecutionLoggerService`）
3. **H2 Console**：http://localhost:9000/h2-console（JDBC URL: `jdbc:h2:mem:forgedb`）查看 `execution_records` 和 `hitl_checkpoints` 表
4. **Dashboard API**：`curl http://localhost:9000/api/dashboard/metrics` 实时查看聚合统计
