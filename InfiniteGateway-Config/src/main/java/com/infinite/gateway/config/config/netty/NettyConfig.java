package com.infinite.gateway.config.config.netty;

import lombok.Data;

/**
 * netty配置
 */
@Data
public class NettyConfig {

    private int eventLoopGroupBossNum = 1;

    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors();

    private int maxContentLength = 64 * 1024 * 1024;

    /**
     * 业务线程池线程数，默认为CPU核数
     * 用于处理网关业务逻辑（过滤器链、限流、灰度、负载均衡等），与IO线程分离
     */
    private int businessThreadNum = Runtime.getRuntime().availableProcessors();

    /**
     * 业务线程池队列大小
     */
    private int businessQueueSize = 512;

}
