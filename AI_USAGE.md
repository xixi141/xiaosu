# AI_USAGE.md — 本项目 AI 工具使用记录

> 说明：本文件从开发第一天开始逐日记录，最终版本为真实经历，非套话。

## 使用的 AI 工具清单（持续更新）

| 工具 | 环节 | 体验 |
|---|---|---|
| Claude Code | （D1）方案规划、技术选型验证、代码生成、测试验证 | 规划质量高，但版本类信息必须实测验证 |

## 每日记录

### D1（2026-08-16）

- 今天做了什么：git init + 红线文件（.gitignore/.env.example）；后端骨架（Spring Boot 3.5.16 + Spring AI 1.1.8，配置全外置，日志落 logs/）；3 张表实体 + repository + 2 条冒烟测试；Spring AI 双模型装配（DeepSeek chat + DashScope embedding）；React 19 + Tailwind v4 前端骨架；scripts/dev.sh 一条命令启动。

- 给 AI 的 prompt 例子（规划阶段让 AI 查证版本的一段）：

> 「2026-08 时点 Spring Boot 3.5.x 与 Spring AI 的最新稳定版本号各是多少？Spring AI 是否已 GA 且 API 稳定（ChatClient、@Tool、QuestionAnswerAdvisor、MessageChatMemoryAdvisor、SimpleVectorStore、Flux 流式）？」

  AI 查到的结论直接改变了我锁版本：**Spring AI 2.0 已 GA 但强制要求 Spring Boot 4**，所以我锁了 Boot 3.5.16 + Spring AI 1.1.8，并用 BOM 手动 import（Boot 不托管 AI 版本）。这条结论当场用 mvn 解析验证通过。

- 哪里能用/哪里必须改：AI 生成的骨架代码（pom/application.yml/logback/实体/repository）基本直接能用，一条 mvn compile 就过。但有一个计划假设当场被打脸：**AI 计划认为 API key 留空应用也能启动，实测 Spring AI 的 OpenAiApi 在 key 为空时直接抛「OpenAI API key must be set」拒绝实例化，应用根本起不来**。我改成占位符 key 兜底（`${DEEPSEEK_API_KEY:sk-placeholder}`，启动时不会发网络请求所以无害），并在 yml 里写了注释说明原因——这种「计划假设 vs 实际行为」的偏差，不实测是发现不了的。

- 被带沟里的经历：暂时没有大的翻车（D1 主要是骨架）。一个小坑：AI 写的实施计划里 live 连通测试没有默认排除机制，`mvn test` 会带着占位符 key 跑真实 API 全红——我加了 surefire `excludedGroups=live` 修复。教训：**AI 写的计划里「默认行为」要自己过一遍构建链路**。

- 怎么验证 AI 代码：① `mvn compile`/`mvn test` 每步必跑（2 条 repository 冒烟测试 PASS）；② `curl /api/health` 实测返回 UP + OpenAiChatModel/OpenAiEmbeddingModel 装配成功；③ 前端 `pnpm build` strict TS 零错误；④ 每任务 commit（D1 共 4 个 commit），git status 保持干净。

## 五个必答题（定稿时填）

1. 用了哪些 AI 工具…
2. 具体 prompt 例子…
3. 被带沟里的经历…
4. 怎么验证…
5. 重做会怎么调整…
