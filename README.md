# strong-ai
## 项目简介
### 功能简介


## 技术栈
- spring boot2.4.2, spring cloud2020,
- mysql, mybatis, mybatis-plus, hikaricp,
- xxl-job
- redis-cluster
- rabbitmq
- nacos
- dubbo


## git仓库地址


## 接口地址


## 备注


## 使用说明
- 服务器端口号：11028

## 开发环境及依赖


## 快速启动

## swagger地址

http://localhost:11028/doc.html

## 使用示例

### AI 聊天

AI 调用由 `ai4j-spring-boot-starter` 自动装配。`bootstrap.yml` 的 `ai.platforms` 以 `id` 作为接口的 `provider`，以 `platform` 指定模型厂商实现。密钥必须通过环境变量或 Nacos 密文配置注入。

```bash
curl -X POST http://localhost:11028/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"provider":"deepseek","model":"deepseek-chat","messages":[{"role":"user","content":"你好"}]}'
```

## 注意

- swagger 文档

1. 只允许在**测试环境**开启, 其它环境**不允许**开启
2. 默认关闭, 如需启用, 请在**配置中心**(如 nacos `config.properties`)中增加配置 `strong.swagger.enable = true`
