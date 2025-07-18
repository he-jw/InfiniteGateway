package com.infinite.gateway.core.filter.loadbalance.strategy;

import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.manager.DynamicConfigManager;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.infinite.gateway.common.constant.LoadBalanceConstant.ROUND_ROBIN_LOAD_BALANCE_STRATEGY;

public class ConsistentHashingLoadBalanceStrategy implements LoadBalanceStrategy {

    private final ConcurrentHashMap<String/* 服务名 */, ConsistentHashing> ringMap = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance chooseInstance(GatewayContext gatewayContext,
                                          List<ServiceInstance> instances,
                                          RouteDefinition.LoadBalanceFilterConfig config) {
        String serviceName = gatewayContext.getRoute().getServiceName();
        return ringMap.computeIfAbsent(serviceName, key -> {
            DynamicConfigManager.getInstance().addRouteListener(serviceName, route -> ringMap.remove(serviceName));
            return new ConsistentHashing(config.getVirtualNodeNum(), instances);
        }).getNode(gatewayContext.getRequest().getClientIp());
    }

    @Override
    public String mark() {
        return ROUND_ROBIN_LOAD_BALANCE_STRATEGY;
    }

    static class ConsistentHashing {

        private final int virtualNodeNum;
        private final TreeMap<Integer /* 哈希值 */, ServiceInstance /* 实例 */> hashRing = new TreeMap<>();

        public ConsistentHashing(int virtualNodeNum, List<ServiceInstance> instances) {
            this.virtualNodeNum = virtualNodeNum;
            for (ServiceInstance instance : instances) {
                addNode(instance);
            }
        }

        public void addNode(ServiceInstance instance) {
            for (int i = 0; i < virtualNodeNum; i++) {
                String virtualNodeName = instance.getInstanceId() + "&&VN" + i;
                int hash = getHash(virtualNodeName);
                hashRing.put(hash, instance);
            }
        }

        public ServiceInstance getNode(String key) {
            if (hashRing.isEmpty()) {
                return null;
            }
            int hash = getHash(key);
            SortedMap<Integer, ServiceInstance> tailMap = hashRing.tailMap(hash);
            Integer target = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
            return hashRing.get(target);
        }

        private int getHash(String str) {
            // FNV-1a 核心算法
            int hash = 0x811c9dc5;  // FNV偏移基础值
            final int prime = 0x01000193; // 16777619 的十六进制表示

            for (int i = 0; i < str.length(); i++) {
                hash ^= str.charAt(i);
                hash *= prime;
            }

            // 增强雪崩效应的位操作 (优化版)
            hash ^= hash >>> 16;
            hash *= 0x85ebca6b;
            hash ^= hash >>> 13;
            hash *= 0xc2b2ae35;
            hash ^= hash >>> 16;

            return hash & 0x7fffffff; // 确保非负
        }
    }
}
