### 版本
- Spring Boot 2.7
- JDK 1.8
- Docker 28.0

## 业务功能

- 支持Java代码判题
- 自动识别环境, 优先使用docker, 其次使用JavaNative
- 限流, docker最大50请求, JavaNative最大100请求

## 业务特性

- 三层防护机制:
  - 正则匹配禁止导入危险库
  - docker容器隔离
  - Linux seccomp系统调用限制
  - (准备在JVM层面添加Java程序调用限制)
- 容器复用:
  - 预创建50个容器
