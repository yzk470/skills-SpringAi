# skills-SpringAI-agent

基于 Spring AI 的智能 Agent 技能系统，支持动态技能扩展、流式对话和 LangGraph4j 状态管理。

## 项目架构

```
skills-SpringAi/
├── src/main/java/org/example/skillsspringai/
│   ├── agent/                     # Agent 实现
│   │   └── SuperFinancialAdvisorAgent.java
│   ├── controller/                # REST API 控制器
│   │   └── ChatController.java
│   ├── entity/                    # 数据实体
│   │   ├── AuditLog.java
│   │   ├── AuditLogMapper.java
│   │   ├── ScriptResult.java
│   │   └── Skill.java
│   ├── framework/                 # 核心框架
│   │   ├── Agent.java             # Agent 接口
│   │   ├── SkillPackage.java      # 技能包接口
│   │   └── impl/                  # 框架实现
│   ├── tool/                      # 工具层
│   │   ├── AgentRegistry.java
│   │   ├── AuditLogService.java
│   │   ├── FileSystemSkillRepository.java
│   │   ├── FinancialTools.java
│   │   ├── PythonScriptExecutor.java
│   │   ├── SkillLoaderService.java
│   │   ├── SkillParser.java
│   │   └── SkillsTool.java
│   └── SkillsSpringAiApplication.java
├── src/main/resources/
│   ├── application.properties     # 基础配置
│   ├── application.yml            # 完整配置
│   ├── sql/                       # 数据库脚本
│   │   └── schema.sql
│   └── skills/                    # 技能定义文件
│       └── super-financial-advisor/
├── scripts/                       # 启动脚本
├── docs/                          # 文档
│   └── API.md
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## 技术栈

| 组件              | 版本    | 说明                    |
|-------------------|---------|------------------------|
| Spring Boot       | 3.2.10  | 应用框架                |
| Spring AI         | 1.0.0   | AI 集成框架             |
| LangGraph4j       | 1.5.13  | 图状态管理              |
| MyBatis-Plus      | 3.5.9   | ORM 框架                |
| MySQL             | 8.0     | 关系型数据库             |
| Java              | 17      | 运行环境                |
| Lombok            | latest  | 代码简化                |

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.0+
- Maven Wrapper（已内置）

### 2. 配置 API Key

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入你的 API Key
# OPENAI_API_KEY=sk-your-api-key-here
```

### 3. 启动应用

**Windows:**
```bash
scripts\start.bat
```

**Linux/Mac:**
```bash
chmod +x scripts/start.sh
./scripts/start.sh
```

**Docker:**
```bash
docker compose up -d
```

**Maven 直接启动:**
```bash
./mvnw spring-boot:run
```

### 4. 验证

```bash
curl "http://localhost:8080/api/chat/stream?agentName=全能金融顾问大师&message=你好，请介绍一下你的功能"
```

## API 使用

### 流式对话

```bash
# 基础调用
curl -X GET "http://localhost:8080/api/chat/stream" \
  -G \
  --data-urlencode "agentName=全能金融顾问大师" \
  --data-urlencode "message=帮我分析贵州茅台" \
  --data-urlencode "sessionId=user-001"
```

### 前端集成

```javascript
const eventSource = new EventSource(
  `/api/chat/stream?agentName=全能金融顾问大师&message=${encodeURIComponent(msg)}`
);

eventSource.addEventListener('content', (e) => {
  const { chunk } = JSON.parse(e.data);
  outputDiv.textContent += chunk;
});

eventSource.addEventListener('done', () => eventSource.close());
```

详细 API 文档见 [docs/API.md](docs/API.md)。

## 核心概念

### Agent（智能代理）

Agent 是 AI 驱动的智能实体，封装了特定领域的技能和知识。每个 Agent 可以：
- 加载一组技能包
- 处理用户消息并流式响应
- 维护上下文状态

### Skill（技能）

技能是 Agent 的能力单元，以 JSON 格式定义在 `src/main/resources/skills/` 目录下。每个技能包含：
- 名称和描述
- 输入参数定义（JSON Schema）
- 返回值定义

### 技能扩展

在 `skills/` 目录下添加新的 JSON 文件即可扩展技能：

```json
{
  "name": "my-skill",
  "description": "技能描述",
  "parameters": {
    "type": "object",
    "properties": {
      "param1": { "type": "string", "description": "参数说明" }
    },
    "required": ["param1"]
  }
}
```

## 配置说明

主要配置项（`application.yml`）：

| 配置项                                 | 默认值                  | 说明         |
|---------------------------------------|------------------------|-------------|
| `spring.ai.openai.api-key`            | 环境变量 `OPENAI_API_KEY` | API 密钥     |
| `spring.ai.openai.base-url`           | api.openai.com         | API 地址     |
| `spring.ai.openai.chat.options.model` | gpt-4                  | 模型名称     |
| `spring.datasource.url`               | jdbc:mysql://...       | MySQL 连接串 |
| `mybatis-plus.configuration.log-impl` | StdOutImpl             | SQL 日志输出 |
| `server.port`                         | 8080                   | 服务端口     |

## 开发指南

### 添加新 Agent

1. 继承 `BaseAgent` 类
2. 实现 `addSkillPackage()` 和 `isAvailable()` 方法
3. 添加 `@Component` 注解注册到容器

### 添加新技能

1. 在 `src/main/resources/skills/<agent-name>/` 下创建 JSON 文件
2. 按照技能格式定义参数和返回值
3. 重启应用自动加载

## 数据库

### 初始化

```bash
# 登录 MySQL 执行建表脚本
mysql -u root -p < src/main/resources/sql/schema.sql
```

或使用 Docker Compose 一键启动（MySQL 容器会自动初始化）：

```bash
docker compose up -d
```

### 数据表

| 表名       | 说明         |
|-----------|-------------|
| audit_log | 审计日志表    |
