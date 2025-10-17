package com.infinite.gateway.core.filter.loadbalance.strategy;

import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.List;

public interface LoadBalanceStrategy {

    ServiceInstance chooseInstance(GatewayContext gatewayContext,
                                   List<ServiceInstance> instances,
                                   RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig);

    String mark();

}
