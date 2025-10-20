package com.infinite.gateway.core.netty;

import com.infinite.gateway.common.util.SystemUtil;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.config.netty.NettyConfig;
import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.executor.BizExecutorManager;
import com.infinite.gateway.core.netty.handler.IoThreadContextHandler;
import com.infinite.gateway.core.netty.handler.NettyHttpServerHandler;
import com.infinite.gateway.core.netty.http2.Http2UpgradeCodecFactory;
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
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
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
                        /*
                         * initChannel（同端口兼容 HTTP/1.1 与 HTTP/2 h2c，仅支持 Upgrade，不支持 Prior-Knowledge）
                         *
                         * 时序与职责：
                         * 1) 统一安装 HTTP/1.1 编解码器 HttpServerCodec，用于解析首个请求并识别是否为 Upgrade: h2c
                         * 2) 安装 HttpServerUpgradeHandler，并提供 UpgradeCodecFactory：
                         *    - 仅当协议名为 "h2c"（Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME）时进行协议切换
                         *    - Upgrade 请求需满足三要素：Connection: Upgrade、Upgrade: h2c、HTTP2-Settings（BASE64URL）
                         *    - 升级成功：
                         *      a. 安装 Http2FrameCodec（父通道，负责 HTTP/2 帧编解码）
                         *      b. 安装 Http2MultiplexHandler，按 stream 创建子 Channel，并交由 H2ChildChannelInitializer 初始化
                         *         子通道内将帧还原为 FullHttpRequest，复用 IoThreadContextHandler + NettyHttpServerHandler
                         *    - 升级失败或未发起升级：回退到 HTTP/1.1 业务链（initHttp1Pipeline）
                         *
                         * 线程与资源：
                         * - 父通道/子通道均绑定 IO EventLoop，业务处理在 bizEventExecutorGroup 上执行
                         * - H2 子流在子通道中做 HttpObject 聚合，保持与 HTTP/1.1 完全一致的 FullHttpRequest 输入
                         * - 你的 ThreadLocal 拒绝兜底逻辑（IoThreadContextHandler）在两条协议中均成立
                         */
                        // Step 1: 安装 HTTP/1.1 编解码器，用于首包解析与 Upgrade 识别
                        //         注意：HttpServerCodec 是有状态的，每个 Channel 必须有自己的实例
                        HttpServerCodec http1 = new HttpServerCodec();

                        // Step 2: 获取 HTTP/2 升级编解码工厂（单例）
                        //         UpgradeCodecFactory 是无状态的，可以被多个 Channel 共享
                        //         每次调用 newUpgradeCodec() 都会创建新的 Http2FrameCodec 和 Http2MultiplexHandler
                        Http2UpgradeCodecFactory upgradeFactory =
                                Http2UpgradeCodecFactory.getInstance(
                                        config.getNetty().getMaxContentLength(),
                                        bizEventExecutorGroup,
                                        nettyProcessor);

                        // Step 3: 安装 HTTP/1.1 编解码器与升级处理器
                        //         HttpServerUpgradeHandler 会在首个请求到达时检查是否需要升级
                        //         - 若检测到 Upgrade: h2c，调用 upgradeFactory.newUpgradeCodec() 获取升级编解码器
                        //         - 升级成功：替换 pipeline 为 Http2FrameCodec + Http2MultiplexHandler
                        //         - 升级失败/未升级：保持 HTTP/1.1 pipeline，继续处理后续请求
                        ch.pipeline().addLast(http1);
                        ch.pipeline().addLast(new HttpServerUpgradeHandler(http1, upgradeFactory));

                        // Step 4: 安装 HTTP/1.1 业务链（作为非升级流量的回退路径）
                        //         包含：HttpServerExpectContinueHandler、HttpObjectAggregator、
                        //         IoThreadContextHandler（ThreadLocal 拒绝兜底）、NettyHttpServerHandler（业务处理）
                        //         若升级成功，HTTP/2 流量由 Http2MultiplexHandler 派发到子 Channel，
                        //         子 Channel 有自己的 pipeline（由 H2ChildChannelInitializer 初始化），
                        //         不会经过这里的 HTTP/1.1 业务链。
                        //
                        // 总结：
                        // - HTTP/1.1 请求：HttpServerCodec → HttpServerUpgradeHandler → initHttp1Pipeline
                        // - HTTP/1.1 Upgrade → h2c：HttpServerCodec → HttpServerUpgradeHandler → upgradeFactory
                        //   → Http2FrameCodec + Http2MultiplexHandler → 子 Channel（H2ChildChannelInitializer）
                        ch.pipeline().addLast(new HttpServerExpectContinueHandler());
                        ch.pipeline().addLast(new HttpObjectAggregator(config.getNetty().getMaxContentLength()));
                        ch.pipeline().addLast(new IoThreadContextHandler());
                        ch.pipeline().addLast(bizEventExecutorGroup, new NettyHttpServerHandler(nettyProcessor));
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
