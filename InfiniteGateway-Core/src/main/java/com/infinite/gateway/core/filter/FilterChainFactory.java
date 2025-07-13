package com.infinite.gateway.core.filter;

import com.infinite.gateway.core.context.GatewayContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;


/**
 * FilterChainFactory 负责构建网关过滤器链。
 * 它根据路由配置动态组装请求处理所需的过滤器（如鉴权、限流等）。
 */
@Slf4j
public class FilterChainFactory {

    /**
     * 存储所有可用的 Filter 实例，key 是 filter.mark()
     */
    private static final Map<String, Filter> filterMap = new HashMap<>();

    /**
     * 缓存每个服务对应的过滤器链，避免重复创建
     */
    private static final Map<String, FilterChain> filterChainMap = new ConcurrentHashMap<>();

    static {
        ServiceLoader<Filter> filterServiceLoader = ServiceLoader.load(Filter.class);
        for (Filter filter : filterServiceLoader) {
            filterMap.put(filter.mark(), filter);
            log.info("Loaded filter: {}", filter.mark());
        }
    }

    /**
     * 构建过滤器链
     * @param gatewayContext 网关上下文
     */
    public static void buildFilterChain(GatewayContext gatewayContext) {
        String serviceName = gatewayContext.getRoute().getServiceName();
        filterChainMap.computeIfAbsent(serviceName, k -> {
            FilterChain filterChain = new FilterChain();
            // 1.添加固定的过滤器

            // 2.添加自定义的过滤器

            // 3.添加路由过滤器

            // 4.对过滤器进行排序

            // 5.注册路由变更时要清除过滤器链

            return filterChain;
        });

        gatewayContext.setFilterChain(filterChainMap.get(serviceName));
    }
}
