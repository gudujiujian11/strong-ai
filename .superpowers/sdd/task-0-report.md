# Task 0：Java 8 测试编译前置修复

## 状态

修复完成；测试源码编译通过，DocumentServiceTest 的 10 项测试全部通过。指定 Maven 全量范围执行完成，但因另两项 Spring 上下文测试中的 Arthas 端口绑定错误而失败。

## 修改

- 文件：`strong-ai-webapp/src/test/java/cn/strong/webapp/user/documentgovernance/service/DocumentServiceTest.java`。
- 新增 `cn.strong.webapp.user.model.documentgovernance.DiffResponse` import。
- 将 `diffComparesAnyTwoVersions`、`diffAgainstDraftUsesDraftContent` 中两处 `var response` 替换为 `DiffResponse response`。
- 已检查 `DocumentService.diff` 的返回类型确为 `DiffResponse`。未修改业务代码、构建配置或其他测试逻辑，未创建 commit。

## 验证命令与结果

在仓库根目录运行：

```sh
MAVEN_OPTS='-Dfile.encoding=UTF-8 -DforkCount=0 -DfailIfNoTests=false' /Users/zhangchunxia/jdk/apache-maven-3.6.3/bin/mvn -pl strong-ai-webapp -am test
```

- 退出码：1，`BUILD FAILURE`。
- `strong-ai`、`strong-ai-model`、`strong-ai-mq-model`、`strong-ai-remote-api` 均为 SUCCESS。
- webapp 测试源码重新编译成功（23 个源文件）。
- 测试汇总：60 项，Failures 0、Errors 2、Skipped 0。
- `DocumentServiceTest`：10 项，Failures 0、Errors 0、Skipped 0。
- 错误用例：`AiChatControllerTest.chat`、`ExampleServiceTest.test1`，均在加载 ApplicationContext 时无法实例化 `arthasAgent`。
- 两份 Surefire 文本报告的最底层原因一致：`Arthas failed to bind telnet or http port! Telnet port: 3658, http port: 8563`。
- 个别依赖插件版本缺失及废弃 API 警告仍存在，不属于本次改动范围。

证据文件位于 `strong-ai-webapp/target/surefire-reports/` 下的 `cn.strong.webapp.user.documentgovernance.service.DocumentServiceTest.txt`、`cn.strong.webapp.user.controller.AiChatControllerTest.txt` 和 `cn.strong.webapp.user.service.ExampleServiceTest.txt`。

## 自检

- `git diff --check` 通过。
- 核对 diff，仅新增一个 import 并替换两处局部变量类型；断言、调用参数和业务行为未变化。
- 已有 `.superpowers/` 及设计/计划文档保持不变，本报告除外。
- Java 8 语法问题已消除；全套测试尚未全绿，剩余失败为 Arthas 初始化端口绑定问题，本任务未扩大范围修改该配置。
