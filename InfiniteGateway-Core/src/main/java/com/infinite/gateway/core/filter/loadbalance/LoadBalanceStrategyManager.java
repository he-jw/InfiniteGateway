package com.infinite.gateway.core.filter.loadbalance;

import com.infinite.gateway.core.filter.loadbalance.startegy.LoadBalanceStrategy;
import com.infinite.gateway.core.filter.loadbalance.startegy.RoundRobinLoadBalanceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class LoadBalanceStrategyManager {

    private static final Map<String, LoadBalanceStrategy> strategyMap = new HashMap<>();

    static {
        ServiceLoader<LoadBalanceStrategy> serviceLoader = ServiceLoader.load(LoadBalanceStrategy.class);
        for (LoadBalanceStrategy strategy : serviceLoader) {
            strategyMap.put(strategy.mark(), strategy);
        }
    }

    public static LoadBalanceStrategy getLoadBalanceStrategy(String mark) {
        return strategyMap.getOrDefault(mark, new RoundRobinLoadBalanceStrategy());
    }
}
