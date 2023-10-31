# Stars API

> 一个开放式的API在线调用平台，为开发者提供便捷、实用、安全的API调用体验。
>
>  Spring Boot + React 全栈项目
>
> 源码地址：[https://github.com/stars-coding/starsapi](https://github.com/stars-coding/starsapi)
>


## 项目展示

- 平台主页

![平台主页](https://github.com/stars-coding/starsapi/blob/master/image/平台主页.png)

- 接口详情

![接口详情](https://github.com/stars-coding/starsapi/blob/master/image/接口详情.png)

- 我的接口

![我的接口](https://github.com/stars-coding/starsapi/blob/master/image/我的接口.png)

- 接口充值

![接口充值](https://github.com/stars-coding/starsapi/blob/master/image/接口充值.png)

- 我的订单

![我的订单](https://github.com/stars-coding/starsapi/blob/master/image/我的订单.png)

- 个人中心

![个人中心](https://github.com/stars-coding/starsapi/blob/master/image/个人中心.png)

- 接口管理

![接口管理](https://github.com/stars-coding/starsapi/blob/master/image/接口管理.png)

- 接口分析

![接口分析](https://github.com/stars-coding/starsapi/blob/master/image/接口分析.png)


## 项目背景

&emsp;&emsp;平台的初衷是服务更广泛的用户和开发者，为他们提供一站式便捷的信息和功能获取途径。API在线调用平台的目标是协助开发者快速接入各种常用服务，从而提高他们的开发效率。这些服务包括但不限于随机头像生成、百度热搜数据、聊天机器人等。通过提供这些接口，我们助力开发者更轻松地实现各种功能，同时也为用户提供更丰富的应用体验，从而提升用户满意度和开发者生产力。

## 系统架构
![系统架构](https://github.com/stars-coding/starsapi/blob/master/image/系统架构.png)


## 技术堆栈

### 前端技术栈

- 开发框架：React、Umi
- 脚手架：Ant Design Pro
- 组件库：Ant Design、Ant Design Components
- 语法扩展：TypeScript、Less
- 生成工具：OpenAPI
- 打包工具：Webpack
- 代码规范：ESLint、StyleLint、Prettier

### 后端技术栈

- 主语言：Java
- 框架：Spring Boot、MyBatis Plus、Spring Cloud
- 数据库：MySQL、Redis
- 中间件：RabbitMQ
- 注册中心：Nacos
- 服务调用：Dubbo RPC
- 微服务网关：Spring Cloud Gateway
- 接口文档：Swagger、Knife4j
- 工具库：Hutool、Apache Common Utils、Gson


## 项目架构

- starsapi-frontend：项目前端。
- starsapi-gateway：API网关。涉及路由转发、流量控制、集中签名校验、请求参数校验以及接口调用统计等。
- starsapi-backend：管理后台。主要包括用户、接口相关的功能。
- starsapi-interface：接口模拟。提供在线调用的接口。
- starsapi-common：共享模块。有公共实体、公共视图，内部服务等。
- starsapi-clint-sdk：客户端SDK。为开发者提供SDK工具包。


## 功能模块

- 管理员(管理)
  - 接口创建：创建新接口。
  - 接口更新：对原有的某个接口进行信息更新。
  - 接口发布：将接口上线，对用户开放。
  - 接口下线：将接口下线，对用户关闭。
  - 接口删除：对平台中的某个接口进行删除。
  - 调用分析：汇总接口调用次数及排名。
  - 发布卡密：一键发布接口充值卡密。
- 体验者(用户)
  - 接口浏览：浏览某个接口的大体信息。
  - 接口查看：详细查看某个的口的全部信息。
  - 接口充值：采用系统内部支付方式，后期扩展微信和支付宝。
  - 接口调用：采取API签名认证校验接口调用权限。
  - 接口分享：对外分享接口。
  - 订单汇总：汇总或筛选支付订单。
  - 订单删除：对某个支付订单进行删除。
  - 秘钥操作：支持查看和重置秘钥。
  - 下载SDK：支持一键本地下载JAR包。
  - 复制依赖：一键复制Maven中央仓库中SDK的依赖。
- 开发者(用户)
  - 接口调用：为开发者引入SDK，一行代码即可轻松调用。
