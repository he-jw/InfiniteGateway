package com.infinite.gateway.core.filter;


import com.infinite.gateway.core.context.GatewayContext;

public interface Filter {

    void doPreFilter(GatewayContext context);

    // TODO 目前没有用上，目前的做法是在路由过滤器拿到响应直接writeANDfLUASH,没有走post Filter
    void doPostFilter(GatewayContext context);

    String mark(); // 标识唯一的过滤器

    int getOrder();

}
