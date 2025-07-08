package com.infinite.gateway.core.context;

import com.infinite.gateway.config.pojo.RouteDefinition;
import com.infinite.gateway.core.request.GatewayRequest;
import com.infinite.gateway.core.response.GatewayResponse;
import io.netty.channel.ChannelHandlerContext;
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

    public void doFilter() {

    }
}
