# wechat-kf-bot

基于 Spring Boot 3 的企业微信客服机器人，面向民宿客服场景。项目支持企业微信客服回调、FAQ 命中、规则分流、MiniMax 大模型兜底回复、会话记忆、Redis 缓存和可选 JDBC 持久化。

## 当前能力

- 企业微信客服 `GET/POST /wecom/kf/callback` 回调校验与消息接收
- 文本消息自动回复，支持企业微信客服 `kf/send_msg`
- 敏感问题优先转人工
- 订单类问题固定追问信息
- `faq.json` 精确 FAQ 命中
- `faq.txt` 作为大模型知识库上下文
- MiniMax 国内 OpenAI 兼容接口调用
- 最近会话上下文记忆
- Redis 缓存 `access_token`、回调去重、最近会话
- 可选 MySQL/JDBC 持久化
- 本地开发聊天接口和 Vue 测试页面
- Knife4j 接口文档
- AI 调用日志单独落盘到 `F:/project/ai-model-debug.log`

## 目录说明

```text
src/main/java/com/example/wechatbot
  config      配置属性、OpenAPI、持久化、诊断日志
  controller  企业微信回调和本地测试接口
  service     回复决策、AI 调用、FAQ、缓存、微信发送等核心逻辑
  repository  内存/JDBC 持久化接口与实现
  model       请求响应模型和回复路由

src/main/resources
  application.yml       通用配置
  application-dev.yml   本地开发配置
  application-prod.yml  生产环境配置
  logback.xml           显式日志配置
  faq.json              精确 FAQ 配置
  faq.txt               大模型知识库文本
  db/schema-mysql.sql   MySQL 表结构

frontend
  Vue 3 本地测试页面
```

## 启动前准备

需要准备以下配置：

- 企业微信 `CorpID`
- 企业微信客服回调 `Token`
- 企业微信客服回调 `EncodingAESKey`
- 企业微信客服 `Secret`
- 客服账号 `open_kfid`
- MiniMax API Key
- 生产环境需要公网 HTTPS 回调地址

本地开发建议通过环境变量提供模型 key：

```powershell
$env:AI_API_KEY="sk-cp-你的完整Key"
```

如果是在 Windows 系统环境变量里配置，配置后需要重启 IDE 或终端。Spring Boot 启动后会打印脱敏诊断日志：

```text
ai_config_loaded baseUrl=..., model=..., configuredApiKey=sk-cp-***xxxx(len=125), envAiApiKey=sk-cp-***xxxx(len=125)
```

如果看到 `<empty>`，说明当前启动进程没有读到环境变量。

## 核心配置

本地配置文件：[application-dev.yml](F:/project/wechat-kf-bot/src/main/resources/application-dev.yml)

```yaml
ai:
  base-url: https://api.minimaxi.com/v1/chat/completions
  api-key: ${AI_API_KEY:}
  model: MiniMax-M2.7
  minimal-test-mode: false
```

`minimal-test-mode=true` 时只发送最小模型请求，用于排查 MiniMax `invalid chat setting (2013)` 一类问题。正常业务使用 `false`。

生产配置文件：[application-prod.yml](F:/project/wechat-kf-bot/src/main/resources/application-prod.yml)

```yaml
ai:
  base-url: ${AI_BASE_URL:https://api.minimaxi.com/v1/chat/completions}
  api-key: ${AI_API_KEY:}
  model: ${AI_MODEL:MiniMax-M2.7}
  minimal-test-mode: ${AI_MINIMAL_TEST_MODE:false}
```

## 启动方式

```powershell
cd F:\project\wechat-kf-bot
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

生产环境：

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

如果本机没有 Maven，可以在 IDE 中直接启动：

[WechatBotApplication.java](F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/WechatBotApplication.java)

## 本地测试

后端测试接口：

```text
POST http://localhost:8080/dev/chat/reply
```

请求示例：

```json
{
  "userId": "dev-user",
  "text": "附近有什么吃的？"
}
```

前端测试页：

```powershell
cd frontend
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

也可以直接打开：

[standalone.html](F:/project/wechat-kf-bot/frontend/standalone.html)

## 企业微信配置

回调地址配置为：

```text
https://你的域名/wecom/kf/callback
```

企业微信后台需要配置：

- URL
- Token，对应 `wecom.token`
- EncodingAESKey，对应 `wecom.aes-key`

客服消息发送还需要：

- `wecom.corp-id`
- `wecom.secret`
- `wecom.kf-account-id`

本地联调需要使用内网穿透工具暴露公网 HTTPS 地址。

## 日志

日志配置文件：

[logback.xml](F:/project/wechat-kf-bot/src/main/resources/logback.xml)

日志文件：

- 项目总日志：`F:/project/wechat-kf-bot.log`
- AI 调用专项日志：`F:/project/ai-model-debug.log`

重点搜索：

```text
ai_config_loaded
ai_reply_request
ai_reply_missing_api_key
ai_reply_http_failed
ai_reply_unexpected_content_type
```

## 接口文档

启动后访问 Knife4j：

```text
http://localhost:8080/doc.html
```

主要接口：

- 企业微信客服回调：`/wecom/kf/callback`
- 本地聊天测试：`/dev/chat/reply`

## 数据存储

默认可使用内存和 Redis 缓存。JDBC 持久化可通过配置开启：

```yaml
persistence:
  jdbc:
    enabled: true
```

MySQL 初始化脚本：

[schema-mysql.sql](F:/project/wechat-kf-bot/src/main/resources/db/schema-mysql.sql)

生产环境可以通过环境变量覆盖数据库和 Redis 配置。

## 常见排查

**模型 key 明文配置能调用，环境变量不能调用**

- 重启 IDE 或启动终端
- 确认启动日志里的 `ai_config_loaded`
- IDEA Run Configuration 中不要覆盖或遗漏 `AI_API_KEY`

**MiniMax 返回 401**

- 检查 key 是否属于国内站
- 国内接口使用 `https://api.minimaxi.com/v1/chat/completions`
- 确认 key 权限和模型权限

**MiniMax 返回 2013**

- 临时设置 `ai.minimal-test-mode=true`
- 确认请求体只包含 `model + messages`
- 再逐步恢复系统提示、知识库和历史上下文

**微信回调校验失败**

- 确认公网 HTTPS 可访问
- 确认 Token 和 EncodingAESKey 一致
- 确认回调路径是 `/wecom/kf/callback`

## 当前待办

- 替换 `application-dev.yml` 中的真实业务配置为本地私有配置或环境变量
- 完善真实民宿 FAQ 和知识库内容
- 接入真实订单系统查询能力
- 增加人工客服后台或人工接管状态
- 增加更多集成测试和回调场景测试
- 生产部署时补充 Docker/进程守护/日志采集方案
