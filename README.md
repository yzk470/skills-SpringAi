# skills-SpringAI-agent

基于 Spring AI 的智能 Agent 技能系统，支持动态技能扩展（SKILL.md）、Python/Node 脚本执行、PPTX 文档操作和流式对话。

## 项目架构

```
skills-SpringAi/
├── .agents/skills/                    # 技能包定义（SKILL.md + Python/Node 脚本）
│   ├── pptx/                          # PPTX 操作技能包
│   │   ├── SKILL.md                   # 技能入口定义
│   │   ├── editing.md                 # 编辑工作流指引
│   │   ├── pptxgenjs.md              # 从零创建指引
│   │   └── scripts/                   # Python 脚本
│   │       ├── add_slide.py           # 添加幻灯片
│   │       ├── clean.py               # 清理未引用文件
│   │       ├── thumbnail.py           # 缩略图预览（需 soffice/pdftoppm）
│   │       └── office/
│   │           ├── unpack.py          # 解包 PPTX → 目录
│   │           ├── pack.py            # 打包目录 → PPTX
│   │           ├── validate.py        # 验证/修复
│   │           ├── soffice.py         # LibreOffice 辅助
│   │           ├── validators/        # XSD 验证器（⚠ 缺少 __init__.py）
│   │           └── helpers/           # DOCX 辅助工具
│   ├── frontend-design/               # 前端设计技能
│   ├── frontend-code-review/          # 前端代码审查技能
│   └── backend-code-review/           # 后端代码审查技能
├── ppt/                               # PPTX 输出目录
├── src/main/java/org/example/skillsspringai/
│   ├── agent/                         # Agent 实现
│   │   └── SuperFinancialAdvisorAgent.java
│   ├── controller/                    # REST API
│   │   └── ChatController.java       # 流式/非流式对话接口
│   ├── entity/                        # 数据实体
│   │   ├── AuditLog.java
│   │   ├── ScriptResult.java
│   │   └── Skill.java
│   ├── framework/                     # 核心框架
│   │   ├── Agent.java
│   │   ├── SkillPackage.java
│   │   └── impl/BaseAgent.java
│   └── tool/                          # 工具层
│       ├── AgentRegistry.java         # Agent 注册中心
│       ├── SkillRegistry.java         # Skill 注册中心
│       ├── SkillLoaderService.java    # 多源加载服务
│       ├── SkillParser.java           # SKILL.md 解析器
│       ├── SkillSource.java           # 技能源接口
│       ├── SkillsTool.java            # Spring AI Tool 适配
│       ├── SkillScriptRunner.java     # Python/Node 脚本执行
│       ├── SkillFileReader.java       # 技能文件读取
│       ├── FinancialTools.java        # 金融工具（硬编码示例）
│       ├── AuditLogService.java       # 审计日志服务
│       ├── PackageClassLoader.java    # 动态类加载器
│       └── PythonScriptExecutor.java  # Python 专用执行器
├── src/main/resources/
│   ├── static/index.html              # Web 前端界面
│   ├── application.yml                # 应用配置
│   └── skills.ppt/pptx/               # 编译时技能包资源
└── pom.xml
```

## 技术栈

| 组件           | 版本    | 说明                    |
|----------------|---------|------------------------|
| Spring Boot    | 3.2.10  | 应用框架                |
| Spring AI      | 1.0.0   | AI 集成框架（Tool Calling）|
| DeepSeek API   | -       | deepseek-chat 模型      |
| H2 Database    | -       | 内存数据库（dev）        |
| MyBatis-Plus   | 3.5.9   | ORM 框架                |
| Java           | 17      | 运行环境                |
| Python         | 3.10.8  | 技能脚本执行            |
| Lombok         | latest  | 代码简化                |

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven（已内置 Wrapper）
- Python 3.10+（用于 PPTX 等技能脚本，可选）
- `pip install defusedxml`（PPTX 技能必须）

### 2. 配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}        # 你的 DeepSeek API Key
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
          max-tokens: 8192
```

### 3. 启动

```bash
./mvnw spring-boot:run
```

应用默认运行在 `http://localhost:8081`，Web 界面在 `http://localhost:8081/`。

### 4. 验证

```bash
# 流式对话（需 URL 编码中文）
curl -G "http://localhost:8081/api/chat/stream" \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=你好" \
  --data-urlencode "sessionId=test-001"

# 非流式对话
curl -G "http://localhost:8081/api/chat" \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=你好"
```

## API

### 流式对话（SSE）

```
GET /api/chat/stream?agentName=<name>&message=<msg>&sessionId=<sid>
```

返回 `text/event-stream`，每个 chunk 为独立的 `data:` 行（Spring MVC 自动封装 SSE 格式）。

前端 EventSource 集成：

```javascript
const url = `/api/chat/stream?agentName=${encodeURIComponent(name)}&message=${encodeURIComponent(msg)}`;
const eventSource = new EventSource(url);

// Spring SSE 默认触发 onmessage
eventSource.onmessage = (e) => {
  if (e.data.startsWith('[ERROR]')) {
    console.error(e.data);
  } else {
    outputDiv.textContent += e.data;
  }
};

// 连接关闭表示流结束
eventSource.onerror = () => eventSource.close();
```

### 非流式对话

```
GET /api/chat?agentName=<name>&message=<msg>&sessionId=<sid>
```

返回 JSON：

```json
{
  "content": "完整回复文本",
  "agentName": "全能金融顾问大师",
  "sessionId": "test-001",
  "durationMs": 15234
}
```

## 技能系统

### Skill 类型

| 类型   | 格式       | 说明                                   |
|--------|-----------|----------------------------------------|
| TEXT   | SKILL.md   | 提供文本指引，AI 读取后按指引操作       |
| PACKAGE | skill.json | 包含 Java 类的工具包，通过 @Tool 注解暴露 |

实际工具执行通过 `SkillScriptRunner`、`SkillFileReader` 等共享工具完成。

### PPTX 技能工作流

标准编辑流程：

```bash
# 1. 解包 PPTX
python scripts/office/unpack.py input.pptx unpacked/

# 2. 添加/复制幻灯片
python scripts/add_slide.py unpacked/ slideLayout1.xml

# 3. 编辑 XML（在 unpacked/ppt/slides/ 下修改）

# 4. 清理未引用文件
python scripts/clean.py unpacked/

# 5. 重新打包
python scripts/office/pack.py unpacked/ output.pptx --original input.pptx
```

Agent 通过以下 Tool 执行这些操作：

| Tool               | 用途                         |
|--------------------|-----------------------------|
| `runInSkill`       | 在技能目录下执行 Python/Node 脚本 |
| `writeFileInSkill` | 在技能目录下创建/修改文件      |
| `readFileInSkill`  | 读取技能目录下的文件           |
| `getOutputDir`     | 获取输出目录（ppt/）          |

### 可用 Python 脚本

| 脚本                          | 用途              | 依赖                        |
|-------------------------------|-------------------|----------------------------|
| `scripts/office/unpack.py`    | 解包 OOXML 文件    | `defusedxml`               |
| `scripts/office/pack.py`      | 打包为 OOXML 文件  | `defusedxml`               |
| `scripts/office/validate.py`  | XSD 验证与自动修复 | `defusedxml`               |
| `scripts/add_slide.py`        | 新增/复制幻灯片    | 无（纯标准库）              |
| `scripts/clean.py`            | 清理未引用文件     | `defusedxml`               |
| `scripts/thumbnail.py`        | 缩略图网格预览     | `defusedxml`, `Pillow`, `soffice`, `pdftoppm` |

### 技能加载架构

```
SkillLoaderService（统一入口）
    ├── ClasspathMarkdownSkillSource    → classpath:skills.ppt/**/SKILL.md
    ├── FileSystemMarkdownSkillSource   → .agents/skills/**/SKILL.md
    ├── ClasspathPackageSkillSource     → classpath 下的 skill.json 包
    └── UrlSkillSource                  → 远程下载技能包
              ↓
        SkillRegistry（统一注册中心）
              ↓
          SkillsTool（暴露为 Spring AI ToolCallback）
```

## 已知问题

1. **`validators/__init__.py` 缺失**：`pack.py` 导入 `validators` 包时会失败，Agent 可在运行时自动创建该文件作为 workaround
2. **`thumbnail.py` 仅限 Unix**：依赖 `soffice` 和 `socket.AF_UNIX`，Windows 下无法运行；需安装 LibreOffice 和 Poppler
3. **中文 URL 编码**：API 调用时中文参数必须 URL 编码，否则 Tomcat 会报 `Invalid character` 错误
4. **H2 数据库**：当前使用内存数据库，重启后审计日志丢失；生产环境需切换 MySQL

## 配置参考

| 配置项                                      | 默认值                    | 说明           |
|--------------------------------------------|--------------------------|---------------|
| `server.port`                              | `8081`                   | 服务端口       |
| `spring.ai.openai.api-key`                 | 见 application.yml       | DeepSeek API Key |
| `spring.ai.openai.base-url`                | `https://api.deepseek.com` | API 地址     |
| `spring.ai.openai.chat.options.model`      | `deepseek-chat`           | 模型           |
| `spring.datasource.url`                    | `jdbc:h2:mem:skills_agent` | H2 内存数据库  |
| `mybatis-plus.configuration.log-impl`      | `StdOutImpl`              | SQL 日志       |
| `logging.level.org.example.skillsspringai` | `DEBUG`                   | 应用日志级别   |

## 开发指南

### 添加新 Agent

```java
@Component
public class MyAgent extends BaseAgent {
    public MyAgent(ChatClient.Builder builder, SkillLoaderService loader, ...) {
        super("Agent名称", "系统提示词", builder.build(), builder);
        // 注册技能
        SkillsTool skillsTool = SkillsTool.builder()
                .registry(loader.getRegistry()).build();
        setSkillsTool(skillsTool);
        // 注册工具
        addToolObject(skillScriptRunner);
        addToolObject(skillFileReader);
    }
}
```

### 添加新 Skill 包

1. 在 `.agents/skills/<skill-name>/` 下创建 `SKILL.md`（含 frontmatter）
2. 如需脚本，放在 `scripts/` 子目录下
3. 确保类路径 `src/main/resources/skills.ppt/` 中有对应资源
4. 重启应用自动加载

### SKILL.md 格式

```markdown
---
name: my-skill
description: "技能描述 — AI 会根据此描述决定何时触发"
license: MIT
---

# 技能标题

## 工作流程
具体操作指引...
```
