# skills-SpringAI-agent API 文档

## 基础信息

- **应用名称**: skills-SpringAI-agent
- **基础路径**: `http://localhost:8080`
- **内容类型**: `application/json`

---

## Chat API

### 流式对话（SSE）

向 AI Agent 发送消息并接收流式响应（Server-Sent Events）。

```
GET /api/chat/stream
```

**查询参数**

| 参数       | 类型   | 必填 | 默认值           | 描述         |
|-----------|--------|------|------------------|-------------|
| agentName | string | 是   | -                | Agent 名称   |
| message   | string | 是   | -                | 用户消息     |
| sessionId | string | 否   | default-session  | 会话标识     |

**响应格式（SSE 事件流）**

```text
event: content
data: {"chunk": "根"}

event: content
data: {"chunk": "据"}

event: content
data: {"chunk": "您的需求..."}

event: done
```

**调用示例**

```bash
# curl 调用
curl -X GET "http://localhost:8080/api/chat/stream?agentName=全能金融顾问大师&message=帮我分析贵州茅台的技术指标&sessionId=user-001"
```

```javascript
// JavaScript EventSource
const eventSource = new EventSource(
  '/api/chat/stream?agentName=全能金融顾问大师&message=帮我分析贵州茅台&sessionId=user-001'
);

eventSource.addEventListener('content', (e) => {
  const { chunk } = JSON.parse(e.data);
  console.log(chunk);
});

eventSource.addEventListener('done', () => {
  console.log('对话完成');
  eventSource.close();
});
```

```python
# Python 调用
import requests

response = requests.get(
    'http://localhost:8080/api/chat/stream',
    params={
        'agentName': '全能金融顾问大师',
        'message': '帮我分析贵州茅台的技术指标',
        'sessionId': 'user-001'
    },
    stream=True
)

for line in response.iter_lines():
    if line:
        print(line.decode('utf-8'))
```

**错误码**

| 状态码 | 描述           |
|--------|---------------|
| 200    | 成功，开始接收流 |
| 404    | Agent 未找到    |
| 500    | 服务器内部错误   |

---

## 可用的 Agent

### 全能金融顾问大师

- **名称**: `全能金融顾问大师`
- **描述**: 提供投资分析、风控计算、组合优化、技术指标分析
- **技能**:
  - `technical-analysis` — 技术指标分析
  - `portfolio-optimizer` — 投资组合优化
  - `risk-calculator` — 风险计算
  - `market-data` — 市场数据查询

---

## 数据库

本服务使用 MySQL 存储审计日志。

- **数据库**: `skills_agent`
- **主要表**: `audit_log` — 记录每次 Agent 对话的审计信息

通过 Docker Compose 启动时会自动创建数据库和表结构。本地开发需手动执行 `src/main/resources/sql/schema.sql`。注意，为了方便起见，也可以使用 `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:skills_agent}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai` 连接数据库进行操作。
