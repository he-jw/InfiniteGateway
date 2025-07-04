package com.infinite.gateway.config.manager;

import com.infinite.gateway.config.pojo.RouteDefinition;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置管理，缓存从配置中心拉取下来的配置
 */
public class DynamicConfigManager {

    private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();

    private ConcurrentHashMap<String /* path */, RouteDefinition> pathRouteDefinitionMap = new ConcurrentHashMap<>();


    public static DynamicConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * 更新路由
     * @param newRoutes
     */
    public synchronized void updateRoutes(List<RouteDefinition> newRoutes) {
        ConcurrentHashMap<String, RouteDefinition> newPathRouteDefinitionMap = new ConcurrentHashMap<>();
        for (RouteDefinition newRoute : newRoutes) {
            for (String path : newRoute.getPaths()) {
                newPathRouteDefinitionMap.put(path, newRoute);
            }
        }
        pathRouteDefinitionMap = newPathRouteDefinitionMap;
    }
}
