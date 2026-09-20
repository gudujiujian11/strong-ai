# Task 2 实施报告

状态：完成。未创建 commit，未修改策略服务、API 或任务链路。

## 范围与实现

- 新增 `strong-ai-webapp/src/main/java/cn/strong/webapp/user/documentgovernance/service/AiOptimizationStrategyCompiler.java`。
  - 提供 `normalizeAndValidate(String)`、`configHash(String)`、`compileSystemPrompt(AiOptimizationStrategyConfig, String, String)`。
  - 使用独立 Jackson mapper 严格拒绝未知字段、非法枚举、重复字段、尾随 JSON 和标量强制转换。
  - 基于 `defaults()` 的结构逐字段递归合并，避免部分 directions/protections/constraints/model 对象覆盖完整默认值；显式 JSON null 被拒绝，缺失字段补默认。
  - 方向固定六项，强度固定 low/medium/high，mode 固定 format/polish/restructure。
  - customInstruction trim 后按 Java String.length() 检查不超过 2000；provider/name trim 后非空，maxTokens 为正整数。
  - 规范化 JSON 的对象属性与 map key 按字母排序，哈希直接取该 JSON 的 UTF-8 字节进行 SHA-256，输出 64 位小写十六进制字符串。
  - Prompt 固定顺序为：安全约束、四个兼容输出标记、文档上下文、固定顺序的启用方向和强度、保护规则、输出约束、独立团队补充要求数据块。
  - 自定义要求和上下文用 JSON 字符串表达，团队要求明确为不可信数据、低于固定安全及结构化约束；不记录配置、指令或 Prompt，Jackson 错误及 cause 不向外透传。
- 新增 `strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/AiOptimizationStrategyCompilerTest.java`，共 12 项测试。
- 修改 `strong-ai-model/src/main/java/cn/strong/webapp/user/model/documentgovernance/AiOptimizationStrategyCompiledContext.java`，增加 `normalizedJson` 和四参构造，保留原三参构造。
- 复用 Task 1 的 `AiOptimizationStrategyConfig.defaults()` 和方向/强度枚举，未改动其内容。

## TDD 证据

先完整写入测试，再运行 RED；确认缺失编译器导致失败之后，才实现生产代码。

RED/GREEN 使用同一命令：

```bash
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am -Dtest=AiOptimizationStrategyCompilerTest test
```

RED，2026-09-20 16:29:12 +08:00，退出码 1，关键输出：

```text
[ERROR] AiOptimizationStrategyCompilerTest.java:[19,19] 找不到符号
  符号: 类 AiOptimizationStrategyCompiler
[INFO] strong-ai-webapp ................................... FAILURE
[INFO] BUILD FAILURE
```

该 RED 与 brief 的预期一致：编译器尚不存在，测试无法编译；不是依赖、环境或拼写问题。

GREEN，2026-09-20 16:31:48 +08:00，退出码 0，关键输出：

```text
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.756 s
[INFO] strong-ai-webapp ................................... SUCCESS
[INFO] BUILD SUCCESS
[INFO] Total time: 28.426 s
```

Surefire 摘要：`strong-ai-webapp/target/surefire-reports/cn.strong.webapp.user.documentgovernance.service.AiOptimizationStrategyCompilerTest.txt`。

测试覆盖默认值、嵌套默认合并、未知方向/字段、非法枚举/mode、非法 JSON/null/重复字段/类型、模型参数、2000 字符边界、配置顺序/空白/显式默认值的哈希等价、哈希与 normalizedJson 字节一致性、错误信息不泄露配置、Prompt 安全顺序和保护规则、关闭方向不编译、编译部分对象不修改调用方对象。

## 自检

- Java 8 兼容；未新增依赖，未修改 pom、部署或环境变量文件。
- 已确认 required methods 可由无参构造的编译器直接调用，编译器作为 Spring Component 可注入。
- `getNormalizedJson()` 返回计算 hash 的准确原始字符串；策略服务应直接保存此值，避免二次序列化改变字节表示。
- `normalizeAndValidate` 返回默认文档上下文的 Prompt；有文档类型/读者时，由调用方再次调用 `compileSystemPrompt`。
- 按固定枚举次序编译方向，规范化存储按 key 排序，两者互不依赖。
- `git diff --check` 通过；检查编译器未发现 logger、日志输出或 printStackTrace。
- 只编辑上述三个生产/测试文件与本报告，保留已有用户和其他任务改动。

## 关注点与未验证项

- 经父代理确认，现有 `AiChatService` 只有 provider 注册表校验及 model 非空校验，没有静态 model 白名单；本任务没有杜撰白名单，provider 支持性仍由实际模型调用阶段的注册表校验。
- 约定显式 JSON null 为非法配置；Java 部分对象编译时，未赋值字段通过 NON_NULL 序列化视为缺失并补默认。
- 仅运行本任务范围的 12 项单元测试，未运行整仓测试、外部模型调用和完整任务链路。文档正文的实际 user message 包裹仍归后续链路任务；本编译器只声明沿用现有正文分隔协议。
- Maven 仍输出项目既有的父 POM 插件版本缺失、既有 API 弃用和不 fork 时 useSystemClassloader 无效等警告；未为此扩大修改范围。
- Prompt 约束降低注入风险，但输出保护校验属于后续任务，不以 Prompt 文本约束代替结果校验。
