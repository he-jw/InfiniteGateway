package com.infinite.gateway.core.netty.processor;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.GatewayException;
import com.infinite.gateway.config.manager.DynamicConfigManager;
import com.infinite.gateway.config.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.helper.RequestHelper;
import com.infinite.gateway.core.helper.ResponseHelper;
import com.infinite.gateway.core.request.GatewayRequest;
import com.infinite.gateway.core.filter.FilterChainFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

/**
 * NettyCoreProcessor 是负责在基于 Netty 的服务器中处理 HTTP 请求的组件。
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor {

    private static final DynamicConfigManager manager = DynamicConfigManager.getInstance();

    /**
     * 处理 HTTP 请求的方法。
     *
     * @param ctx ChannelHandlerContext 对象，Netty 通道上下文
     * @param request FullHttpRequest 对象，表示 HTTP 请求
     */
    @Override
    public void process(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // 1. 构建网关上下文（包含请求信息、服务路由等）
            // 1.1. 通过请求URI匹配路由规则
            RouteDefinition route = manager.matchingRouteByUri(request.uri());
            // 1.2. 构建网关请求对象
            GatewayRequest gatewayRequest = RequestHelper.buildGatewayRequest(
                    manager.getServiceDefinition(route.getServiceName()),
                    request,
                    ctx
            );
            // 1.3. 创建网关上下文（包含连接是否保持长连接）
            GatewayContext gatewayContext = new GatewayContext(
                    ctx,
                    gatewayRequest,
                    route,
                    HttpUtil.isKeepAlive(request)
            );

            // 2. 构建过滤器链（根据配置动态组装过滤器）
            FilterChainFactory.buildFilterChain(gatewayContext);

            // 3. 执行过滤器链（处理请求的核心流程）
            gatewayContext.doFilter();

        } catch (GatewayException e) {
            // 4. 处理已知网关异常（如路由未找到、流控限制等）
            log.error("处理错误 {} {}", e.getCode(), e.getCode().getMessage());
            // 构建对应的HTTP错误响应
            FullHttpResponse httpResponse = ResponseHelper.buildHttpResponse(e.getCode());
            // 发送响应并释放资源
            doWriteAndRelease(ctx, request, httpResponse);
        } catch (Throwable t) {
            // 5. 处理未知异常（兜底处理）
            log.error("处理未知错误", t);
            // 构建500内部错误响应
            FullHttpResponse httpResponse = ResponseHelper.buildHttpResponse(ResponseCode.INTERNAL_ERROR);
            // 发送响应并释放资源
            doWriteAndRelease(ctx, request, httpResponse);
        }
    }

    // TODO
    private void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse httpResponse) {

    }

    /**
     * 将处理完成的响应写回客户端
     * @param context 网关上下文对象
     */
    public static void writeBackResponse(GatewayContext context) {

    }
}