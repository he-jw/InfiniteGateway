package com.infinite.gateway.core;

import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.core.config.Container;
import lombok.extern.slf4j.Slf4j;

/**
 * 网关启动入口类，负责初始化并启动网关核心组件
 */
@Slf4j  // 启用Slf4j日志
public class Bootstrap {

    public static void main(String[] args) {
        Bootstrap.run(args);
    }

    private Config config;          // 网关静态配置对象
    private Container container;    // 网关核心容器（处理网络通信）

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
        // 1. 加载配置, 从gateway.yaml加载配置

        // 2. 初始化配置中心（动态路由管理）

        // 3. 启动容器（核心通信组件）, 启动Netty服务端和客户端

        // 4. 初始化注册中心（服务发现）

        // 5. 注册优雅停机钩子
    }
}
