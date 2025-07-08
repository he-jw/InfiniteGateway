package com.infinite.gateway.config.manager;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.NotFoundException;
import com.infinite.gateway.config.pojo.RouteDefinition;
import com.infinite.gateway.config.pojo.ServiceDefinition;
import com.infinite.gateway.config.pojo.ServiceInstance;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 动态配置管理，缓存从配置中心拉取下来的配置
 */
@Data
public class DynamicConfigManager {

    private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    private ConcurrentHashMap<String /* path */, RouteDefinition> pathRouteDefinitionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String /* 服务名 */, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String /* 服务名 */, ConcurrentHashMap<String /* 实例id */, ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

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

    /**
     * 根据uri匹配路由，通过uri匹配上了多个路由，则返回 path 长度最大的路由
     * @param uri
     * @return
     */
    // TODO: uri的格式是什么样的
    public RouteDefinition matchingRouteByUri(String uri) {
        List<RouteDefinition> routeDefinitions = new ArrayList<>();
        List<Map.Entry<String, RouteDefinition>> entryList = pathRouteDefinitionMap.entrySet()
                .stream()
                .sorted(((o1, o2) -> o2.getKey().length() - o1.getKey().length()))
                .toList();
        for (Map.Entry<String, RouteDefinition> entry : entryList) {
            String replaced = entry.getKey().replace("**", ".*");
            if (Pattern.matches(replaced, uri)) {
                routeDefinitions.add(entry.getValue());
            }
        }
        if (routeDefinitions.isEmpty()) {
            throw new NotFoundException(ResponseCode.PATH_NO_MATCHED);
        }
        return routeDefinitions.get(0);
    }

    public ServiceDefinition getServiceDefinition(String serviceName) {
        return serviceDefinitionMap.get(serviceName);
    }


}
