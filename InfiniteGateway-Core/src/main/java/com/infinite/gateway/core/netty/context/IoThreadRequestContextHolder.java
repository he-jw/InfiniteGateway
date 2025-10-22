package com.infinite.gateway.core.netty.context;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

/**
 * IO线程侧的请求上下文，用于在IO线程自己传递请求上下文
 */
public final class IoThreadRequestContextHolder {
    private IoThreadRequestContextHolder() {}

    @Data
    public static final class RequestContext {
        private final ChannelHandlerContext ctx;
        private final FullHttpRequest request;

        public RequestContext(ChannelHandlerContext ctx, FullHttpRequest request) {
            this.ctx = ctx;
            this.request = request;
        }
    }

    private static final ThreadLocal<RequestContext> TL = new ThreadLocal<>();

    public static void set(ChannelHandlerContext ctx, FullHttpRequest request) {
        TL.set(new RequestContext(ctx, request));
    }

    public static RequestContext get() {
        return TL.get();
    }

    /**
     * 清理 ThreadLocal，防止内存泄漏
     * 应在 IoThreadContextHandler 的 channelRead finally 块中调用
     */
    public static void clear() {
        TL.remove();
    }
}

