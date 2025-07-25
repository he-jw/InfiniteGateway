package com.infinite.gateway.config.config.netty;

import lombok.Data;

/**
 * netty配置
 */
@Data
public class NettyConfig {

    private int eventLoopGroupBossNum = 1;

    private int eventLoopGroupWorkerNum = Runtime.getRuntime().availableProcessors() * 2;

    private int maxContentLength = 64 * 1024 * 1024;

}
