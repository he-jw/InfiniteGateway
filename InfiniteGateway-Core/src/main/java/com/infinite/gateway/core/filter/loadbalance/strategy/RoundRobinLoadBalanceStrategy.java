package com.infinite.gateway.core.filter.loadbalance.strategy;

import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.infinite.gateway.common.constant.LoadBalanceConstant.ROUND_ROBIN_LOAD_BALANCE_STRATEGY;

public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {

    Map<String, AtomicInteger> strictPositionMap = new ConcurrentHashMap<>();

    Map<String, Integer> positionMap = new ConcurrentHashMap<>();

    private final int THRESHOLD = Integer.MAX_VALUE >> 2; // 预防移除的安全阈值

    @Override
    public ServiceInstance chooseInstance(GatewayContext context,
                                          List<ServiceInstance> instances,
                                          RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig) {
        String serviceName = context.getRoute().getServiceName();
        ServiceInstance serviceInstance;
        if (loadBalanceFilterConfig.isStrictRoundRobin()) {
            AtomicInteger strictPosition = strictPositionMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
            int index = Math.abs(strictPosition.getAndIncrement());
            serviceInstance = instances.get(index % instances.size());
            if (index >= THRESHOLD) {
                strictPosition.set((index + 1) % instances.size());
            }
        } else {
            int position = positionMap.getOrDefault(serviceName, 0);
            int index = Math.abs(position++);
            serviceInstance = instances.get(index % instances.size());
            if (position >= THRESHOLD) {
                positionMap.put(serviceName, (position + 1) % instances.size());
            }
        }
        return serviceInstance;
    }


    @Override
    public String mark() {
        return ROUND_ROBIN_LOAD_BALANCE_STRATEGY;
    }
}
