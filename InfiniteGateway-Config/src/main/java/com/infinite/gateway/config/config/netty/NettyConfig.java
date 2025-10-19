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
     * 业务线程池线程数，默认为CPU核数*2
     * 用于处理网关业务逻辑（过滤器链、限流、灰度、负载均衡等），与IO线程分离
     */
    private int businessThreadNum = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 业务线程池队列大小，默认512
     * 用于背压控制，防止业务积压
     */
    private int businessQueueSize = 512;

}
