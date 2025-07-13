package com.infinite.gateway.core.context;

import com.infinite.gateway.config.pojo.RouteDefinition;
import com.infinite.gateway.core.filter.FilterChain;
import com.infinite.gateway.core.helper.ResponseHelper;
import com.infinite.gateway.core.request.GatewayRequest;
import com.infinite.gateway.core.response.GatewayResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.Data;

/**
 * GatewayContext 是网关请求处理的核心上下文对象。
 * 它贯穿整个请求生命周期，封装了请求、响应、路由信息以及当前执行的过滤器链等关键数据。
 */
@Data
public class GatewayContext {

    /**
     * Netty 的 ChannelHandlerContext，用于与客户端进行通信。
     */
    private ChannelHandlerContext nettyCtx;

    /**
     * 请求过程中发生的异常（如有），供后续异常处理使用。
     */
    private Throwable throwable;

    /**
     * 封装的网关请求对象，包含客户端原始请求的所有信息。
     */
    private GatewayRequest request;

    /**
     * 封装的网关响应对象，用于构建并发送返回给客户端的响应。
     */
    private GatewayResponse response;

    /**
     * 当前请求匹配的路由定义，包含服务名、目标地址、过滤器配置等信息。
     */
    private RouteDefinition route;

    /**
     * 是否保持连接（HTTP Keep-Alive）。
     */
    private boolean keepAlive;

    /**
     * 过滤器链，用于执行过滤操作。
     */
    private FilterChain filterChain;

    /**
     * 当前正在执行的过滤器索引。
     */
    private int curFilterIndex = 0;

    /**
     * 标识当前是否在执行 preFilter 阶段。
     * true 表示正在执行前置过滤器，false 表示后置过滤器阶段。
     */
    private boolean isDoPreFilter = true;

    /**
     * 构造函数，初始化 GatewayContext。
     *
     * @param nettyCtx Netty 的上下文对象
     * @param request 网关请求对象
     * @param route 匹配的路由定义
     * @param keepAlive 是否保持连接
     */
    public GatewayContext(ChannelHandlerContext nettyCtx, GatewayRequest request,
                          RouteDefinition route, boolean keepAlive) {
        this.nettyCtx = nettyCtx;
        this.request = request;
        this.route = route;
        this.keepAlive = keepAlive;
    }

    /**
     * 执行过滤器链。
     * 过滤器分为两个阶段：
     * - 前置过滤器（preFilter）：按顺序执行
     * - 后置过滤器（postFilter）：逆序执行
     *
     * 每次调用 doFilter() 方法会推进当前执行的过滤器索引，
     * 直到所有过滤器执行完毕后，最终调用 ContextHelper.writeBackResponse() 返回响应。
     */
    public void doFilter() {
        int size = filterChain.size();
        if (isDoPreFilter) {
            // 执行前置过滤器
            filterChain.doPreFilter(curFilterIndex++, this);
            if (curFilterIndex == size) {
                // 所有前置过滤器已执行完毕，切换为后置阶段
                isDoPreFilter = false;
                curFilterIndex--;  // 回退一个索引，准备执行 postFilter
            }
        } else {
            // 执行后置过滤器
            filterChain.doPostFilter(curFilterIndex--, this);
            if (curFilterIndex < 0) {
                // 所有过滤器都已执行完毕，写回响应
                this.writeBackResponse();
            }
        }
    }

    /**
     * 将处理完成的响应写回客户端
     */
    public void writeBackResponse() {
        // 1. 从上下文构建HTTP响应对象
        FullHttpResponse httpResponse = ResponseHelper.buildHttpResponse(this.getResponse());
        // 2. 根据连接类型处理响应
        if (!this.isKeepAlive()) {
            // 短连接：发送响应后关闭连接
            this.getNettyCtx().writeAndFlush(httpResponse)
                    .addListener(ChannelFutureListener.CLOSE);
        } else {
            // 长连接：设置Keep-Alive头部并发送响应
            httpResponse.headers().set(
                    HttpHeaderNames.CONNECTION,  // "Connection"
                    HttpHeaderValues.KEEP_ALIVE  // "keep-alive"
            );
            this.getNettyCtx().writeAndFlush(httpResponse);
        }

    }
}
