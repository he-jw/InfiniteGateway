package com.infinite.gateway.core.config;

import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.netty.NettyHttpClient;
import com.infinite.gateway.core.netty.NettyHttpServer;

public class Container implements LifeCycle {

    private final NettyHttpClient nettyHttpClient = new NettyHttpClient();
    private final NettyHttpServer nettyHttpServer = new NettyHttpServer();

    @Override
    public void init() {

    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }
}
