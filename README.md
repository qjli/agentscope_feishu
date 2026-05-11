# agentscope-feishu

基于 **AgentScope Java** 与 **飞书开放平台（Lark / Feishu）** 的示例集成：在 IM 中接收用户消息，经 **ReActAgent** 推理与工具调用后，通过 Open API 回复；支持 **JsonSession** 会话持久化、**message_id** 幂等、以及 **ToolSuspend + 交互卡片** 的人机协同（HITL）演示。

---

## 背景与定位

飞书负责 **消息通道与用户交互形态**（事件订阅、发消息、卡片回调）；AgentScope Java 负责 **推理—工具—记忆**（`ReActAgent`、`Toolkit`、`Session`、`Hook` 等）。二者通过薄适配层完成 **飞书事件 ↔ `Msg`** 的映射。

典型可扩展场景包括：单聊/群 Copilot、企业知识助手（结合文档 API + AgentScope RAG）、办公工具链（日程/多维表格等封装为 `@Tool`）、运维告警机器人、以及多智能体在服务端编排后仍由单一机器人入口对外。

---

## 技术栈与版本

| 组件 | 说明 |
|------|------|
| JDK | 17+ |
| Spring Boot | 3.3.x（Jakarta Servlet） |
| AgentScope Java | `io.agentscope:agentscope` 1.0.12 |
| 飞书 SDK | `com.larksuite.oapi:oapi-sdk` 2.4.19 |
| 其他 | Caffeine（消息幂等）、Project Reactor（AgentScope 异步） |

模型侧当前示例使用 **阿里云 DashScope（通义）**；亦可按 [AgentScope 安装文档](https://java.agentscope.io/) 替换为 OpenAI 等兼容实现。

---

## 架构概览

```mermaid
flowchart LR
  subgraph feishu [Feishu]
    IM[IM事件]
    API[OpenAPI]
  end
  subgraph adapter [Adapter]
    WH["/webhook/event"]
    EVT[EventDispatcher]
  end
  subgraph agentscope [AgentScope]
    AG[ReActAgent]
    TK[Toolkit]
    SB[SkillBox]
    SS[JsonSession]
  end
  IM --> WH
  WH --> EVT
  EVT --> AG
  AG --> TK
  AG --> SB
  TK --> API
  AG --> SS
```

- **Webhook**：`FeishuWebhookController` 将 HTTP 请求体与 Header 转为 `EventReq`，调用 `EventDispatcher.handle`（无需旧版 `ServletAdapter` 的 `javax.servlet`）。
- **异步**：`im.message.receive_v1` 在 `feishuEventExecutor` 中异步处理，避免阻塞飞书回调线程。
- **会话**：`sessionId = sanitize(chatId) + "_" + sanitize(openId)`，可选话题群追加 `_t_<threadId>`；`JsonSession` 持久化目录可配置。
- **并发**：同一 `sessionId` 使用 `FeishuSessionLockRegistry` 互斥；**每个请求新建 `ReActAgent` 实例**（符合 AgentScope「有状态 Agent 不可并发共享」约束）。

---

## 仓库结构（核心代码）

```
src/main/java/io/agentscope/feishu/
├── AgentscopeFeishuApplication.java    # 启动类
├── agent/
│   └── FeishuSessionAgentFactory.java  # ReActAgent + Toolkit + Session 加载/保存
├── config/
│   ├── FeishuBeansConfiguration.java   # Client、EventDispatcher、JsonSession、线程池
│   └── FeishuProperties.java           # agentscope.feishu.* 配置
├── crm/                                 # CRM Mock REST、卡片变量、Skill 绑定工具（见下文「CRM（Skill）」）
│   ├── CrmRemoteClient.java
│   ├── CrmReplyFormatter.java
│   ├── skill/                           # classpath 技能 feishu_crm 的 Java 适配
│   │   ├── CrmClasspathSkillHolder.java
│   │   └── CrmSkillTools.java
│   └── web/CrmMockApiController.java    # GET /api/crm/customerInfo、/api/crm/orders
├── lark/
│   ├── FeishuMessageEventService.java   # 收消息：幂等、串行、ReActAgent（含 SkillBox）、TOOL_SUSPENDED
│   ├── FeishuMessageSender.java         # 发文本 / 交互卡片
│   ├── FeishuTextContentParser.java     # 解析 text 消息 content JSON
│   └── PendingApprovalService.java      # 卡片回调恢复 ToolSuspend
├── session/
│   ├── SessionIdSanitizer.java          # sessionId 安全校验（防路径穿越）
│   └── FeishuSessionLockRegistry.java
├── tools/
│   ├── FeishuLarkTools.java             # 飞书 API → @Tool（Mono）
│   ├── FeishuToolContext.java         # chatId 注入（ToolExecutionContext）
│   └── ApprovalTools.java             # ToolSuspend 演示
└── web/
    └── FeishuWebhookController.java     # POST /webhook/event

src/main/resources/skills/feishu_crm/    # AgentScope 技能包：SKILL.md + references（ClasspathSkillRepository 根为 skills/）
```

---

## 功能交付清单

| 能力 | 说明 |
|------|------|
| 事件订阅 | `im.message.receive_v1`；卡片 **`card.action.trigger`（P2CardActionTrigger）** 用于审批恢复 |
| 消息回复 | `CreateMessage` 向当前 `chat_id` 发文本；交互卡片 schema `2.0` |
| Agent | `ReActAgent` + DashScope；未配置模型 Key 时对用户文本做 Echo 提示 |
| Session | `JsonSession`，`loadIfExists` / `saveTo`；目录见 `agentscope.feishu.session-dir` |
| 飞书 Tool | `getCurrentChatMetadata`、`sendFollowUpTextToCurrentChat`（`Mono`）；`feishuPing` 演示 **presetParameters** |
| 幂等 | `message_id` 写入 Caffeine，TTL 可配置 |
| HITL | `request_sensitive_action_approval` 抛 `ToolSuspendException` → 发卡片 → 回调里 `agent.call` 工具结果消息并继续推理 |
| **CRM（Skill）** | 用户消息统一进入 `ReActAgent`；`SkillBox` 注册 classpath 技能 `feishu_crm`，模型按需 `load_skill_through_path` 后调用 `crm_send_*` 工具 → `CrmRemoteClient` + **飞书模板卡片**（失败回退纯文本） |

### CRM（AgentScope Skill）

CRM 能力仅通过 **Skill + 渐进式工具** 提供：`FeishuSessionAgentFactory` 为每个会话挂载 `SkillBox`，技能定义在 `src/main/resources/skills/feishu_crm/`（`SKILL.md` + `references/`），Java 适配在 `io.agentscope.feishu.crm.skill`（`CrmClasspathSkillHolder`、`CrmSkillTools`）。

推荐用户话术（与 `references/crm-patterns.md` 一致；公司名可替换，`陕西中辰海锋新能源有限公司` 为 Mock 富数据示例）：

| 用户输入 | 后端效果（由模型调用工具完成） |
|---------|------|
| `查询<公司> 客户基本信息` | `crm_send_customer_info_card` → `GET /api/crm/customerInfo` |
| `查询<公司>`（整句不含「订单」） | 同上 |
| `查询<公司>订单量` | `crm_send_orders_summary_card(..., ALL)` → `GET /api/crm/orders` |
| `查询<公司>已结算的订单量` | `crm_send_orders_summary_card(..., SETTLED)` |

配置：`agentscope.crm.base-url`（留空则 `http://127.0.0.1:{server.port}`，适合本机 Mock 与网关反代后的真实后端）。

客户信息卡片（与 [飞书文档：使用指定应用发送飞书卡片](https://open.feishu.cn/document/feishu-cards/quick-start/send-feishu-cards-with-app-bots) 一致）：

- `agentscope.crm.customer-info-use-template-card`：是否用模板卡片（默认 `true`）。
- `agentscope.crm.customer-info-card-template-id`：搭建工具中的卡片 ID（默认 `AAqtrZyWSproW`，可用环境变量 `FEISHU_CUSTOMER_CARD_TEMPLATE_ID` 覆盖）。
- `agentscope.crm.customer-info-card-template-version`：模板版本号，留空使用平台最新已发布版本。

接口字段 → 卡片变量映射见 `CustomerInfoCardVariables`（`baseCompanyName`、`baseCompanyBizCode`、`baseCompanyType` 等）。

订单统计卡片（模板 ID 默认 `AAqtroVtGb0Yc`）：

- `agentscope.crm.orders-use-template-card`、`orders-card-template-id`、`orders-card-template-version`（或环境变量 `FEISHU_ORDERS_CARD_TEMPLATE_*`）。
- 变量映射见 `OrdersCardVariables`：`companyName`、`unSettledNum`、`settledNum`、`invoicedNum`、`totalNum`（均为字符串传入模板）。`/api/crm/orders` 的 Mock 会按请求中的 **companyName** 生成数据（演示企业为富数据，其它为基于名称 hash 的确定性占位）。

其它非 CRM 问题仍由同一 `ReActAgent` 与通用飞书工具处理。

---

## 快速开始

### 1. 环境变量（推荐，勿将密钥写入 Git）

| 变量 | 含义 |
|------|------|
| `FEISHU_APP_ID` | 飞书自建应用 App ID |
| `FEISHU_APP_SECRET` | 飞书自建应用 App Secret |
| `FEISHU_VERIFICATION_TOKEN` | 事件订阅 Verification Token |
| `FEISHU_ENCRYPT_KEY` | 事件加密 Encrypt Key（启用加密时必填） |
| `AGENTSCOPE_MODEL_DASHSCOPE_API_KEY` | DashScope API Key |
| `AGENTSCOPE_MODEL_NAME` | 模型名，默认可用 `qwen-turbo` 等 |

应用内等价配置前缀：`agentscope.feishu.*`、`agentscope.model.*`（见 `src/main/resources/application.yml`）。

### 2. 飞书开放平台配置（摘要）

1. 创建企业自建应用，启用 **机器人**，开通 **接收消息** 等相关权限。  
2. **事件订阅**：配置请求地址 `https://<你的公网域名>/webhook/event`，订阅 **接收消息 v2.0**（`im.message.receive_v1`）。  
3. 若使用审批卡片：订阅 **卡片 action** 对应事件（与 SDK 中 `P2CardActionTrigger` 一致），同一 URL 或按控制台要求配置。  
4. 将控制台中的 **Verification Token / Encrypt Key** 与代码或环境变量对齐。

### 3. 构建与运行

```bash
mvn test
mvn spring-boot:run
```

默认 HTTP 端口见 `application.yml` 中 `server.port`（可改为 `8080` 等）。

### 4. 健康检查

可自行增加 `GET /actuator/health`（需引入 `spring-boot-starter-actuator`）；当前仓库未默认开启。

---

## 安全与合规提示

- **密钥与 Token**：务必通过环境变量或私密配置中心注入；**不要**把 `app_secret`、模型 API Key、verification token 提交到公开仓库。  
- **sessionId**：仅允许安全字符，防止 JsonSession 目录穿越（实现见 `SessionIdSanitizer`）。  
- **日志 / 会话文件**：可能含用户内容与提示词，生产环境需脱敏、限权与留存策略。  
- **幂与重推**：飞书可能对事件重推；`message_id` 去重可降低重复回复风险。

---

## 测试

```bash
mvn test
```

包含 `SessionIdSanitizerTest` 等对会话主键规则的单元测试。

---

## 延伸阅读

- [AgentScope Java 文档](https://java.agentscope.io/)  
- [飞书开放平台 - 接收消息事件](https://open.feishu.cn/document/server-docs/im-v1/message/events/receive)  
- 本仓库实现对应调研中的 **Webhook 接入层 + Session 隔离 + Toolkit 飞书工具 + 异步与幂等 + 卡片恢复 ToolSuspend**；若需 **WebSocket 长连接** 接收事件，可在同应用内并行接入飞书 SDK 的长连接模式（与当前 HTTP 回调正交）。

---

## License

若无特别声明，以仓库根目录 `LICENSE` 为准（若尚未添加，请自行补充）。
