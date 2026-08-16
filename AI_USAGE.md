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

### D2（2026-08-17）

- 今天做了什么：8 篇知识库文档（md/txt/docx/pdf 四格式，docx/pdf 用 uv + python-docx/reportlab 生成）；mock 数据（15 员工/30 考勤/20 订单，含迟到加班退款边界）；中文切块器（TDD）；VectorStoreService + SimpleVectorStore JSON 持久化；文档入库全链路（SHA256 去重 + 同名替换 + Tika 解析 + 索引）；钉钉 Stream SDK 接入（1.3.7）；文档管理页 + seed.sh。

- 给 AI 的 prompt 例子（计划阶段让 AI 查证 Spring AI API 的 prompt）：

> 「2026-08 时点 Spring Boot 3.5.x 与 Spring AI 的最新稳定版本号各是多少？…SimpleVectorStore 持久化机制（save 后写 JSON 文件？路径配置？）…」

- 哪里能用/哪里必须改（D2 是「AI 情报失实」密集爆发的一天，全部靠 javap 实测修正）：
  1. **AI 计划漏了依赖**：SimpleVectorStore/SearchRequest 在独立构件 `spring-ai-vector-store` 里，openai starter 不传递引入——编译报「程序包不存在」才发现的，加依赖解决。
  2. **AI 写的 API 不存在**：计划的 `setVectorStoreFilePath(String)`、`getSimilarDocumentsCount()` 在 1.1.8 里都没有。真实 API 是路径只通过 `save(File)/load(File)` 传入，计数要自己读持久化 JSON 条目数。我用 `javap` 反编译 jar 拿到真实签名后重写。
  3. **AI 说的 Mock 模型不存在**：计划称「Spring AI 自带 MockChatModel/MockEmbeddingModel」，我搜遍了本地仓库所有 1.1.8 构件都没有。自己写了 Fake 模型（字符桶哈希伪向量，含相同字符的文本相似度更高，检索命中可预期）——反而比官方 Mock 更适合我们的测试。
  4. **AI 写的接口方法名错了**：`ChatModel.defaultOptions()` 实际是 `getDefaultOptions()`；`EmbeddingModel` 的抽象方法是 `embed(Document)` 而不是 `embed(String)`；`Embedding` 构造要带 index。全部编译报错后 javap 修正。
  5. **AI 建议的写法造成循环依赖**：`toolCallbackProvider(Object[] toolObjects)` 注入全部 bean 会与 Spring AI 的 toolCallbackResolver 自动装配成环（启动失败），改成空注册 + Task 16 起显式传工具 bean。
  6. **AI 计划对题目的理解有缺口**：原设计只对「同内容」去重，但题目分水岭是「同名不同内容要识别并替换」。测试暴露后补上：同 sha 返回 duplicate；同名不同 sha 自动删除旧版本再入库。
  7. **AI 给的钉钉 SDK 用法是旧版**：计划写 `Consumer<CallbackContext<BotCallbackDataModel>>`，1.3.7 实际是 `OpenDingTalkCallbackListener<Req, Resp>` + 内置 `BotReplier`（白赚一个回复工具，删掉了自己写的 RestClient 回复代码）。SDK 版本也从计划的 1.3.2 锁到 Maven Central 最新的 1.3.7。
  8. **自己生成的知识库脚本有 bug**（这条不是 AI 情报失实，是 AI 写码的 bug）：gen_knowledge.py 把两行文本误打包成 tuple 导致 reportlab `setFont` 收到字符串 size 抛 TypeError——读报错定位，10 秒修完。

- 被带沟里的经历：最典型的是第 2/3 条——如果我只信计划里「SimpleVectorStore 有 setVectorStoreFilePath」直接写业务，问题会拖到运行时才暴露。**教训：AI 查的第三方库 API 细节，一律以 javap/官方 javadoc 实测为准；搜不到就搜 jar 里有没有这个类**。另外切块器测试还暴露过一次「测试假设与设计契约矛盾」（测试期望保留段落边界，我的设计是按句合并）——不是迁就实现改测试，而是把测试改成对契约的精确表达（注释里写明每个断言的推导）。

- 怎么验证 AI 代码：① 8 个测试类逐步 TDD（切块器 4 条、VectorStore 2 条、入库 4 条全绿，全部 Fake 模型离线跑）；② 编译失败→javap 查真实 API→重写→重跑，这个循环今天走了 5 轮；③ 订单/考勤 mock 数据的统计口径与测试断言手工核算一致（17 笔有效订单/3 笔退款、001 上周出勤 5 天）；④ 前端 strict TS 构建零错误；⑤ 每任务 commit（D2 共 6 个 commit）。

## 五个必答题（定稿时填）

1. 用了哪些 AI 工具…
2. 具体 prompt 例子…
3. 被带沟里的经历…
4. 怎么验证…
5. 重做会怎么调整…
