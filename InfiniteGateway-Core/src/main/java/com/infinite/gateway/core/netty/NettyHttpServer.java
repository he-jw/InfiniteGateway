package com.infinite.gateway.core.netty;

import com.infinite.gateway.common.util.SystemUtil;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.config.NettyConfig;
import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.netty.handler.NettyHttpServerHandler;
import com.infinite.gateway.core.netty.processor.NettyProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;

/**
 * Netty HTTP服务器实现，负责接收客户端请求
 * 实现LifeCycle接口管理生命周期
 */
public class NettyHttpServer implements LifeCycle {

    private Config config;
    private NettyConfig nettyConfig;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    private EventLoopGroup eventLoopGroupWorker;
    private final NettyProcessor nettyProcessor;

    public NettyHttpServer(NettyConfig nettyConfig, NettyProcessor nettyProcessor, Config config) {
        this.config = config;
        this.nettyConfig = nettyConfig;
        this.nettyProcessor = nettyProcessor;
        init();
    }

    /**
     * 初始化Netty服务端组件
     */
    private void init() {
        // 创建ServerBootstrap实例
        this.serverBootstrap = new ServerBootstrap();

        // 根据操作系统类型选择IO模型
        if (SystemUtil.useEpoll()) {
            // Linux系统使用Epoll模型
            this.eventLoopGroupBoss = new EpollEventLoopGroup(
                    nettyConfig.getEventLoopGroupBossNum(),  // boss线程数
                    new DefaultThreadFactory("epoll-netty-boss-nio") // 线程命名
            );
            this.eventLoopGroupWorker = new EpollEventLoopGroup(
                    nettyConfig.getEventLoopGroupWorkerNum(), // worker线程数
                    new DefaultThreadFactory("epoll-netty-worker-nio")
            );
        } else {
            // 其他系统使用NIO模型
            this.eventLoopGroupBoss = new NioEventLoopGroup(
                    nettyConfig.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("default-netty-boss-nio")
            );
            this.eventLoopGroupWorker = new NioEventLoopGroup(
                    nettyConfig.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("default-netty-worker-nio")
            );
        }
    }

    /**
     * 启动Netty服务器
     */
    @Override
    public void start() {
        // 配置服务器参数
        serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)  // 设置线程组
                // 选择通道类型（Epoll或NIO）
                .channel(SystemUtil.useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                // TCP参数配置
                .option(ChannelOption.SO_BACKLOG, 1024)          // 连接等待队列大小
                .option(ChannelOption.SO_REUSEADDR, true)        // 端口重用
                .option(ChannelOption.SO_KEEPALIVE, true)        // 保持连接
                .childOption(ChannelOption.TCP_NODELAY, true)    // 禁用Nagle算法
                .childOption(ChannelOption.SO_SNDBUF, 65535)     // 发送缓冲区大小
                .childOption(ChannelOption.SO_RCVBUF, 65535)     // 接收缓冲区大小
                .localAddress(new InetSocketAddress(config.getPort()))
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                                new HttpServerCodec(),  // HTTP请求编解码器
                                new HttpObjectAggregator(config.getNetty().getMaxContentLength()),
                                new HttpServerExpectContinueHandler(), // 处理100 Continue请求
                                new NettyHttpServerHandler(nettyProcessor) // 自定义业务处理器
                        );
                    }
                });
    }

    @Override
    public void shutdown() {

    }
}
