package com.infinite.gateway.core;

import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.loader.ConfigLoader;
import com.infinite.gateway.config.manager.DynamicConfigManager;
import com.infinite.gateway.config.service.ConfigCenterProcessor;
import com.infinite.gateway.core.config.Container;
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
        // 1. 加载静态配置
        config = ConfigLoader.load(args);
        // 2. 初始化配置中心（动态路由管理）
        initConfigCenter();
        // 3. 启动容器（核心通信组件）, 启动Netty服务端和客户端
        container = new Container(config);
        container.start();
        // 4. 初始化注册中心（服务发现）

        // 5. 注册优雅停机钩子

        log.info("debug");
    }

    /**
     * 初始化配置中心
     */
    private void initConfigCenter() {
        // 1.使用SPI机制加载配置中心实现（如Nacos）
        ConfigCenterProcessor configCenterProcessor = ServiceLoader.load(ConfigCenterProcessor.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到注册中心实现"));

        // 2.初始化配置中心客户端，把从静态yaml文件加载到的 ConfigCenterProperties 的配置传递给配置中心实现
        configCenterProcessor.init(config.getConfigCenter());
        // 3.添加监听器，订阅路由变更事件
        configCenterProcessor.subscribeRoutesChange(newRoutes -> {
            DynamicConfigManager.getInstance().updateRoutes(newRoutes);
        });
    }
}
