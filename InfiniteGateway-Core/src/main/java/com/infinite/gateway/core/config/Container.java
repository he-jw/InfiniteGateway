package com.infinite.gateway.core.config;

import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.netty.NettyHttpClient;
import com.infinite.gateway.core.netty.NettyHttpServer;
import com.infinite.gateway.core.netty.processor.NettyCoreProcessor;

public class Container implements LifeCycle {

    private final NettyHttpClient nettyHttpClient;
    private final NettyHttpServer nettyHttpServer;

    public Container(Config config) {
        this.nettyHttpClient = new NettyHttpClient();
        this.nettyHttpServer = new NettyHttpServer(config.getNetty(), new NettyCoreProcessor(), config);
    }

    @Override
    public void start() {
        nettyHttpClient.start();
        nettyHttpServer.start();
    }

    @Override
    public void shutdown() {
        nettyHttpClient.shutdown();
        nettyHttpServer.shutdown();
    }
}
