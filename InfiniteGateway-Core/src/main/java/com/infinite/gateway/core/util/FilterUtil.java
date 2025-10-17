package com.infinite.gateway.core.util;

import com.alibaba.fastjson.JSON;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.Objects;

import static com.infinite.gateway.common.constant.FilterConstant.GRAY_FILTER_NAME;
import static com.infinite.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;

public class FilterUtil {

    public static RouteDefinition.FilterConfig findFilterConfigByName(GatewayContext gatewayContext, String filterName) {
        return gatewayContext.getRoute().getFilterConfigs().stream()
                .filter(Objects::nonNull)
                .filter(RouteDefinition.FilterConfig::isEnable)
                .filter(filterConfig -> Objects.nonNull(filterConfig.getName()) && filterConfig.getName().equals(filterName))
                .findFirst().orElse(null);
    }

    public static RouteDefinition.FilterConfig DefaultLoadBalanceFilterConfig() {
        return new RouteDefinition.FilterConfig(
                LOAD_BALANCE_FILTER_NAME,
                true,
                JSON.toJSON(new RouteDefinition.LoadBalanceFilterConfig()).toString()
        );
    }

    public static RouteDefinition.FilterConfig DefaultGrayFilterConfig() {
        return new RouteDefinition.FilterConfig(
                GRAY_FILTER_NAME,
                true,
                JSON.toJSON(new RouteDefinition.GrayFilterConfig()).toString()
        );
    }
}
