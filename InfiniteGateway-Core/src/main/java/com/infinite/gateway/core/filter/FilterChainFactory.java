package com.infinite.gateway.core.filter;

import com.infinite.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;


/**
 * FilterChainFactory 负责构建网关过滤器链。
 * 它根据路由配置动态组装请求处理所需的过滤器（如鉴权、限流等）。
 */
@Slf4j
public class FilterChainFactory {

    public static void buildFilterChain(GatewayContext gatewayContext) {

    }
}
