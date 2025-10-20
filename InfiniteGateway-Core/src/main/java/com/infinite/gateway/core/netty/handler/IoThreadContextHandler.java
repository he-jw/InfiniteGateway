package com.infinite.gateway.core.netty.handler;

import com.infinite.gateway.core.netty.context.IoThreadRequestContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * IO-thread-side handler that exposes the current request context via ThreadLocal
 * between HttpObjectAggregator and the business handler.
 */
public class IoThreadContextHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        IoThreadRequestContextHolder.set(ctx, (FullHttpRequest) msg);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}

