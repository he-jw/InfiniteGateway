package com.infinite.gateway.core.manager;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.NotFoundException;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.listener.RouteListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 动态配置管理，缓存从配置中心拉取下来的配置
 */
@Slf4j
@Data
public class DynamicConfigManager {

    private static final DynamicConfigManager INSTANCE = new DynamicConfigManager();
    /**
     * 定时任务执行器, 用于定时打印所有 map 的内容，方便开发时调试
     */
    private ScheduledThreadPoolExecutor scheduler;

    private ConcurrentHashMap<String /* path */, RouteDefinition> pathRouteDefinitionMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String /* 服务名 */, RouteDefinition> serviceRouteDefinitionMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String /* 服务名 */, ServiceDefinition> serviceDefinitionMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String /* 服务名 */, ConcurrentHashMap<String /* 实例id */, ServiceInstance>> serviceInstanceMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String /* 服务名 */, List<RouteListener>> routeListeners = new ConcurrentHashMap<>();

    static {
        INSTANCE.initScheduler();
    }

    public static DynamicConfigManager getInstance() {
        return INSTANCE;
    }

    public void onRouteListeners(List<RouteDefinition> newRoutes) {
        for (RouteDefinition newRoute : newRoutes) {
            routeListeners.computeIfPresent(newRoute.getServiceName(), (key, value) -> {
                for (RouteListener routeListener : value) {
                    routeListener.changeOnRoute(newRoute);
                }
                return value;
            });
        }
    }

    public void addRouteListener(String serviceName , RouteListener routeListener) {
        routeListeners.computeIfAbsent(serviceName, key -> new CopyOnWriteArrayList<>()).add(routeListener);
    }

    /**
     * 更新路由
     * @param newRoutes
     */
    public synchronized void updateRoutes(List<RouteDefinition> newRoutes) {
        // 1.更新 newPathRouteDefinitionMap
        ConcurrentHashMap<String, RouteDefinition> newPathRouteDefinitionMap = new ConcurrentHashMap<>();
        for (RouteDefinition newRoute : newRoutes) {
            for (String path : newRoute.getPaths()) {
                newPathRouteDefinitionMap.put(path, newRoute);
            }
        }
        pathRouteDefinitionMap = newPathRouteDefinitionMap;

        // 2.更新 newServiceRouteDefinitionMap
        ConcurrentHashMap<String, RouteDefinition> newServiceRouteDefinitionMap = new ConcurrentHashMap<>();
        for (RouteDefinition newRoute : newRoutes) {
            newServiceRouteDefinitionMap.put(newRoute.getServiceName(), newRoute);
        }
        serviceRouteDefinitionMap = newServiceRouteDefinitionMap;
    }

    /**
     * 根据uri(格式：/user/1)匹配路由，通过uri匹配上了多个路由，则返回 path 长度最大的路由
     * @param uri
     * @return
     */
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

    public void updateServiceDefinition(ServiceDefinition serviceDefinition) {
        serviceDefinitionMap.put(serviceDefinition.getServiceName(), serviceDefinition);
    }

    public void updateServiceInstance(ServiceDefinition serviceDefinition, Set<ServiceInstance> set) {
        serviceInstanceMap.put(
                serviceDefinition.getServiceName(),
                new ConcurrentHashMap<>(set.stream()
                        .collect(Collectors.toMap(ServiceInstance::getInstanceId, instance -> instance))));
    }

    /**
     * 初始化定时任务
     */
    private void initScheduler() {
        scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true); // 设置为守护线程，避免阻塞程序退出
            return t;
        });

        // 每30秒执行一次打印操作
        scheduler.scheduleAtFixedRate(this::printAllMaps, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * 定时任务：打印所有 map 的内容
     */
    private void printAllMaps() {
        log.info("Printing all maps...（测试配置中心和验证中的效果）");
        System.out.println("Printing pathRouteDefinitionMap:");
        pathRouteDefinitionMap.forEach((key, value) -> System.out.println(key + " => " + value));

        System.out.println("Printing serviceDefinitionMap:");
        serviceDefinitionMap.forEach((key, value) -> System.out.println(key + " => " + value));

        System.out.println("Printing serviceInstanceMap:");
        serviceInstanceMap.forEach((serviceKey, instanceMap) -> {
            System.out.println("Service: " + serviceKey);
            instanceMap.forEach((instanceKey, instanceValue) ->
                    System.out.println("  " + instanceKey + " => " + instanceValue));
        });
    }




}
