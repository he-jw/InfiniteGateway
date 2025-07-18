package com.infinite.gateway.core.filter.flow.limiter;

import com.infinite.gateway.core.context.GatewayContext;

public interface FlowLimiter {

    void limit(GatewayContext context);

}
