# skills-SpringAI-agent

基于 Spring AI 的智能 Agent 技能系统。支持 **SKILL.md 定义技能包**、**Python/Node 脚本执行**、**PPTX 文档操作（解包→编辑→清理→打包）** 以及 **SSE 流式对话**。AI 模型通过 Tool Calling 机制自主调用技能工具完成复杂任务。

## 目录

- [项目架构](#项目架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [技能系统](#技能系统)
- [PPTX 操作](#pptx-操作)
- [数据库](#数据库)
- [前端界面](#前端界面)
- [配置参考](#配置参考)
- [开发指南](#开发指南)
- [调试与排错](#调试与排错)
- [已知问题](#已知问题)

## 项目架构

```
skills-SpringAi/
│
├── .agents/skills/                         # ★ 技能包（SKILL.md + Python/Node 脚本）
│   ├── pptx/                               # PPTX 操作技能
│   │   ├── SKILL.md                        #   技能入口（含 frontmatter 元数据）
│   │   ├── editing.md                      #   编辑工作流指引
│   │   ├── pptxgenjs.md                    #   从零创建 PPTX 指引（备用方案）
│   │   ├── LICENSE.txt                     #   技能许可协议
│   │   └── scripts/                        #   Python 工具脚本
│   │       ├── add_slide.py                #   新增/复制幻灯片
│   │       ├── clean.py                    #   清理未引用的文件
│   │       ├── thumbnail.py                #   缩略图网格预览
│   │       └── office/
│   │           ├── unpack.py               #   解包 OOXML → 目录
│   │           ├── pack.py                 #   打包目录 → OOXML
│   │           ├── validate.py             #   XSD 验证/自动修复
│   │           ├── soffice.py              #   LibreOffice 辅助工具
│   │           ├── validators/             #   XSD 验证器模块
│   │           │   ├── base.py             #     基类（BaseSchemaValidator）
│   │           │   ├── pptx.py             #     演示文稿验证
│   │           │   ├── docx.py             #     文档验证
│   │           │   └── redlining.py        #     修订跟踪验证
│   │           ├── helpers/                #   DOCX 辅助
│   │           │   ├── merge_runs.py       #     合并相邻格式 Run
│   │           │   └── simplify_redlines.py#     简化修订记录
│   │           └── schemas/                #   ECMA/ISO 标准 XSD 文件
│   │
│   ├── frontend-design/                    # 前端设计技能
│   │   └── SKILL.md
│   ├── frontend-code-review/               # 前端代码审查技能
│   │   ├── SKILL.md
│   │   └── references/                     #   审查规则参考
│   │       ├── business-logic.md
│   │       ├── code-quality.md
│   │       └── performance.md
│   └── backend-code-review/                # 后端代码审查技能
│       ├── SKILL.md
│       └── references/
│           ├── architecture-rule.md
│           ├── db-schema-rule.md
│           ├── repositories-rule.md
│           └── sqlalchemy-rule.md
│
├── ppt/                                    # PPTX 文件输出目录
│   └── *.pptx                              #   生成的演示文稿
│
├── src/main/java/org/example/skillsspringai/
│   ├── SkillsSpringAiApplication.java      # Spring Boot 启动类
│   │
│   ├── agent/                              # Agent 实现层
│   │   └── SuperFinancialAdvisorAgent.java #   默认 Agent：全能金融顾问大师
│   │
│   ├── controller/                         # REST API 层
│   │   └── ChatController.java             #   /api/chat（非流式）和
│   │                                       #   /api/chat/stream（SSE 流式）
│   │
│   ├── entity/                             # 数据实体
│   │   ├── Skill.java                      #   技能实体（类型、路径、元数据）
│   │   ├── AuditLog.java                   #   审计日志实体
│   │   ├── AuditLogMapper.java             #   MyBatis-Plus Mapper
│   │   └── ScriptResult.java               #   脚本执行结果封装
│   │
│   ├── framework/                          # 核心框架层
│   │   ├── Agent.java                      #   Agent 抽象接口
│   │   ├── SkillPackage.java               #   技能包抽象接口
│   │   └── impl/
│   │       └── BaseAgent.java              #   Agent 基础实现（Tool 注册、流式处理）
│   │
│   └── tool/                               # 工具层（★ 核心）
│       ├── SkillSource.java                #   技能源抽象接口
│       ├── ClasspathMarkdownSkillSource.java#  从 classpath 加载 SKILL.md
│       ├── FileSystemMarkdownSkillSource.java# 从文件系统加载 SKILL.md
│       ├── ClasspathPackageSkillSource.java #  从 classpath 加载 Java 技能包
│       ├── UrlSkillSource.java             #   从远程 URL 下载技能包
│       ├── SkillParser.java                #   SKILL.md frontmatter 解析器
│       ├── SkillRegistry.java              #   技能统一注册中心
│       ├── SkillLoaderService.java         #   多源加载调度服务
│       ├── SkillsTool.java                 #   Spring AI ToolCallback 适配器
│       ├── SkillScriptRunner.java          #   Python/Node 脚本执行（runInSkill）
│       ├── SkillFileReader.java            #   技能目录文件读写
│       ├── FinancialTools.java             #   金融计算工具（@Tool 示例）
│       ├── PythonScriptExecutor.java       #   Python 脚本专用执行器
│       ├── PackageClassLoader.java         #   动态 JAR 类加载器
│       ├── AgentRegistry.java              #   Agent 注册中心
│       ├── AuditLogService.java            #   审计日志 + AI 质检
│       ├── FileSystemSkillRepository.java  #   文件系统技能仓库（遗留）
│       └── SkillInput.java                 #   空输入 POJO
│
└── src/main/resources/
    ├── static/index.html                   # Web 前端（单页应用）
    ├── application.yml                     # 主配置文件
    ├── application.properties              # Spring 基础配置
    └── skills.ppt/pptx/                    # 编译时 skill 资源（classpath 加载源）
        ├── SKILL.md
        ├── editing.md
        ├── pptxgenjs.md
        └── scripts/                        #   skills.ppt 源文件的副本
```

### 核心数据流

```
用户请求（HTTP）
    │
    ▼
ChatController
    │  从 AgentRegistry 获取 Agent
    │  组装 context（sessionId, tools）
    ▼
BaseAgent.processStream()
    │  buildSkillsContext()  → 注入技能列表到 System Prompt
    │  prompt.toolCallbacks() → 注册 SkillsTool
    │  prompt.tools()        → 注册 FinancialTools
    │  prompt.tools(toolObj) → 注册 SkillScriptRunner / SkillFileReader
    │  prompt.stream().content()
    ▼
Spring AI / DeepSeek API
    │  AI 决策：返回文本 or 调用 Tool
    │  ┌─ 返回文本 ──→ Flux<String> 流式输出到客户端
    │  └─ 调用 Tool → DefaultToolCallingManager 执行
    │         │
    │         ├── SkillsTool 回调 → 返回技能指引文本给 AI
    │         ├── runInSkill   → SkillScriptRunner → ProcessBuilder
    │         ├── writeFileInSkill → 在技能目录写文件
    │         ├── readFileInSkill  → 读技能目录下文件
    │         └── FinancialTools   → PythonScriptExecutor
    │
    ▼
ChatController 审计
    │  doOnComplete / onErrorResume
    │  saveAuditLog() → AuditLogService.logAndEvaluate()
    │     ├── 调用 critic-agent 做 AI 质量评估
    │     └── 写入 audit_log 表
    ▼
客户端收到 SSE data: 行 或 JSON 响应
```

## 技术栈

| 组件                | 版本       | 用途                         |
|---------------------|-----------|------------------------------|
| Spring Boot         | 3.2.10    | 应用框架                      |
| Spring AI           | 1.0.0     | AI 集成（ChatClient、Tool Calling） |
| Spring Web (MVC)    | 6.x       | REST + SSE 支持               |
| Reactor Core        | 3.6.x    | 响应式流（Flux/Mono）         |
| DeepSeek API        | -         | LLM 后端（deepseek-chat）     |
| MyBatis-Plus        | 3.5.9     | ORM / 数据库操作              |
| H2 Database         | -         | 开发环境内存数据库             |
| MySQL Connector     | -         | 生产环境数据库（已包含依赖）    |
| LangGraph4j         | 1.5.13    | 图状态管理（框架依赖）         |
| Lombok              | -         | 代码简化                     |
| Java                | 17        | 运行环境                      |
| Python              | 3.10.8    | 技能脚本执行                   |
| Maven               | -         | 构建工具（已内置 Wrapper）     |

### Python 依赖

```bash
pip install defusedxml        # PPTX 工具链必须（unpack/pack/clean/validate）
pip install "markitdown[pptx]"# 文本提取（可选）
pip install Pillow            # 缩略图生成（可选，需 soffice + pdftoppm）
```

## 快速开始

### 1. 环境准备

| 工具         | 要求        | 验证命令              |
|-------------|------------|----------------------|
| JDK         | 17+        | `java -version`      |
| Maven       | 已内置      | `./mvnw --version`   |
| Python      | 3.10+（可选）| `python --version`   |
| defusedxml  | 必须（PPTX） | `python -c "import defusedxml"` |

### 2. 配置

核心配置在 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}         # 你的 API Key
      chat:
        options:
          model: deepseek-chat              # 模型选择
          temperature: 0.7                   # 生成创造性
          max-tokens: 8192                  # 最大输出长度
          timeout: 30s                      # API 超时

  datasource:
    url: jdbc:h2:mem:skills_agent;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password:
    driver-class-name: org.h2.Driver

  h2:
    console:
      enabled: true                         # H2 Web 控制台
      path: /h2-console

server:
  port: ${SERVER_PORT:8081}                 # 默认 8081，可通过环境变量覆盖
```

> **注意**：`api-key` 建议通过环境变量 `DEEPSEEK_API_KEY` 注入，避免提交到版本控制。

### 3. 启动

```bash
# Maven 启动
./mvnw spring-boot:run

# 或编译后运行
./mvnw package -DskipTests
java -jar target/skills-SpringAI-agent-0.0.1-SNAPSHOT.jar
```

启动日志关键行：

```
Started SkillsSpringAiApplication in 1.938 seconds
Tomcat started on port 8081 (http)
全能金融顾问大师已初始化 (技能: N, 通用工具: 3)
```

### 4. 验证

```bash
# SSE 流式对话
curl -G "http://localhost:8081/api/chat/stream" \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=你好" \
  --data-urlencode "sessionId=test-001"

# JSON 非流式对话
curl -G "http://localhost:8081/api/chat" \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=你好"

# 测试 Python 执行能力
curl -G "http://localhost:8081/api/chat/stream" \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=用runInSkill在pptx技能中执行 python -c 'print(1+1)'"
```

浏览器打开 `http://localhost:8081/` 可使用 Web 界面。

## API 文档

### 流式对话（SSE）

```
GET /api/chat/stream
```

| 参数         | 类型    | 必需 | 默认值           | 说明                    |
|-------------|--------|------|-----------------|------------------------|
| `agentName` | String | 是   | -               | Agent 名称，需 URL 编码  |
| `message`   | String | 是   | -               | 用户消息，需 URL 编码    |
| `sessionId` | String | 否   | `default-session` | 会话标识，用于多轮对话  |

**响应格式**：`text/event-stream`（SSE）

```
data:你好
data:！我
data:是
data:全能金融顾问
data:...
```

Spring MVC 自动将 `Flux<String>` 的每个元素封装为 `data: <chunk>\n\n` 格式。

**前端集成**：

```javascript
const url = `/api/chat/stream?agentName=${
  encodeURIComponent(name)
}&message=${
  encodeURIComponent(msg)
}&sessionId=${sessionId}`;

const es = new EventSource(url);

es.onmessage = (e) => {
  if (e.data.startsWith('[ERROR]')) {
    showError(e.data.slice(7));
  } else {
    appendText(e.data);
  }
};

// HTTP 连接关闭时流结束
es.onerror = () => {
  es.close();
  onStreamEnd();
};
```

### 非流式对话

```
GET /api/chat
```

参数与流式接口相同。返回 `application/json`：

```json
{
  "content": "完整的回复文本",
  "agentName": "全能金融顾问大师",
  "sessionId": "test-001",
  "durationMs": 15234
}
```

错误响应：

```json
{
  "error": "服务异常: ..."
}
```

超时时间：180 秒。

### 错误处理

后端对常见错误进行了用户友好翻译：

| 原始错误                                  | 用户看到的提示                                                      |
|------------------------------------------|-------------------------------------------------------------------|
| `Connection reset` / `SocketException`   | DeepSeek API 连接被重置，网络波动或服务端限流导致，请稍后重试。       |
| `timeout` / `Timeout`                    | 请求超时，请简化问题后重试。                                        |
| `Conversion from JSON`                   | AI 模型生成的工具调用参数格式异常（通常是因为脚本内容太长导致 JSON 截断） |
| `Invalid character in request target`    | 请求 URL 中包含非法字符（中文未编码）                                |

### 审计

每次对话完成后自动记录到 `audit_log` 表，并调用内置的 `critic-agent` 技能对对话质量进行 AI 评估。

## 技能系统

### 设计理念

技能（Skill）是 AI Agent 的能力单元。系统支持两种技能形态：

| 类型      | 格式         | 加载源     | 行为                                        |
|----------|-------------|-----------|---------------------------------------------|
| **TEXT** | SKILL.md    | classpath / 文件系统 / URL | AI 调用时返回技能指引文本，AI 根据指引自行调用 `runInSkill` 等工具 |
| **PACKAGE** | skill.json | classpath / URL | 包含 Java 类，通过 `@Tool` 注解直接暴露方法 |

目前主要使用 TEXT 类型，脚本执行等实际操作由共享工具 `SkillScriptRunner`、`SkillFileReader` 完成。

### 技能加载架构

```
SkillLoaderService                          ← 统一入口
    │
    ├── ClasspathMarkdownSkillSource        ← skills.ppt/**/SKILL.md
    ├── FileSystemMarkdownSkillSource       ← .agents/skills/**/SKILL.md
    ├── ClasspathPackageSkillSource         ← classpath:skill.json
    └── UrlSkillSource                      ← HTTP 远程下载
            │
            ▼
      SkillRegistry                         ← ConcurrentHashMap 存储
            │                                 register(Skill) / get(name) / getAll()
            ▼
        SkillsTool                          ← 适配为 Spring AI ToolCallback[]
            │
            ▼
    BaseAgent.processStream()               ← prompt.toolCallbacks(callbacks)
```

### SKILL.md 格式

技能定义文件使用 YAML frontmatter + Markdown 正文：

```markdown
---
name: pptx
description: "处理 .pptx 文件 — 创建、读取、编辑、分析演示文稿。"
license: Proprietary. LICENSE.txt has complete terms
---

# PPTX Skill

## 快速参考

| 任务            | 指引                    |
|----------------|------------------------|
| 读取/分析内容   | `python -m markitdown`  |
| 创建/编辑（推荐）| 见 [editing.md](editing.md) |
| 从零创建（备用） | 见 [pptxgenjs.md](pptxgenjs.md) |

## 编辑工作流

1. 分析模板 → 2. 解包 → 3. 编辑 XML → 4. 清理 → 5. 打包

...
```

### Tool 注册流程（以全能金融顾问大师为例）

```java
// SuperFinancialAdvisorAgent 构造函数
SkillsTool skillsTool = SkillsTool.builder()
        .registry(skillLoaderService.getRegistry())
        .build();
setSkillsTool(skillsTool);        // → prompt.toolCallbacks(skillsTool.getToolCallbacks())

addToolObject(skillFileReader);   // → prompt.tools(skillFileReader)
addToolObject(skillScriptRunner); // → prompt.tools(skillScriptRunner)

// ChatController 中额外注册
context.put("tools", financialTools);  // → prompt.tools(tools)
```

AI 可用的完整工具集：

| Tool Callback            | 来源                  | @Tool 方法                                                                 |
|--------------------------|----------------------|---------------------------------------------------------------------------|
| `pptx` / 各技能名称       | SkillsTool           | 调用后返回对应 SKILL.md 的指引文本                                          |
| `runInSkill`             | SkillScriptRunner    | 在技能目录下执行 Python/Node/Bash 脚本或内联代码                            |
| `writeFileInSkill`       | SkillScriptRunner    | 在技能目录下创建/覆写文件                                                   |
| `getOutputDir`           | SkillScriptRunner    | 返回 ppt/ 输出目录的绝对路径                                                |
| `readFileInSkill`        | SkillFileReader      | 读取技能目录下的文件内容                                                    |
| `listFilesInSkill`       | SkillFileReader      | 列出技能目录下的文件                                                       |
| `calculateDcf`           | FinancialTools       | DCF 现金流折现估值                                                         |
| `calculateTechnicalIndicators` | FinancialTools | 技术指标 MA/RSI/MACD                                                      |
| `calculateVar`           | FinancialTools       | 投资组合 VaR 风险价值                                                      |
| `optimizePortfolio`      | FinancialTools       | 马科维茨投资组合优化                                                        |

### SkillScriptRunner 详解

```java
@Tool(description = "在某个技能(skill)包的工作目录下执行脚本或命令。")
public String runInSkill(
    @ToolParam(description = "技能名称，例如 pptx") String skillName,
    @ToolParam(description = "解释器: python, python3, node, bash, sh") String interpreter,
    @ToolParam(description = "脚本文件名或 -c/-e 表示内联代码") String scriptOrCode,
    @ToolParam(description = "脚本参数") String... args)
```

使用示例（AI 调用时自动传入这些参数）：

```
# 执行内联 Python
runInSkill('pptx', 'python', '-c', 'print("hello")')

# 执行脚本文件
runInSkill('pptx', 'python', 'scripts/office/unpack.py', 'input.pptx', 'unpacked/')

# 执行 Node.js 内联
runInSkill('pptx', 'node', '-e', 'console.log(1+1)')
```

**安全特性**：
- 路径穿越检测（`Path.normalize()` + `startsWith()` 校验）
- 60 秒执行超时（超时后 `destroyForcibly()`）
- 执行后自动将新生成的 `.pptx/.pdf/.png` 等输出文件移动到 `ppt/` 目录
- Python 解释器自动检测（优先 `D:\python\python3.10.8\python.exe`）

## PPTX 操作

### 标准工作流

```
unpack.py                   编辑 XML                  clean.py         pack.py
  │                            │                        │                │
  ▼                            ▼                        ▼                ▼
input.pptx  ──→ unpacked/  ──→ 修改 slides/*.xml  ──→ 清理未引用 ──→ output.pptx
               ppt/slides/     ppt/presentation.xml      orphan files
               ppt/theme/      增删改文本/形状/布局
               ppt/media/
               28 个 XML 文件
```

### 命令速查

```bash
# 1. 解包（提取 OOXML 内部结构）
python scripts/office/unpack.py spring_ai.pptx unpacked/

# 2. 查看当前幻灯片
ls unpacked/ppt/slides/         # slide1.xml, slide2.xml, slide3.xml

# 3. 查看可用布局
ls unpacked/ppt/slideLayouts/   # slideLayout1.xml

# 4. 添加一张幻灯片（基于布局）
python scripts/add_slide.py unpacked/ slideLayout1.xml
# → Created slide4.xml from slideLayout1.xml
# → Add to presentation.xml: <p:sldId id="259" r:id="rId10"/>

# 5. 手动编辑 XML（修改文本、调整布局等）
#    编辑 unpacked/ppt/slides/slide4.xml

# 6. 清理未引用文件
python scripts/clean.py unpacked/
# → 自动检测并删除 orphaned slides、rels、media 等

# 7. 打包（验证 + 压缩回 PPTX）
python scripts/office/pack.py unpacked/ output.pptx --original spring_ai.pptx
# → Auto-repaired N issue(s)
# → All validations PASSED!
# → Successfully packed unpacked/ to output.pptx
```

### 脚本依赖矩阵

| 脚本                | Python 标准库 | defusedxml | Pillow | soffice | pdftoppm |
|--------------------|:-----------:|:----------:|:------:|:-------:|:--------:|
| `add_slide.py`     | ✅           |            |        |         |          |
| `unpack.py`        | ✅           | ✅          |        |         |          |
| `clean.py`         | ✅           | ✅          |        |         |          |
| `pack.py`          | ✅           | ✅          |        |         |          |
| `validate.py`      | ✅           | ✅          |        |         |          |
| `thumbnail.py`     | ✅           | ✅          | ✅      | ✅       | ✅        |

### 常见操作示例

**修改文本内容**：

```bash
# 解包后直接编辑 XML
# 在 unpacked/ppt/slides/slide1.xml 中搜索 <a:t> 标签修改文本
# 改完后 clean + pack
```

**从模板创建新演示文稿**：

发给 AI 的提示词示例：
```
请用 spring_ai.pptx 作为模板，创建一个关于"人工智能未来趋势"的 5 页演示文稿。
使用 editing.md 中的工作流：unpack → 复制 slide → 编辑 XML → clean → pack
```

## 数据库

### 开发环境（H2 内存数据库）

- URL: `jdbc:h2:mem:skills_agent;DB_CLOSE_DELAY=-1;MODE=MySQL`
- 控制台: `http://localhost:8081/h2-console`
- JDBC URL 填: `jdbc:h2:mem:skills_agent`
- **数据在应用重启后丢失**

### 生产环境（MySQL）

切换到 MySQL 时修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/skills_agent?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  h2:
    console:
      enabled: false
```

初始化数据库：

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

### 数据表

#### audit_log（审计日志）

| 列名            | 类型         | 说明             |
|----------------|-------------|-----------------|
| id             | BIGINT (PK) | 主键自增          |
| session_id     | VARCHAR(64) | 会话 ID          |
| skill_name     | VARCHAR(128)| 调用的 Agent 名称 |
| user_message   | TEXT        | 用户原始消息      |
| agent_response | TEXT        | AI 回复全文       |
| evaluation     | TEXT        | AI 质检评估结果   |
| timestamp      | DATETIME    | 记录时间          |
| duration_ms    | BIGINT      | 处理耗时（毫秒）   |
| deleted        | TINYINT     | 逻辑删除标记      |

索引：`session_id`, `skill_name`, `timestamp`。

### AI 质检流程

```
saveAuditLog()
    │
    ▼
AuditLogService.logAndEvaluate(auditLog)
    │
    ├── 1. 查找 critic-agent 技能（TEXT 类型）
    ├── 2. 用 SKILL.md 内容作为 System Prompt
    ├── 3. 将 "用户：... AI：..." 作为 User Message
    ├── 4. 调用 ChatClient 获取评估结果
    ├── 5. 设置 auditLog.evaluation
    └── 6. auditLogMapper.insert(auditLog)
```

## 前端界面

`src/main/resources/static/index.html` — 单页 Web 应用，功能：

- Agent 选择器
- 消息输入 + 发送
- SSE 流式文本渲染（打字机效果）
- 错误/超时提示
- Session ID 管理
- 对话历史显示

技术栈：原生 HTML/CSS/JavaScript，无框架依赖。通过 `EventSource` 接收 SSE 流。

## 配置参考

### 完整配置项

| 配置项                                             | 默认值                          | 说明                           |
|---------------------------------------------------|--------------------------------|-------------------------------|
| `server.port`                                     | `8081`                         | HTTP 服务端口                   |
| `server.port` (env)                               | `${SERVER_PORT:8081}`          | 可通过环境变量覆盖               |
| `spring.ai.openai.api-key`                        | 见 application.yml             | DeepSeek API Key               |
| `spring.ai.openai.base-url`                       | `https://api.deepseek.com`     | LLM API 地址                   |
| `spring.ai.openai.chat.options.model`             | `deepseek-chat`                | 模型名称                        |
| `spring.ai.openai.chat.options.temperature`       | `0.7`                          | 生成随机性（0=确定，1=最大随机） |
| `spring.ai.openai.chat.options.max-tokens`        | `8192`                         | 单次回复最大 token 数            |
| `spring.ai.openai.chat.options.timeout`           | `30s`                          | API 调用超时                    |
| `spring.datasource.url`                           | `jdbc:h2:mem:skills_agent`     | 数据库连接                      |
| `spring.datasource.username`                      | `sa`                           | 数据库用户                      |
| `spring.datasource.password`                      | -                              | 数据库密码                      |
| `spring.datasource.driver-class-name`             | `org.h2.Driver`                | JDBC 驱动                      |
| `spring.h2.console.enabled`                       | `true`                         | H2 Web 控制台开关              |
| `spring.h2.console.path`                          | `/h2-console`                  | H2 控制台路径                   |
| `spring.mvc.async.request-timeout`                | `300000`                       | 异步请求超时（ms）               |
| `mybatis-plus.configuration.log-impl`             | `StdOutImpl`                   | SQL 日志输出类                  |
| `mybatis-plus.configuration.map-underscore-to-camel-case` | `true`                   | 下划线转驼峰                     |
| `logging.level.root`                              | `INFO`                         | 根日志级别                      |
| `logging.level.org.example.skillsspringai`        | `DEBUG`                        | 应用日志级别                    |
| `logging.level.org.springframework.ai.chat.client`| `DEBUG`                        | AI 客户端日志                   |
| `logging.level.org.springframework.ai.openai`     | `DEBUG`                        | OpenAI 适配日志                 |
| `logging.level.org.springframework.ai.model.tool` | `DEBUG`                        | Tool Calling 日志              |

### 环境变量

| 变量               | 说明              |
|-------------------|------------------|
| `DEEPSEEK_API_KEY`| API 密钥（推荐使用）|
| `SERVER_PORT`     | 覆盖服务端口       |

## 开发指南

### 项目启动类

```java
@SpringBootApplication
public class SkillsSpringAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkillsSpringAiApplication.class, args);
    }
}
```

### 添加新 Agent

```java
@Component
public class MyAgent extends BaseAgent {

    public MyAgent(
            ChatClient.Builder chatClientBuilder,
            SkillLoaderService skillLoaderService,
            SkillFileReader skillFileReader,
            SkillScriptRunner skillScriptRunner) {

        super(
            "我的Agent",                          // Agent 名称
            "你是一个...（系统提示词）",            // 角色描述
            chatClientBuilder.build(),
            chatClientBuilder
        );

        // 1. 注册技能 Tool（使 SKILL.md 技能对 AI 可见）
        SkillsTool skillsTool = SkillsTool.builder()
                .registry(skillLoaderService.getRegistry())
                .build();
        setSkillsTool(skillsTool);

        // 2. 注册共享工具（脚本执行、文件读写）
        addToolObject(skillFileReader);
        addToolObject(skillScriptRunner);

        // 3. 注册自定义 @Tool Bean（可选）
        // addToolObject(myCustomToolBean);
    }

    @Override
    public void addSkillPackage(SkillPackage skillPackage) {
        this.skillPackages.add(skillPackage);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
```

注册后，可通过 `AgentRegistry` 自动发现：

```java
// AgentRegistry 构造时会扫描所有 Agent 类型的 Bean
AgentRegistry(List<Agent> agents) {
    agents.forEach(a -> this.agents.put(a.getName(), a));
}
```

### 添加新 Skill 包

**Step 1**：在 `.agents/skills/<name>/` 下创建 `SKILL.md`：

```markdown
---
name: my-tools
description: "当用户需要做 X 操作时触发"
license: MIT
---

# My Tools Skill

## 工作流程
1. 先做 A
2. 再做 B

## 脚本
- `scripts/do_x.py` — 执行 X 操作
```

**Step 2**：如果包含脚本，放在 `scripts/` 子目录。

**Step 3**：同时在 `src/main/resources/skills.ppt/<name>/` 下创建相同结构（classpath 加载需要）。

**Step 4**：重启应用，技能自动加载。

### 添加新的 @Tool 方法

在 Spring Bean 中添加 `@Tool` 注解方法即可：

```java
@Component
public class MyTools {

    @Tool(description = "执行某操作，参数说明...")
    public String doSomething(
            @ToolParam(description = "参数说明") String param1,
            @ToolParam(description = "参数说明") int param2) {
        // 实现逻辑
        return "结果";
    }
}
```

然后在 Agent 构造函数中注册：

```java
addToolObject(myTools);
```

### 添加新的 SkillSource

实现 `SkillSource` 接口：

```java
public class DatabaseSkillSource implements SkillSource {
    @Override
    public List<Skill> load() {
        // 从数据库加载技能
    }

    @Override
    public boolean supports(String protocol) {
        return "db".equals(protocol);
    }
}
```

在 `SkillLoaderService` 中注入即可。

### 关键日志输出

开发调试时应关注以下日志：

```
# 技能加载
SkillLoaderService : 加载 [pptx] 类型的技能

# Agent 初始化
SuperFinancialAdvisorAgent : 全能金融顾问大师已初始化 (技能: 5, 通用工具: 3)

# Tool 调用
DefaultToolCallingManager : Executing tool call: runInSkill

# 脚本执行
SkillScriptRunner : 执行脚本: D:\python\python3.10.8\python.exe ...
SkillScriptRunner : 脚本执行成功: office/unpack.py → 55 字符输出

# SSE 流完成
ChatController : SSE stream completed: session=xxx, size=2048 chars

# 审计日志
AuditLogService : 审计日志已保存: session=xxx, duration=15234ms
```

## 调试与排错

### 常见问题

**Q: 启动时报 `Invalid character in request target`**

A: 请求 URL 中中文未编码。使用 `encodeURIComponent()` 编码 Agent 名称和消息。

**Q: `mvnw` 无法执行**

A: 确保文件有执行权限：`chmod +x mvnw`（Linux/Mac）。Windows 使用 `mvnw.cmd`。

**Q: Python 脚本报 `No module named 'defusedxml'`**

A: 安装依赖：`pip install defusedxml`

**Q: SSE 流不显示数据**

A: 检查：
1. DeepSeek API Key 是否正确
2. 网络能否访问 `api.deepseek.com`
3. 查看启动日志中 `spring.ai.openai` 的 DEBUG 日志

**Q: Tool Calling 不工作**

A: 确认 `logging.level.org.springframework.ai.model.tool` 设为 `DEBUG`，查看 AI 是否尝试调用 Tool。如果 AI 不调用 Tool，检查系统提示词是否明确指示了相关能力。

**Q: `thumbnail.py` 执行失败**

A: 该脚本依赖 LibreOffice (`soffice`) 和 Poppler (`pdftoppm`)，且当前版本在 Windows 下兼容性受限。

### 验证技能加载状态

启动后查看日志中：
```
全能金融顾问大师已初始化 (技能: N, 通用工具: 3)
```

- `技能: N` 表示成功加载 N 个 SKILL.md 技能
- `通用工具: 3` 表示 SkillScriptRunner + SkillFileReader + 其他

### 验证 Python 执行能力

```bash
curl -G "http://localhost:8081/api/chat/stream" \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=用runInSkill在pptx技能中执行 python -c 'import sys; print(sys.version)'" \
  --data-urlencode "sessionId=test"
```

预期返回类似 `data:3.10.8 (tags/v3.10.8:aaaf517...)` 的输出。

## 已知问题

1. **`validators/__init__.py` 缺失**
   - 影响：`pack.py` 首次导入 `validators` 包时失败
   - Workaround：Agent 可在运行时检测并自动创建该文件
   - 根因：`.agents/skills/pptx/scripts/office/validators/` 目录缺少 `__init__.py`

2. **`thumbnail.py` Windows 兼容性**
   - 依赖 `soffice`（LibreOffice）、`pdftoppm`（Poppler）、`socket.AF_UNIX`
   - Windows 下无法运行；仅在 Linux/Mac + LibreOffice 环境中可用

3. **中文 URL 编码**
   - Tomcat 默认禁止未编码字符（RFC 7230）
   - `agentName` 和 `message` 中的中文必须经 `encodeURIComponent()` 编码

4. **H2 内存数据库**
   - 审计日志在应用重启后丢失
   - 生产环境需切换到 MySQL（schema 已就绪）

5. **`FinancialTools` 硬编码**
   - `calculateDcf`、`calculateVar` 等方法依赖 `super-financial-advisor` 目录下的 Python 脚本
   - 这些脚本当前未包含在项目资源中（方法会报 `FileNotFound`）
   - 属于示例代码，需补充对应的 Python 脚本文件

6. **Spring AI SSE 多线程日志干扰**
   - MyBatis-Plus `StdOutImpl` 的 `value(Type)` 格式和 DEBUG 日志输出在控制台看起来像乱码
   - 不影响功能，生产环境将 `log-impl` 改为 `Slf4jImpl` 即可
