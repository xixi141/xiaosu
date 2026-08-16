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

### D3+D4（2026-08-18 至 08-19）

- 今天做了什么：ChatService 问答编排核心（拒答预检→RAG 检索→引用→多轮记忆→工具回调→流式）、SSE 流式 + 调试聊天页、日志查询 API + 日志页、4 个 @Tool 工具 + agent loop 测试、钉钉业务接入（会话隔离 + 引用卡片 + 兜底）、设置页。

- 给 AI 的 prompt 例子（写 ChatService 前我让 AI 先查 API 的命令）：

> 「用 javap 检查 spring-ai-client-chat 1.1.8 的 ChatClient.Builder 有哪些方法（defaultSystem/defaultAdvisors/defaultTools/mutate），确认 MessageChatMemoryAdvisor 的构造方式」

- 哪里能用/哪里必须改（继续「javap 实测」模式，又抓出 5 个计划外的坑）：
  1. **MessageChatMemoryAdvisor 强制要 conversationId**：请求时必须 `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key))`，否则运行时抛「conversationId cannot be null」——测试立刻暴露，改成 sessionKey 传入。
  2. **ToolCallAdvisor 不会自动装配**：模型返回 tool_call 后工具根本不执行（测试断言 answer 为空）。用 javap 反编译发现 1.1.8 里没有任何自动配置把它加进 advisor 链，必须在 ChatClient 上显式 `.defaultAdvisors(memoryAdvisor, ToolCallAdvisor.builder().toolCallingManager(...).build())`。这是全项目最隐蔽的坑——不加它，验收 7.2 会全线崩。
  3. **AssistantMessage 带 toolCalls 的构造器是 protected 且类为 final**：测试里没法直接构造「模型返回工具调用」的假响应，改用反射（测试代码里有注释说明原因）。
  4. **测试 classpath 的 application.yml 会整体遮蔽主配置**（不是合并）：我只写了数据源覆盖，结果 spring.ai 配置全丢、应用起不来。补全后解决。
  5. 其他小坑：`MessageWindowChatMemory` 没有 int 构造器要走 builder；`ToolContext` 在 `chat.model` 包不在 `tool.context`；`Map.of` 最多 10 对键值；Lombok `log` 字段被局部变量遮蔽导致编译失败；MockMvc 测 Flux 流式要用 asyncDispatch 才能拿到完整事件序列。

- 被带沟里的经历：最值得记的是第 2 条。AI 计划的 ChatService 直接用 `.defaultTools(...)`，计划里完全没提 ToolCallAdvisor——如果我在联调阶段才用真实 API 测工具调用，会看到「模型一直在要工具、工具一直不执行」的诡异现象，排查成本极高。**是「Mock LLM 触发真实工具」这条离线测试把坑提前压出来的**：不用花一分钱 API 费、不用等网络，10 分钟内定位到 advisor 缺失。这让我彻底确信「能离线测的绝不在线测」。

- 怎么验证 AI 代码：① 24 条测试全绿（含 agent loop 测试：脚本 LLM 第一轮吐 tool_call → 真实 OrderTool 统计 17 笔订单 → 第二轮吐答案 → chat_log 完整记录）；② MockMvc 断言 SSE 事件序列 meta→token→done；③ 前端 strict TS 构建零错误；④ 每任务 commit（D3+D4 共 5 个 commit）。

- 重做会怎么调整（阶段小结）：先让 AI 把「集成链路」的测试写出来（哪怕用假模型），再写实现——今天的 5 个坑有 4 个是被测试逼出来的，而不是我主动想到的。

### D5（2026-08-20）真实模型联调：工具循环排障完整实录

今天拿着真金白银的 API key 跑验收，暴露了离线测试测不出来的三个深层问题，全部定位到根因并修复：

**问题 1：DashScope embedding 连不上（404/400）**
- 现象：curl 兼容端点报 400「Required body invalid」，Spring AI 报 404
- 排障过程：curl 直接探测 → 查阿里云官方文档 → 发现 2026 年起 DashScope 兼容端点改用**业务空间域名**（`{业务空间ID}.cn-beijing.maas.aliyuncs.com`，控制台「业务空间管理」页面可查），旧域名已弃用；换域名后 curl 通了，但 Spring AI 仍 404
- 第二层根因：Spring AI 的 OpenAiApi 会在 base-url 后自动追加默认路径 `/v1/embeddings`，我们的 base-url 已含 `/compatible-mode/v1` → 实际请求 `.../v1/v1/embeddings`。用 `spring.ai.openai.embedding.embeddings-path=/embeddings` 覆盖解决
- 教训：**AI 查文档时（包括我自己）都容易漏掉「客户端 SDK 会在 base-url 后拼默认路径」这类隐式行为**，换供应商时这是最高发的事故点

**问题 2：模型不调工具反而用 RAG 内容回答（「员工 001 是哪个部门的」答成了加班 FAQ）**
- 根因：检索到的无关知识库内容干扰了模型的工具判断
- 修复：system prompt 改为「按优先级排列的规则」，工具类问题设为最高优先级并明确「不得用知识库内容代替」

**问题 3（最硬核的一个）：DeepSeek 工具循环 400「insufficient tool messages following tool_calls」**
- 现象：离线测试全绿（假模型），真实 DeepSeek 一跑工具就 400
- 排障实录：
  1. 写了一个临时的 RequestLogger Adivsor 打印每次模型调用的消息角色序列，发现工具循环第二次调用的消息是 `[SYSTEM | USER | TOOL]`——assistant 的 tool_calls 消息不见了
  2. 反编译（javap -c）MessageChatMemoryAdvisor.before() 确认：记忆 advisor 在工具循环的内部调用中重排消息列表，吞掉了 assistant tool_calls 消息
  3. 修复方案一（改 conversationHistoryEnabled=false）反而更糟：反编译 ToolCallAdvisor 发现 false 时 Spring AI **故意**只发 `[system, 最后一条工具结果]`（对 OpenAI 够用，DeepSeek 不行）
  4. 最终修复：**彻底放弃 MessageChatMemoryAdvisor，手动管理记忆**——提示词自己拼 [system + 历史干净对话 + 当前提问]，工具循环内部调用不再经过记忆逻辑；conversationHistoryEnabled 保持默认 true
  5. 顺带修出第二个问题：工具上下文（ToolRecorder）必须放 ToolCallingChatOptions 里，请求级 `.toolContext()` 方法在 1.1.8 中根本不会被工具读取（反编译 DefaultToolCallingManager.buildToolContext 确认）
- 修复后验收：7.2 三题全过（员工/订单双工具链/时间），7.3 多轮指代全过（「他」→001 出勤 5 天），会话隔离验证通过（另一用户问「他呢？」模型反问要工号）
- 教训：**「离线全绿 ≠ 真实模型能跑」**。Mock LLM 只会按脚本出牌，真实的工具循环消息构造、供应商对消息序列的严格校验，只有真模型能测出来。但如果一开始就没有离线测试，这些问题会淹没在「连不上」「没响应」的表层现象里，根本到不了反编译定位的深度。

- 怎么验证 AI 代码：① 27 条离线测试全绿（重构后 agent loop 测试依然通过）② 真实 API 验收 7.1-7.4 全过（curl 逐题实测 + 日志页核对工具轨迹）③ 反编译对照（javap -c）确认修复与框架真实行为一致 ④ 每步 commit。

### D6（2026-08-16）中文文件名乱码排障：从「列表乱码」追到「MSYS curl 发 GBK 字节」

启动项目验收时发现知识库管理页 8 个中文文件名全是乱码（`Ա���ֲ�.md` 这种），聊天引用里也跟着乱。完整排障与修复记录：

**现象与定位**
- 乱码只出现在 19:36 seed.sh 批量导入的文档上，18:27 浏览器单传的「测试手册.md」正常 → 问题在导入链路的客户端侧
- 用 Python 做解码矩阵实验：把磁盘上的正确文件名 `.encode('gbk')` 再 `.decode('utf-8', errors='replace')`，与库里 8 个乱码串**全部精确匹配** → 服务端收到的就是 GBK 字节，被 Tomcat 按 UTF-8 解码成 U+FFFD 后入库（`Ա` 就是 GBK 的 D4B1 被当成 UTF-8 解出来的）
- 三路对比实验锁定元凶：MSYS2 的 `/mingw64/bin/curl` 上传 → 乱码；`C:\Windows\System32\curl.exe` → 干净；Python urllib → 干净。**MSYS curl 在中文 Windows 上把非 ASCII 文件名按 ANSI 码页（GBK）转换后发出**
- 次生事故：报销制度.txt / 考勤制度.txt 乱码后撞名，触发「同名自动替换」逻辑，**知识库静默丢了一份文档**——乱码不只是丑，会触发数据丢失

**修复**
1. seed.sh 改用 Windows 原生 curl（Linux/macOS 回退 PATH 里的 curl），实测干净
2. 数据重建：删除乱码文档后重导，撞名丢失的那份也回来了；最终 9 份文档全部干净、13 个向量与切片数一致
3. 后端加防线（TDD）：上传接口拒绝含 U+FFFD 的文件名，返回 400 明确报错——乱码从此在入口被挡，不会再进库触发任何同名替换逻辑。先写 MockMvc 测试看它红（400 expected but got 200），再写最小实现

**被带沟里的经历（两条，都值得记）**
- 我自己也被乱码骗了两次：第一次用 Python 检查数据时没设 `PYTHONUTF8`，stdin 按 GBK 解码把 JSON 里的 U+FFFD 解成了 `\udcbd` 孤立代理字符，一度以为库里存了代理字符；第二次把「U+FFFD 替换字符」直接写进 Java 源码，改逃逸序列时工具反复把我的 `\uFFFD` 输出成字面字符。**教训：排查编码问题时，你自己的检查管道本身就是一条可能乱码的链路——结果写 UTF-8 文件、转义字符用 chr() 拼，别相信终端回显**
- 清理数据时过滤器写得太宽（`'测试' in filename`），把用户上传的正常文档「测试手册.md」也删了且原件无法恢复，最后用测试夹具内容重建。**教训：批量删除用 id 白名单，不要用名字模糊匹配**

- 怎么验证 AI 代码：① 解码矩阵实验 8/8 匹配证明根因；② 三路对比实验锁定元凶工具；③ TDD 红线-绿线（先看新测试失败再实现）；④ 26 条离线测试全绿；⑤ 在线冒烟：MSYS curl 传乱码名 → 400，原生 curl 传干净名 → 200；⑥ 最终列表 9 份全干净 + 向量数一致。

## 五个必答题（定稿时填）

1. 用了哪些 AI 工具…
2. 具体 prompt 例子…
3. 被带沟里的经历…
4. 怎么验证…
5. 重做会怎么调整…
