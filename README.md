# Stars API

> 一个开放式的API在线调用平台，为开发者提供便捷、实用、安全的API调用体验
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

&emsp;&emsp;平台的初衷是服务更广泛的用户和开发者，为他们提供便捷的信息和功能获取途径。API在线调用平台旨在协助开发者迅速接入常用服务，以提高他们的开发效率。这些服务包括但不限于随机生成头像、随机壁纸，以及专为二次元爱好者设计的随机动漫图片等。通过提供这些接口，我们帮助开发者更轻松地实现这些功能，同时也为用户提供更丰富的应用体验，提高用户满意度。

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
- 框架：Spring Boot、Mybatis-plus、Spring Cloud
- 数据库：Mysql、Redis
- 中间件：RabbitMq
- 注册中心：Nacos
- 服务调用：Dubbo
- 微服务网关：Spring Cloud Gateway
- 接口文档：Swagger、Knife4j
- 工具库：Hutool、Apache Common Utils、Gson


## 项目模块

- starsapi-frontend ：项目前端。
- starsapi-gateway ：API网关。涉及到路由转发、流量控制、流量染色、集中处理签名校验、请求参数校验以及接口调用统计。
- starsapi-backend ：WEB系统。主要包括用户、接口相关的功能。
- starsapi-interface：模拟接口。提供在线调用的接口。
- starsapi-clint-sdk：客户端SDK。为开发者提供SDK工具包。
- starsapi-common ：公共模块。有公共实体、公共视图，内部服务等。


## 功能模块

- 管理员(管理)
  - 接口创建
  - 接口更新
  - 接口发布
  - 接口下线
  - 接口删除
  - 调用分析：汇总接口调用次数及排名。
  - 发布卡密：一键发布接口充值卡密。
- 体验者(用户)
  - 接口浏览
  - 接口查看
  - 接口充值：采用系统内部支付方式，后期扩展微信和支付宝。
  - 接口调用：采取API签名认证校验接口调用权限。
  - 接口分享
  - 订单汇总
  - 订单删除
  - 秘钥操作：支持查看和重置秘钥。
  - 下载SDK：支持一键本地下载JAR包。
  - 复制依赖：一键复制Maven中央仓库中SDK的依赖。
- 开发者(用户)
  - 接口调用：为开发者引入SDK，一行代码即可轻松调用。
