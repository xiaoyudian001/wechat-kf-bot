# wechat-kf-bot

基于 Spring Boot 3 的微信客服机器人示例，适用于民宿客服场景。

## 已实现能力

- 企业微信微信客服回调验签与解密
- 敏感问题优先转人工
- 订单类问题固定追问
- FAQ 优先命中
- FAQ 文本知识库作为大模型上下文
- 最近 3 轮会话上下文记忆
- 统一日志记录回复路径
- 回调短期幂等去重，避免重复回复
- 微信客服 `send_msg` 回发客户消息

## 项目结构

- `src/main/java/com/example/wechatbot/controller`
  - 微信客服回调入口
- `src/main/java/com/example/wechatbot/service`
  - 回复决策、FAQ、敏感词、订单识别、消息发送等服务
- `src/main/resources/application.yml`
  - 企业微信与大模型配置
- `src/main/resources/faq.json`
  - FAQ 精确匹配配置
- `src/main/resources/faq.txt`
  - 提供给大模型的业务知识文本

## 启动前准备

你需要准备以下信息：

- 企业微信 `CorpID`
- 微信客服回调 `Token`
- 微信客服回调 `EncodingAESKey`
- 微信客服 `Secret`
- 客服账号 `open_kfid`
- 大模型 `API Key`
- 一个公网可访问的 `HTTPS` 地址

本地开发可以填写到 [application-dev.yml](/F:/project/wechat-kf-bot/src/main/resources/application-dev.yml)，生产环境建议通过环境变量注入 [application-prod.yml](/F:/project/wechat-kf-bot/src/main/resources/application-prod.yml)：

- `wecom.corp-id`
- `wecom.token`
- `wecom.aes-key`
- `wecom.secret`
- `wecom.kf-account-id`
- `ai.api-key`

## 微信客服后台配置步骤

### 1. 创建客服账号

登录微信客服后台：

- [微信客服后台](https://work.weixin.qq.com/kf)

创建一个客服账号，并记下该客服账号对应的 `open_kfid`。

### 2. 开启 API 接入

在微信客服后台找到该客服账号的 API 管理或开发配置页，确认该账号允许通过 API 管理消息收发。

如果没有开启 API 管理：

- 你的服务端即使收到回调
- 也无法调用微信客服接口主动回复客户

### 3. 配置回调地址

将回调地址配置为：

```text
https://你的域名/wecom/kf/callback
```

需要同时配置：

- `URL`
- `Token`
- `EncodingAESKey`

其中：

- `Token` 要和 [application.yml](/F:/project/wechat-kf-bot/src/main/resources/application.yml) 的 `wecom.token` 一致
- `EncodingAESKey` 要和 [application.yml](/F:/project/wechat-kf-bot/src/main/resources/application.yml) 的 `wecom.aes-key` 一致

### 4. 获取微信客服 Secret

在微信客服开发配置页获取 `Secret`，填入：

- `wecom.secret`

这个值用于服务端换取 `access_token`，再调用：

- `gettoken`
- `kf/send_msg`

### 5. 获取客服链接并发起会话

在微信客服后台生成客服入口链接，让用户从该入口发起咨询。

只有用户真正进入微信客服会话后，这个机器人才能开始收消息、自动回复。

## 本地配置步骤

### 1. 修改配置文件

编辑 [application-dev.yml](/F:/project/wechat-kf-bot/src/main/resources/application-dev.yml)，填入本地联调参数。

示例：

```yaml
wecom:
  corp-id: wwxxxxxxxxxxxxxxxx
  token: your_callback_token
  aes-key: your_encoding_aes_key
  secret: your_wechat_kf_secret
  kf-account-id: your_open_kfid

ai:
  api-key: sk-xxxx
```

### 2. 修改 FAQ

如果你要替换成自己的民宿信息，优先修改：

- [faq.json](/F:/project/wechat-kf-bot/src/main/resources/faq.json)
- [faq.txt](/F:/project/wechat-kf-bot/src/main/resources/faq.txt)

推荐做法：

- 高频固定问答写进 `faq.json`
- 业务知识、规则说明写进 `faq.txt`

## 启动方式

进入项目目录：

```bash
cd F:\project\wechat-kf-bot
```

运行：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

生产环境建议使用 `prod` profile，并通过环境变量提供密钥：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

如果你本机没有 Maven，可以：

1. 安装 Maven 后再运行
2. 或者在 IDE 里以 Spring Boot 应用方式启动 [WechatBotApplication.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/WechatBotApplication.java)

## 回调联调步骤

### 1. 先确保服务可被公网访问

微信客服回调必须能访问你的服务，所以你至少要满足：

- 服务已经启动
- 地址是公网 `HTTPS`
- 路径可访问：`/wecom/kf/callback`

如果你本地开发，可以先用内网穿透工具把本地 `8080` 暴露成公网 HTTPS 地址。

### 2. 配置回调并完成校验

在微信客服后台保存回调地址时，微信会发起一次 `GET` 请求做校验。

本项目里对应接口是：

- [WechatKfCallbackController.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/controller/WechatKfCallbackController.java)

方法：

- `GET /wecom/kf/callback`

如果：

- `Token` 配错
- `EncodingAESKey` 配错
- 回调地址不是公网 HTTPS

那么后台校验会失败。

### 3. 发起一条测试消息

用微信打开客服入口，发送一条纯文本消息，例如：

```text
几点入住？
```

成功后，服务端会收到 `POST /wecom/kf/callback` 回调。

### 4. 查看日志

项目会记录几类关键日志：

- 微信回调消息
- 命中路径
- 最终回复内容
- 异常信息

重点看类似日志：

```text
chat_reply userId=xxx route=FAQ_MATCH userText=几点入住 reply=您好，我们这边通常是15:00后可以办理入住。
```

如果命中大模型，会看到：

```text
chat_reply userId=xxx route=AI_REPLY userText=附近有地铁吗 reply=您好，附近有地铁站，出行比较方便。
```

### 5. 验证不同路径

你可以分别测试这几类问题：

- FAQ 问题
  - `几点入住`
  - `可以带宠物吗`
- 敏感问题
  - `我要退款`
  - `我要投诉`
- 订单问题
  - `我明天怎么入住`
  - `帮我查订单`
- 普通咨询
  - `附近有什么吃的`

预期结果：

- FAQ 问题优先命中标准答案
- 敏感问题直接转人工
- 订单问题固定追问订单信息
- 其他问题走大模型

## 常见排查点

### 1. 微信后台提示回调校验失败

优先检查：

- 回调地址是否公网 `HTTPS`
- `Token` 是否一致
- `EncodingAESKey` 是否一致
- 企业微信配置的应用和客服账号是否正确

### 2. 能收到回调，但无法回消息

优先检查：

- `wecom.secret` 是否正确
- `wecom.kf-account-id` 是否正确
- 客服账号是否已开启 API 管理
- 用户是否在官方允许的发送窗口内

### 3. FAQ 没命中，直接走了大模型

优先检查：

- [faq.json](/F:/project/wechat-kf-bot/src/main/resources/faq.json) 的关键词是否覆盖用户说法
- 用户提问是否带了很多噪声表达
- 是否被订单规则或敏感词规则提前拦截

### 4. 模型回复不稳定

优先检查：

- [faq.txt](/F:/project/wechat-kf-bot/src/main/resources/faq.txt) 是否包含足够业务信息
- [application.yml](/F:/project/wechat-kf-bot/src/main/resources/application.yml) 的 `system-prompt` 是否过于宽泛
- 当前消息是否本该由 FAQ 或规则优先处理

## 当前实现边界

这版项目是一个 MVP，当前有这些限制：

- 只处理文本消息
- 会话上下文只保存在内存中，重启后会丢失
- 没接真实订单系统
- 没做人工坐席后台
- 没做数据库持久化

## 持久化准备

仓库中已经补了第一版持久化骨架，便于下一步接 MySQL 或其他关系型数据库：

- 表结构脚本：[schema-mysql.sql](/F:/project/wechat-kf-bot/src/main/resources/db/schema-mysql.sql)
- 消息实体：[ChatMessageEntity.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/entity/ChatMessageEntity.java)
- 回调事件实体：[CallbackEventEntity.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/entity/CallbackEventEntity.java)
- 仓储接口：[ChatMessageRepository.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/repository/ChatMessageRepository.java)、
  [CallbackEventRepository.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/repository/CallbackEventRepository.java)
- 服务门面：[PersistentConversationMemoryService.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/service/PersistentConversationMemoryService.java)、
  [PersistentCallbackEventService.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/service/PersistentCallbackEventService.java)

当前这些类还没有接入主流程，目的是先把数据模型和接口边界稳定下来。下一步可以在不改业务规则的前提下补 JDBC 或 JPA 实现，再把内存方案逐步替换掉。

当前仓库已经补了 `JdbcTemplate` 版本的仓储实现，但默认仍然关闭：

- JDBC 消息仓储：[JdbcChatMessageRepository.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/repository/jdbc/JdbcChatMessageRepository.java)
- JDBC 回调仓储：[JdbcCallbackEventRepository.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/repository/jdbc/JdbcCallbackEventRepository.java)
- 可选装配配置：[PersistentStorageConfig.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/config/PersistentStorageConfig.java)

启用条件：

- `persistence.jdbc.enabled=true`
- 配置 `spring.datasource.url`
- 配置 `spring.datasource.username`
- 配置 `spring.datasource.password`

在当前版本里，这些 JDBC Bean 只是在 Spring 容器中可选注册，还没有替换现有的内存消息链路，目的是先保证数据库接入面稳定。

现在主流程已经支持“双实现切换”：

- 默认情况下继续使用内存版会话与回调幂等。
- 当 `persistence.jdbc.enabled=true` 且数据源可用时，会自动优先使用 JDBC 版会话存储与回调去重。

这意味着：

- `dev` 环境可以零成本继续本地调试。
- `prod` 环境可以逐步切到数据库，不需要一次性重写业务规则层。

数据库初始化与异常处理补充：

- `prod` 环境默认会通过 `spring.sql.init` 自动执行 [schema-mysql.sql](/F:/project/wechat-kf-bot/src/main/resources/db/schema-mysql.sql)
- 若不希望启动时自动建表，可以覆盖 `SPRING_SQL_INIT_MODE=never`
- JDBC 仓储现在会在数据库读写失败时记录结构化错误日志，并抛出统一的 `IllegalStateException`

## Redis 缓存

本项目现在支持把 Redis 作为短期状态缓存层使用，MySQL 仍然负责长期可追溯数据。

已经接入 Redis 的内容：

- 企业微信 `access_token`：减少重复调用企业微信 `gettoken`
- 回调短期去重：通过 Redis `SETNX` 快速拦截重复回调
- 最近会话缓存：缓存最近 3 轮对话，减少频繁查 MySQL

本地 `dev` 环境默认连接：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

Redis 相关开关：

```yaml
redis-cache:
  enabled: true
  key-prefix: wechat-kf-bot
  callback-dedupe-ttl-minutes: 10
  conversation-ttl-hours: 24
```

常用 key 形态：

```text
wechat-kf-bot:wecom:access_token
wechat-kf-bot:callback:dedupe:{dedupeKey}
wechat-kf-bot:chat:recent:{externalUserId}
```

## 本地测试页面

项目下提供了一个 Vue 3 测试台，用于直接调用后端 dev 测试接口，不需要模拟企业微信加密回调。

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

前端目录：

- [frontend](/F:/project/wechat-kf-bot/frontend)

如果 Vite 被本机权限策略拦截，可以直接打开免构建页面：

- [standalone.html](/F:/project/wechat-kf-bot/frontend/standalone.html)

启动方式：

```bash
cd frontend
npm install
npm run dev
```

页面默认访问地址：

```text
http://localhost:5173
```

## 代码注释规范

后端 Java 类和方法统一使用 JavaDoc 注释，新增类或方法时需要包含：

```java
/**
 * 描述：简单介绍类或方法作用。
 *
 * @author wangjw
 * @date 2026-04-24
 */
```

## 接口文档

项目已接入 Knife4j，后端启动后访问：

```text
http://localhost:8080/doc.html
```

当前文档分组：

- `企业微信客服接口`：包含企业微信 URL 校验和消息回调接口
- `本地测试接口`：包含本地聊天测试接口 `/dev/chat/reply`

但它已经足够用于：

- 跑通微信客服机器人回调
- 验证 FAQ + 大模型自动回复
- 验证民宿微信自动答疑场景

## 相关代码入口

- [WechatKfCallbackController.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/controller/WechatKfCallbackController.java)
- [AiReplyService.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/service/AiReplyService.java)
- [KfMessageService.java](/F:/project/wechat-kf-bot/src/main/java/com/example/wechatbot/service/KfMessageService.java)
- [faq.json](/F:/project/wechat-kf-bot/src/main/resources/faq.json)
- [faq.txt](/F:/project/wechat-kf-bot/src/main/resources/faq.txt)
#项目概要
