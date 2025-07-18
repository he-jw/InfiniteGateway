package com.infinite.gateway.core.filter.flow;

import cn.hutool.json.JSONUtil;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.Filter;
import com.infinite.gateway.core.filter.flow.limiter.FlowLimiter;
import com.infinite.gateway.core.filter.flow.limiter.impl.SlidingWindowLimiter;
import com.infinite.gateway.core.filter.flow.limiter.impl.TokenBucketLimiter;
import com.infinite.gateway.core.filter.flow.limiter.impl.leakyBucketLimiter;
import com.infinite.gateway.core.manager.DynamicConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.infinite.gateway.common.constant.FilterConstant.FLOW_FILTER_NAME;
import static com.infinite.gateway.common.constant.FilterConstant.FLOW_FILTER_ORDER;

public class FlowFilter implements Filter {

    private final Map<String /* 服务名 */, FlowLimiter> flowLimiterMap = new ConcurrentHashMap<>();

    @Override
    public void doPreFilter(GatewayContext context) {
        context.getRoute().getFilterConfigs().stream()
                .filter(filterConfig -> filterConfig.isEnable() && FLOW_FILTER_NAME.equals(filterConfig.getName()))
                .findFirst()
                .ifPresentOrElse(filterConfig -> {
                    RouteDefinition.FlowFilterConfig flowFilterConfig;
                    if (filterConfig.getConfig() != null) {
                        flowFilterConfig = JSONUtil.toBean(filterConfig.getConfig(), RouteDefinition.FlowFilterConfig.class);
                    } else {
                        flowFilterConfig = new RouteDefinition.FlowFilterConfig();
                    }
                    flowLimiterMap.computeIfAbsent(context.getRoute().getServiceName(), serviceName -> {
                        DynamicConfigManager.getInstance().addRouteListener(serviceName, routeDefinition -> flowLimiterMap.remove(serviceName));
                        return buildFlowLimiter(flowFilterConfig, context);
                    }).limit(context);
                }, context::doFilter);
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private FlowLimiter buildFlowLimiter(RouteDefinition.FlowFilterConfig flowFilterConfig, GatewayContext context) {
        switch (flowFilterConfig.getType()) {
            case TOKEN_BUCKET:
                return new TokenBucketLimiter(flowFilterConfig.getCapacity(), flowFilterConfig.getRate());
            case LEAKY_BUCKET:
                return new leakyBucketLimiter(
                        flowFilterConfig.getCapacity(),
                        flowFilterConfig.getRate(),
                        context.getNettyCtx().channel().eventLoop());
            case SLIDING_WINDOW:
                return new SlidingWindowLimiter(flowFilterConfig.getCapacity());
            default:
                return new TokenBucketLimiter(flowFilterConfig.getCapacity(), flowFilterConfig.getRate());
        }
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return FLOW_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return FLOW_FILTER_ORDER;
    }
}
