# InfiniteGateway

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.java.net/)
[![Netty](https://img.shields.io/badge/Netty-4.x-red.svg)](https://netty.io/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

基于 Netty 与动态线程池技术，自主设计并实现了一个高性能、全异步的微服务 API 网关。该架构通过多 Reactor 模型支撑高并发网络通信，利用动态线程池保障系统弹性与资源利用率，并借助 AsyncHttpClient 完成全链路无阻塞处理。其核心是可插拔的动态过滤器链，借此提供了包括智能路由、灰度分流、精准流量治理、负载均衡及熔断降级在内的一站式流量管理能力，赋能微服务体系稳定高效运行。

## 基于多 Reactor 多线程架构设计请求交互流程
<img width="699" height="820" alt="image" src="https://github.com/user-attachments/assets/faead986-1be5-4bd3-8d8e-4fc4120b49fe" />

## 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- Nacos/Zookeeper (可选)

### 1. 下载源码
```bash
git clone https://github.com/he-jw/InfiniteGateway.git

cd InfiniteGateway
```

### 2. Nacos 配置中心规则示例

#### 2.1. 路由配置
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
#### 2.2. 动态线程池配置
```json
{
  "enable": true,
  "executors": [
    {
      "threadPoolId": "gateway-biz-executor",
      "corePoolSize": 8,
      "maximumPoolSize": 10,
      "queueCapacity": 1000,
      "workQueue": "VARIABLE_LINKED_BLOCKING_QUEUE",
      "rejectedHandler": "GatewayJdkPolicy",
      "keepAliveTime": 60,
      "allowCoreThreadTimeOut": false,
      "notify": {
        "receives": "admin@example.com",
        "interval": 5
      },
      "alarm": {
        "enable": true,
        "queueThreshold": 80,
        "activeThreshold": 80
      }
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
