package com.infinite.gateway.core.netty;

import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.netty.processor.NettyCoreProcessor;

public class Container implements LifeCycle {

    private final NettyHttpClient nettyHttpClient;
    private final NettyHttpServer nettyHttpServer;

    public Container(Config config) {
        this.nettyHttpClient = new NettyHttpClient(config);
        this.nettyHttpServer = new NettyHttpServer(config.getNetty(), new NettyCoreProcessor(), config);
    }

    @Override
    public void start() {
        nettyHttpServer.start();
        nettyHttpClient.start();
    }

    @Override
    public void shutdown() {
        nettyHttpServer.shutdown();
        nettyHttpClient.shutdown();
    }
}
