package com.infinite.gateway.core.filter.route.resilience.fallback;

import com.infinite.gateway.core.context.GatewayContext;

public interface FallbackHandler {

    void handle(GatewayContext context, Throwable throwable);

    String mark();

}
