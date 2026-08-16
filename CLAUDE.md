# CLAUDE.md — 「小苏」项目说明

招聘笔试项目：公司内部 AI 助手「小苏」（钉钉 @机器人 + RAG 知识库 + Agent 工具调用 + Web 管理后台）。详细实施计划见 `docs/superpowers/plans/2026-08-16-xiaosu-implementation.md`。

## 技术栈（版本锁定，勿升级）

- 后端：Java 21 + Spring Boot **3.5.16** + Spring AI **1.1.8**（禁用 2.x——强制 Boot 4；BOM 手动 import）
- 前端：React 19 + Vite + Tailwind v4 + TypeScript strict（pnpm）
- 数据：H2 文件模式（`AUTO_SERVER=TRUE`）+ SimpleVectorStore（JSON 持久化，pgvector 仅 Roadmap）
- 模型：chat=DeepSeek；embedding=阿里云百炼 text-embedding-v4（**工作空间域名**，见 .env.example）
- IM：钉钉 Stream 模式（SDK 1.3.7，出站长连接，无需公网 IP）

## 常用命令（Windows Git Bash）

```bash
./scripts/dev.sh    # 一条命令起前后端（读 .env）
./scripts/seed.sh   # 导入 knowledge/ 全部文档
./scripts/test.sh   # 27 条离线测试（Mock 模型，不花 API 钱）
./scripts/start.sh  # 本地生产模式（打包 jar）
# live 测试：source .env 后 mvn test -Dtest=AiConnectivityLiveTest -Dsurefire.excludedGroups=none
```

注意：每个 Bash 工具调用是独立 shell，跑 mvn/curl 前必须 `cd /d/A_one/xiaosu/backend` + `set -a && source ../.env && set +a`。

## 关键架构决策

- RAG 检索在 ChatService 内同步做（无自定义 advisor）；引用 = 检索命中切片（CitationAssembler）
- 多轮记忆**手动管理**（不用 MessageChatMemoryAdvisor，见坑 #4）：FilteredChatMemory 只存「user + 无工具调用的 assistant」干净对话，按 `userId#conversationId` 隔离
- 工具轨迹经 `ToolCallingChatOptions.toolContext` 传 ToolRecorder → 落 chat_log.tool_calls
- 拒答：RefusalGuard 规则预检（不调模型）+ system prompt 双保险

## Spring AI 1.1.8 实测坑（重要！改这段代码前先读）

1. **ToolCallAdvisor 不会自动装配**，必须显式 `.defaultAdvisors(toolCallAdvisor)`，否则模型要了工具也不执行
2. **`conversationHistoryEnabled(false)` 会让内部调用只发 `[system, tool结果]`**，DeepSeek 要求 tool 消息前有 assistant tool_calls → 保持默认 true
3. **MessageChatMemoryAdvisor 会破坏工具循环内部调用的消息列表**（吞 assistant tool_calls 消息）→ 本项目不用它，手动拼历史
4. 请求级 `.toolContext(Map)` **不会被工具读取**；必须放 `DefaultToolCallingChatOptions.builder().toolContext(map)` 传进 `.options()`
5. `MessageChatMemoryAdvisor` 强制要 conversationId 参数；`MessageWindowChatMemory` 无 int 构造器（用 builder）
6. SimpleVectorStore/SearchRequest 在独立构件 `spring-ai-vector-store`（openai starter 不传递引入）；无 `setVectorStoreFilePath`/`getSimilarDocumentsCount`（路径走 save(File)/load(File)，计数读 JSON 条目数）
7. DashScope 兼容端点：base-url 含 `/compatible-mode/v1` 时必须配 `spring.ai.openai.embedding.embeddings-path=/embeddings`（否则 OpenAiApi 拼出 `/v1/v1/embeddings` 404）
8. `MockChatModel/MockEmbeddingModel` 在 1.1.8 不存在，用自写的 `FakeAiModels`（字符桶哈希伪向量）；测试置 `xiaosu.rag.similarity-threshold=0.0`
9. `toolCallbackProvider(Object[] toolObjects)` 全量注入 bean 会与 toolCallbackResolver 循环依赖 → 显式传 4 个工具 bean
10. 测试 classpath 的 `src/test/resources/application.yml` 会**整体遮蔽**主配置（不是合并），必须完整复制 spring.ai 段
11. MockMvc 测 Flux 流式要用 `asyncDispatch` 模式才能拿完整事件序列
12. AssistantMessage 带 toolCalls 的构造器是 protected 且类 final → 测试用反射（见 ScriptedChatModel）

## 目录约束（笔试题红线）

单文件 ≤500 行（当前最大 230）、单目录 ≤8 文件（Java 包同样遵守）、`.env` 永不入库（.gitignore 已覆盖）、logs 落 `logs/`、每任务 commit。

## 验收状态（2026-08-16/20）

- 7.1/7.2/7.3/7.4/7.6 API 侧实测全过；7.5 兜底文案实测通过
- 钉钉 IM 已打通（Stream 连接正常、消息收发/会话隔离/引用卡片实测通过）
- 离线测试 27/27 全绿；GitHub 公开仓库已推送（xixi141/xiaosu）
- 待用户：录演示视频、云服务器部署、发送投递邮件

## AI_USAGE.md 维护约定

D1-D5 已记录全部真实踩坑（15 分核心评分项）。后续每次用 AI 开发后追加当日真实经历，定稿前由用户以第一人称过一遍。
