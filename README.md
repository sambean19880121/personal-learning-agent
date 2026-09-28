# Personal Learning Agent

本地运行的中文学习助手。它从技术资讯中准备学习材料，生成三个问题，并在提交答案后展示逐题评分、扣分原因和参考答案。学习记录保存在本机 SQLite 数据库中。

## 环境要求

- Java 21
- Maven 3.9 或更新版本
- macOS（仅系统通知依赖 `osascript`；网页和 API 可在其他系统运行）
- 网络连接（获取资讯、调用 DeepSeek，以及加载网页使用的 Vue CDN）

## 配置

应用默认监听 `18080` 端口，数据库位于 Java 的 `${user.home}/learning-agent.db`。在 macOS 的当前用户下，`${user.home}` 通常是 `/Users/你的用户名`。日志写入项目工作目录下的 `logs/learning-agent.log`。

如需使用 DeepSeek 生成学习内容和批改答案，在 `src/main/resources/` 中创建**不会提交到 Git** 的 `application-local.yml`：

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY}
```

启动前设置环境变量：

```bash
export DEEPSEEK_API_KEY='你的 API Key'
```

也可以直接把密钥写在本地配置文件中，但不要提交该文件。项目的 `.gitignore` 已排除 `application-local.yml`。没有可用的 DeepSeek 配置或请求失败时，应用会使用内置学习内容；批改会显示**按回答长度计算的临时分数**，它不能判断答案是否正确，参考内容可能只包含文章或预设要点。

其他配置可以在 `application-local.yml` 中覆盖，例如：

```yaml
server:
  port: 18081
spring:
  datasource:
    url: jdbc:sqlite:/绝对路径/learning-agent.db
```

目前提醒时间在 `ReminderScheduler` 中固定为 **Asia/Shanghai 每天 21:00**，应用必须保持运行才能触发。`application.yml` 中的 `agent.reminder-time` 目前没有接入调度逻辑，修改它不会改变提醒时间。

## 启动

在项目根目录执行：

```bash
mvn spring-boot:run
```

打开 <http://localhost:18080/>。如果修改了 `server.port`，请使用对应端口。按 `Ctrl+C` 停止应用。

## 使用方式

1. 在网页中选择学习方向，或使用自动推荐，然后点击“开始练习”。文章下方会显示资讯来源和“查看原文”链接。
2. 阅读文章并回答三个问题，点击提交。
3. 查看总分、逐题评分原因和参考答案。总分是逐题分数的平均值，四舍五入为整数。
4. 修改答案后可以重新批改；刷新网页可查看当天已保存的结果。

首次准备学习内容时会尝试抓取资讯源，速度取决于网络。生成器会结合资讯标题和 RSS 摘要，写成约 800–1200 字的分段讲解；它不会自动读取完整原文，涉及版本细节时请核对来源链接。自动推荐会优先加载当天已保存的学习内容；手动选择方向会生成新的当天内容。

来源和原文链接取自生成文章时选中的资讯条目，不由 AI 编造。没有资讯或 AI 不可用时，内置课程标注为“内置课程”，没有原文链接。旧文章会按标题匹配已抓取的资讯回填；无法匹配的旧文章会显示“旧文章未记录来源”。

## API

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/api/learning/today` | 查看当前学习内容 |
| `GET` 或 `POST` | `/api/learning/start?track=DATABASE` | 开始学习，可选指定方向 |
| `POST` | `/api/learning/evaluate` | 提交 `{ "answers": ["答案1", "答案2", "答案3"] }` |
| `GET` | `/api/learning/evaluation` | 查看当天已保存的批改结果 |
| `GET` | `/api/knowledge/latest` | 查看最近收集的资讯 |
| `POST` | `/api/knowledge/refresh` | 手动刷新资讯源 |

`/api/learning/evaluate` 返回每题的 `score`、`feedback`、`referenceAnswer`，以及说明评分方式的 `scoringMethod`。如果显示“临时分数”，说明本次 AI 批改不可用。

## 数据与安全

SQLite 数据库、日志、构建产物和本地密钥配置都不提交到 Git。项目当前只面向本机使用，API 没有用户认证；不要直接暴露到公网。源码变更记录见 [change.log](change.log)。
