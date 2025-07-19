package com.infinite.gateway.core.filter.route.resilience.fallback;

import com.infinite.gateway.common.constant.FallbackConstant;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.helper.ResponseHelper;

public class DefaultFallbackHandler implements FallbackHandler {
    @Override
    public void handle(GatewayContext context, Throwable throwable) {
        String serviceName = context.getRoute().getServiceName();
        if (context.getRoute().getResilienceConfig().isFallbackEnabled()) {
            context.setResponse(ResponseHelper.buildGatewayResponse(String.format("服务 %s 触发熔断降级", serviceName)));
            context.writeBackResponse();
        }
    }

    @Override
    public String mark() {
        return FallbackConstant.DEFAULT_FALLBACK_HANDLER_NAME;
    }
}
