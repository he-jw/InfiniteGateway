package com.infinite.gateway.core.filter.loadbalance.strategy;

import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.Random;

import static com.infinite.gateway.common.constant.LoadBalanceConstant.RANDOM_LOAD_BALANCE_STRATEGY;

public class RandomLoadBalanceStrategy implements LoadBalanceStrategy {

    private static final Random random = new Random();

    @Override
    public ServiceInstance chooseInstance(GatewayContext gatewayContext,
                                          List<ServiceInstance> instances,
                                          RouteDefinition.LoadBalanceFilterConfig routeDefinition) {
        return instances.get(random.nextInt(instances.size()));
    }

    @Override
    public String mark() {
        return RANDOM_LOAD_BALANCE_STRATEGY;
    }
}
