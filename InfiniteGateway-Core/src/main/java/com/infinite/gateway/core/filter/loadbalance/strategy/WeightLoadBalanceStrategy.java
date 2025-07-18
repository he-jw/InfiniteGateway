package com.infinite.gateway.core.filter.loadbalance.strategy;

import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.Random;

import static com.infinite.gateway.common.constant.LoadBalanceConstant.WEIGHT_LOAD_BALANCE_STRATEGY;

public class WeightLoadBalanceStrategy implements LoadBalanceStrategy {
    @Override
    public ServiceInstance chooseInstance(GatewayContext gatewayContext,
                                          List<ServiceInstance> instances,
                                          RouteDefinition.LoadBalanceFilterConfig routeDefinition) {
        int sum = instances.stream().mapToInt(ServiceInstance::getWeight).sum();
        int random = new Random().nextInt(sum);
        for (ServiceInstance instance : instances) {
            random -= instance.getWeight();
            if (random < 0) {
                return instance;
            }
        }
        return null;
    }

    @Override
    public String mark() {
        return WEIGHT_LOAD_BALANCE_STRATEGY;
    }
}
