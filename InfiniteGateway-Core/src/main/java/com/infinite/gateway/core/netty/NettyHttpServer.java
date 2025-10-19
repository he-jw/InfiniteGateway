package com.infinite.gateway.core.netty;

import com.infinite.gateway.common.util.SystemUtil;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.config.netty.NettyConfig;
import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.executor.BizExecutorManager;
import com.infinite.gateway.core.netty.handler.NettyHttpServerHandler;
import com.infinite.gateway.core.netty.processor.NettyProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * Netty HTTP服务器实现，负责接收客户端请求
 * 实现LifeCycle接口管理生命周期
 */
@Slf4j
public class NettyHttpServer implements LifeCycle {

    private Config config;
    private NettyConfig nettyConfig;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    private EventLoopGroup eventLoopGroupWorker;
    private EventExecutorGroup bizEventExecutorGroup;
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
        this.serverBootstrap = new ServerBootstrap();
        if (SystemUtil.useEpoll()) {
            // Linux系统使用Epoll模型
            this.eventLoopGroupBoss = new EpollEventLoopGroup(
                    nettyConfig.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("epoll-netty-server-boss-nio")
            );
            this.eventLoopGroupWorker = new EpollEventLoopGroup(
                    nettyConfig.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("epoll-netty-server-worker-nio")
            );
        } else {
            // 其他系统使用NIO模型
            this.eventLoopGroupBoss = new NioEventLoopGroup(
                    nettyConfig.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("default-netty-server-boss-nio")
            );
            this.eventLoopGroupWorker = new NioEventLoopGroup(
                    nettyConfig.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("default-netty-server-worker-nio")
            );
        }

        // 初始化业务线程池（用于执行过滤器链等业务逻辑，与IO线程分离）
        BizExecutorManager.getInstance().init(
                nettyConfig.getBusinessThreadNum(),
                nettyConfig.getBusinessQueueSize()
        );
        this.bizEventExecutorGroup = BizExecutorManager.getInstance().getBizEventExecutorGroup();

        log.info("NettyHttpServer initialized with boss={}, worker={}, bizThreads={}",
                nettyConfig.getEventLoopGroupBossNum(),
                nettyConfig.getEventLoopGroupWorkerNum(),
                nettyConfig.getBusinessThreadNum());
    }

    /**
     * 启动Netty服务器
     */
    @SneakyThrows(InterruptedException.class)
    @Override
    public void start() {
        serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                // 选择通道类型（Epoll或NIO）
                .channel(SystemUtil.useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                // TCP参数配置
                .option(ChannelOption.SO_BACKLOG, 1024)          // 连接等待队列大小
                .option(ChannelOption.SO_REUSEADDR, true)        // 端口重用
                .option(ChannelOption.SO_KEEPALIVE, true)        // 保持连接
                .childOption(ChannelOption.TCP_NODELAY, true)    // 禁用Nagle算法
                .childOption(ChannelOption.SO_SNDBUF, 65535)     // 发送缓冲区大小
                .childOption(ChannelOption.SO_RCVBUF, 65535)     // 接收缓冲区大小
                .localAddress(new InetSocketAddress(config.getPort())) // 绑定端口
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                // HTTP编解码器（在IO线程执行）
                                new HttpServerCodec(),
                                new HttpServerExpectContinueHandler(),
                                new HttpObjectAggregator(config.getNetty().getMaxContentLength())
                        );
                        // 业务Handler绑定到业务线程池（业务逻辑在业务线程执行）
                        ch.pipeline().addLast(
                                bizEventExecutorGroup,
                                new NettyHttpServerHandler(nettyProcessor)
                        );
                    }
                });
        serverBootstrap.bind().sync();
        log.info("gateway startup on port {}", this.config.getPort());
    }

    /**
     * 停止Netty服务器
     */
    @Override
    public void shutdown() {
        log.info("Shutting down NettyHttpServer...");
        // 优雅关闭业务线程池
        BizExecutorManager.getInstance().shutdown();
        // 优雅关闭IO线程池
        eventLoopGroupBoss.shutdownGracefully();
        eventLoopGroupWorker.shutdownGracefully();
        log.info("NettyHttpServer shutdown completed");
    }
}
