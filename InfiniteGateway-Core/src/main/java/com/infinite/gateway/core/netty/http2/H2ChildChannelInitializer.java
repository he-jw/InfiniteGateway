package com.infinite.gateway.core.netty.http2;

import com.infinite.gateway.core.netty.handler.IoThreadContextHandler;
import com.infinite.gateway.core.netty.handler.NettyHttpServerHandler;
import com.infinite.gateway.core.netty.processor.NettyProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * HTTP/2 子流 Channel 初始化器
 * 将 H2 帧还原为 HttpObject / FullHttpRequest，并复用现有业务链
 */
public class H2ChildChannelInitializer extends ChannelInitializer<Channel> {

    private final int maxContentLength;
    private final ThreadPoolExecutor bizThreadPoolExecutor;
    private final NettyProcessor nettyProcessor;

    public H2ChildChannelInitializer(int maxContentLength,
                                     ThreadPoolExecutor bizThreadPoolExecutor,
                                     NettyProcessor nettyProcessor) {
        this.maxContentLength = maxContentLength;
        this.bizThreadPoolExecutor = bizThreadPoolExecutor;
        this.nettyProcessor = nettyProcessor;
    }

    @Override
    protected void initChannel(Channel ch) {
        // 将 HTTP/2 帧转换为 HttpObject（HEADERS → HttpRequest，DATA → HttpContent）
        // true 表示服务端模式
        ch.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(true));

        // 聚合 HttpRequest + HttpContent 为 FullHttpRequest
        ch.pipeline().addLast(new HttpObjectAggregator(maxContentLength));

        // 在 IO 线程中将 ctx + FullHttpRequest 存入 ThreadLocal，用于拒绝兜底
        ch.pipeline().addLast(new IoThreadContextHandler());

        // 业务处理器（内部手动提交任务到业务线程池）
        ch.pipeline().addLast(new NettyHttpServerHandler(nettyProcessor, bizThreadPoolExecutor));
    }
}

