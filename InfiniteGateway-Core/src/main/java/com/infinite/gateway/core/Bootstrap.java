package com.infinite.gateway.core;

import com.infinite.gateway.common.pojo.ServiceDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.common.util.NetUtil;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.loader.ConfigLoader;
import com.infinite.gateway.config.service.ConfigCenterService;
import com.infinite.gateway.core.manager.DynamicConfigManager;
import com.infinite.gateway.core.netty.Container;
import com.infinite.gateway.dynamic.thread.pool.helper.ThreadPoolRefreshPropertiesHelper;
import com.infinite.gateway.dynamic.thread.pool.registry.RejectedPolicyRegistry;
import com.infinite.gateway.register.service.RegisterCenterService;
import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

/**
 * 网关启动入口类，负责初始化并启动网关核心组件
 */
@Slf4j
public class Bootstrap {

    /**
     * 静态配置
     */
    private Config config;

    /**
     * 容器，负责启动核心通信组件
     */
    private Container container;

    /**
     * 静态启动方法，网关程序入口
     * @param args 命令行参数
     */
    public static void run(String[] args) {
        new Bootstrap().start(args);  // 创建实例并启动
    }

    /**
     * 网关启动主流程
     * @param args 命令行参数
     */
    public void start(String[] args) {
        // 1. 初始化拒绝策略注册表（加载 SPI 扩展）
        RejectedPolicyRegistry.init();
        log.info("RejectedPolicyRegistry initialized");

        // 2. 加载静态配置
        config = ConfigLoader.load(args);

        // 3. 初始化配置中心（动态路由管理）
        initConfigCenter();

        // 4. 启动容器（核心通信组件）, 启动Netty服务端和客户端
        container = new Container(config);
        container.start();

        // 5. 初始化注册中心（服务发现）
        initRegisterCenter();

        // 6. 注册优雅停机钩子
        registerGracefullyShutdown();
    }

    /**
     * 注册JVM停机钩子，实现优雅停机
     */
    private void registerGracefullyShutdown() {
        // 添加JVM关闭时的钩子，当JVM接收到终止信号（如kill命令、程序正常退出或发生系统中断时）会被触发
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 关闭容器释放资源
            container.shutdown();
            log.info("Gateway shutdown gracefully");
        }));
    }

    /**
     * 初始化注册中心
     */
    private void initRegisterCenter() {
        RegisterCenterService nacosRegisterCenterService = ServiceLoader.load(RegisterCenterService.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到注册中心实现"));
        nacosRegisterCenterService.init(config);

        ServiceDefinition serviceDefinition = buildServiceDefinition();
        ServiceInstance serviceInstance = buildServiceInstance();
        nacosRegisterCenterService.register(serviceDefinition, serviceInstance);

        nacosRegisterCenterService.subscribeAllServices((sd, set) -> {
            DynamicConfigManager.getInstance().updateServiceDefinition(sd);
            DynamicConfigManager.getInstance().updateServiceInstance(sd, set);
        });
    }

    /**
     * 构建服务实例
     * @return 服务实例
     */
    private ServiceInstance buildServiceInstance() {
        String ip = NetUtil.getLocalIp();
        return ServiceInstance.builder()
                .serviceName(config.getName())
                .instanceId(ip + ":" + config.getPort())
                .ip(ip)
                .port(config.getPort())
                .enabled(config.getRegisterCenter().isEnabled())
                .weight(1)
                .build();
    }

    /**
     * 构建服务定义
     * @return 服务定义
     */
    private ServiceDefinition buildServiceDefinition() {
        return ServiceDefinition.builder()
                .serviceName(config.getName())
                .env(config.getEnv())
                .enabled(config.getConfigCenter().isEnabled())
                .build();
    }

    /**
     * 初始化配置中心
     */
    private void initConfigCenter() {
        // 1.使用SPI机制加载配置中心实现（如Nacos）
        ConfigCenterService configCenterService = ServiceLoader.load(ConfigCenterService.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到注册中心实现"));
        // 2.初始化配置中心客户端，把从静态yaml文件加载到的 ConfigCenterProperties 的配置传递给配置中心实现
        configCenterService.init(config.getConfigCenter());
        // 3.添加监听器，订阅路由变更事件
        configCenterService.subscribeRoutesChange(newRoutes -> {
            // 更新路由
            DynamicConfigManager.getInstance().updateRoutes(newRoutes);
            // 广播路由变更
            DynamicConfigManager.getInstance().onRouteListeners(newRoutes);
        });
        configCenterService.subscribeThreadPoolParamsChange(ThreadPoolRefreshPropertiesHelper::refresherDynamicThreadPool);
    }


}
