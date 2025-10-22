package com.infinite.gateway.core.netty.handler;

import com.infinite.gateway.core.netty.processor.NettyProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {

    private final NettyProcessor nettyProcessor;
    private final ThreadPoolExecutor bizThreadPoolExecutor;

    public NettyHttpServerHandler(NettyProcessor nettyProcessor, ThreadPoolExecutor bizThreadPoolExecutor) {
        this.nettyProcessor = nettyProcessor;
        this.bizThreadPoolExecutor = bizThreadPoolExecutor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest request = (FullHttpRequest) msg;

        // 手动提交任务到业务线程池
        // 注意：IoThreadContextHandler 已经在 IO 线程中将 ctx 和 request 存入 ThreadLocal
        // 拒绝策略会从 ThreadLocal 中获取这些信息
        bizThreadPoolExecutor.execute(() -> {
            nettyProcessor.process(ctx, request);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 调用父类的 exceptionCaught 方法，它将按照 ChannelPipeline 中的下一个处理器继续处理异常
        super.exceptionCaught(ctx, cause);
    }
}
