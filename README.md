# 🚀 InfiniteGateway

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.java.net/)
[![Netty](https://img.shields.io/badge/Netty-4.x-red.svg)](https://netty.io/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

> **高性能、可扩展的API网关** - 为微服务架构量身定制的流量入口解决方案

## ✨ 核心特性

### 🔥 高性能架构
- **基于 Netty NIO** - 充分利用异步非阻塞I/O，支持高并发处理
- **全异步处理链路** - 使用 AsyncHttpClient 实现请求转发全链路异步化
- **零拷贝优化** - 基于 Netty 的零拷贝技术，减少内存拷贝开销
- **智能线程模型** - 自适应 Epoll/NIO 模型，Linux 下自动启用高性能 Epoll

### 🎯 插拔式过滤器架构
- **动态过滤器链** - 支持运行时动态组装和调整过滤器执行顺序
- **多阶段处理** - Pre/Post 双阶段过滤器设计，精确控制请求处理流程
- **SPI 扩展机制** - 基于 Java SPI 的插件化架构，轻松扩展自定义功能
- **热插拔支持** - 无需重启即可加载和卸载过滤器插件

### ⚡ 智能流量管理
- **多维度限流** - 支持滑动窗口、令牌桶、漏桶三种限流算法
- **动态限流策略** - 支持运行时动态调整限流参数和策略切换
- **精准流控** - 基于服务、接口、用户等多维度的精细化流量控制
- **优雅降级** - 智能识别系统负载，自动触发限流和降级机制

### 🔄 智能负载均衡
- **多策略支持** - 轮询、随机、权重、一致性哈希等多种负载均衡算法
- **健康检查** - 实时监控后端服务健康状态，自动剔除异常节点
- **动态权重** - 支持运行时动态调整服务实例权重
- **会话保持** - 基于一致性哈希的会话亲和性支持

### 🌐 多中心支持
- **多注册中心** - 支持 Nacos、Zookeeper 等主流注册中心
- **多配置中心** - 集成 Nacos Config 等配置中心，实现配置热更新
- **服务发现** - 自动发现和注册微服务，支持服务上下线动态感知
- **配置同步** - 分布式配置实时同步，确保集群配置一致性

### 🛡️ 弹性和容错
- **熔断保护** - 集成 Resilience4j 提供熔断、重试、超时等容错机制
- **优雅降级** - 支持自定义降级策略和 Fallback 响应
- **故障隔离** - 基于舱壁模式的资源隔离，防止故障传播
- **自动恢复** - 智能检测服务恢复状态，自动开启半开状态探测

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
git clone https://github.com/your-username/InfiniteGateway.git

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
本地电脑：MacBook Air M2 8C 24G 使用Jmeter进行压测，后端服务只起一个实例节点 InfiniteGateway-Test-User，对比 SpringCloudGateway 和 InfiniteGateway 同等条件压测结果
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

## 📋 路线图

### v1.0 (当前版本)
- ✅ 核心网关功能
- ✅ 基础过滤器支持
- ✅ 多种负载均衡策略
- ✅ 基础监控指标

### v2.0 (未来规划)
- 📋 gRPC 协议支持
- 📋 AI 智能路由
- 📋 可视化配置管理

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 贡献方式
1. 🐛 提交 Bug 报告
2. 💡 提出新功能建议
3. 📖 改进文档
4. 💻 提交代码

### 开发流程
1. Fork 项目
2. 创建特性分支: `git checkout -b feature/amazing-feature`
3. 提交更改: `git commit -m 'Add amazing feature'`
4. 推送分支: `git push origin feature/amazing-feature`
5. 提交 Pull Request

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。
---

**如果这个项目对你有帮助，请给我一个 ⭐️ Star！** 
