# 🚀 InfiniteGateway

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.java.net/)
[![Netty](https://img.shields.io/badge/Netty-4.x-red.svg)](https://netty.io/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

> **高性能、可扩展的API网关** - 为微服务架构量身定制的流量入口解决方案

## 核心特性
- **基于 Netty NIO** - 充分利用异步非阻塞I/O，支持高并发处理
- **全异步处理链路** - 使用 AsyncHttpClient 实现请求转发全链路异步化
- **智能线程模型** - 自适应 Epoll/NIO 模型，Linux 下自动启用高性能 Epoll
- **动态过滤器链** - 支持运行时动态组装和调整过滤器执行顺序
- **多阶段处理** - Pre/Post 双阶段过滤器设计，精确控制请求处理流程
- **SPI 扩展机制** - 基于 Java SPI 的插件化架构，轻松扩展自定义功能
- **热插拔支持** - 无需重启即可加载和卸载过滤器插件

## 🏗️ 架构设计

```
┌─────────────────┐    ┌──────────────────────────────────────────┐
│   Client        │    │              InfiniteGateway             │
│                 │    │                                          │
│  ┌─────────────┐│    │ ┌─────────┐  ┌─────────────────────────┐ │
│  │   Request   ││───▶│ │  Netty  │  │    Filter Chain         │ │
│  └─────────────┘│    │ │ Server  │  │                         │ │
│                 │    │ └─────────┘  │ ┌─────┐ ┌─────┐ ┌─────┐ │ │
│  ┌─────────────┐│    │              │ │CORS │ │Auth │ │Rate │ │ │
│  │  Response   ││◀───│              │ │     │ │     │ │Limit│ │ │
│  └─────────────┘│    │              │ └─────┘ └─────┘ └─────┘ │ │
└─────────────────┘    │              │ ┌─────┐ ┌─────┐ ┌─────┐ │ │
                       │              │ │Load │ │Gray │ │Route│ │ │
┌─────────────────┐    │              │ │Bal  │ │     │ │     │ │ │
│   Config        │    │              │ └─────┘ └─────┘ └─────┘ │ │
│   Center        │◀──▶│              └─────────────────────────┘ │
│  (Nacos/ZK)     │    │                                          │
└─────────────────┘    │ ┌──────────────────────────────────────┐ │
                       │ │         AsyncHttpClient              │ │
┌─────────────────┐    │ │                                      │ │
│   Registry      │    │ └──────────────────┬───────────────────┘ │
│   Center        │◀──▶│                    │                     │
│  (Nacos/ZK)     │    └────────────────────┼─────────────────────┘
└─────────────────┘                         │
                                            ▼
                       ┌─────────────────────────────────────────┐
                       │            Backend Services             │
                       │                                         │
                       │ ┌─────────┐ ┌─────────┐ ┌─────────┐     │
                       │ │Service A│ │Service B│ │Service C│     │
                       │ └─────────┘ └─────────┘ └─────────┘     │
                       └─────────────────────────────────────────┘
```

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- Nacos/Zookeeper (可选)

### 1. 下载源码
```bash
git clone https://github.com/he-jw/InfiniteGateway.git

cd InfiniteGateway
```

### 2. Nacos 配置中心路由规则示例
```json
{
    "routes": [
        {
            "id": "user-service-route",
            "serviceName": "user-service",
            "paths": [
                 "/user/**",
                 "/user/ops/**"
             ],
            "filterConfigs": [
                {
                    "name": "flow_filter",
                    "enabled": true
                }
            ]
        }
    ]
}
```

### 3. 网关启动

#### 开发环境启动
```bash
cd InfiniteGateway-Startup

mvn compile exec:java -Dexec.mainClass="com.infinite.gateway.startup.Main"
```

#### 生产环境启动
```bash
mvn clean package -DskipTests

java -jar InfiniteGateway-Startup/target/InfiniteGateway-Startup-*.jar
```


## 📊 性能表现

### 基准测试环境
本地电脑：MacBook Air M2 8C 24G 使用Jmeter进行压测，后端服务只起一个实例节点 InfiniteGateway-Test-User，对比 SpringCloudGateway 和 InfiniteGateway 同等条件压测结果，结果显示InfiniteGateway吞吐量是SpringCloudGateway的两倍多。
### 
SpringCloudGateway 结果：
<img width="1439" height="287" alt="image" src="https://github.com/user-attachments/assets/21fb9849-aca7-4b8b-86a5-d2e0b7d08276" />

InfiniteGateway结果：
<img width="1444" height="285" alt="image" src="https://github.com/user-attachments/assets/11490b94-a40e-4803-8b4d-464659ef3e5a" />


> *性能数据基于典型业务场景测试，实际表现因环境而异*

## 🔧 扩展开发

### 自定义过滤器
```java
@Component
public class CustomAuthFilter implements Filter {
    
    @Override
    public void doPreFilter(GatewayContext context) {
        // 自定义认证逻辑
        String token = context.getRequest().getHeaders().get("Authorization");
        if (!validateToken(token)) {
            throw new AuthenticationException("Invalid token");
        }
        context.doFilter();
    }
    
    @Override
    public String mark() {
        return "custom_auth_filter";
    }
    
    @Override
    public int getOrder() {
        return 100;
    }
}
```

### 自定义负载均衡策略
```java
public class CustomLoadBalanceStrategy implements LoadBalanceStrategy {
    
    @Override
    public ServiceInstance chooseInstance(GatewayContext context,
                                          List<ServiceInstance> instances,
                                          LoadBalanceConfig config) {
        // 自定义负载均衡逻辑
        return instances.get(customSelectIndex(context, instances));
    }
    
    @Override
    public String mark() {
        return "CUSTOM_STRATEGY";
    }
}
```

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。
