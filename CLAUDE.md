# Forward 后端

## 语言要求
请始终使用中文回答。

## 项目概述
天谷数智化中心产研管理系统后端主服务，处理业务需求、产品需求、项目、任务、Bug、故障单等核心业务。

## 技术栈
- Java Spring Boot
- Maven 构建（父 POM: mandarin-pure-bom 2.13.2）
- MyBatis ORM
- Redis 缓存（tedis）
- 消息队列（esign-mq）
- 定时任务（schedulerT）
- Lombok + MapStruct + Hutool + Guava
- EasyExcel 导出

## Maven
Maven 路径: `~/apache-maven-3.6.0/bin/mvn`

## 模块结构
```
forward/
├── facade/   # API 接口定义（Query 类、Facade 接口）
├── model/    # 数据模型
├── dal/      # 数据访问层（MyBatis Mapper）
├── service/  # 业务逻辑 + Controller
└── deploy/   # 部署配置
```

## 接口查找
- Controller: `service/src/main/java/com/timevale/forward/service/controller/`
- Query 参数: `facade/src/main/java/com/timevale/forward/facade/api/query/`
- 搜索 `@PostMapping` 或 `@GetMapping` 找接口定义

## API Context Path
前端请求前缀为 `/infocenter-manager/forward`，对应本服务的接口。
