package com.infinite.gateway.core.filter;

import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.manager.DynamicConfigManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.infinite.gateway.common.constant.FilterConstant.*;


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

    private static final Set<String> addListener = new CopyOnWriteArraySet<>();

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
            FilterChain chain = new FilterChain();
            // 1.添加固定的过滤器
            addFilterIfPresent(chain, CORS_FILTER_NAME);         // 跨域处理
            addFilterIfPresent(chain, FLOW_FILTER_NAME);        // 流量控制
            addFilterIfPresent(chain, GRAY_FILTER_NAME);        // 灰度发布
            addFilterIfPresent(chain, LOAD_BALANCE_FILTER_NAME); // 负载均衡
            // 2.添加自定义的过滤器
            // 根据路由配置添加自定义过滤器
            addFilter(chain, gatewayContext.getRoute().getFilterConfigs());
            // 3.添加路由过滤器
            addFilterIfPresent(chain, ROUTE_FILTER_NAME); // 路由处理
            // 4.对过滤器进行排序
            chain.sort();
            // 5.向 manager 添加路由变更时要清除过滤器链的监听器
            if (!addListener.contains(serviceName)) {
                DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute ->
                        filterChainMap.remove(newRoute.getServiceName()));
                addListener.add(serviceName);
            }
            return chain;
        });
        gatewayContext.setFilterChain(filterChainMap.get(serviceName));
    }

    /**
     * 添加路由配置中指定的自定义过滤器。
     */
    private static void addFilter(FilterChain chain, Set<RouteDefinition.FilterConfig> filterConfigs) {
        if (filterConfigs == null || filterConfigs.isEmpty()) return;
        for (RouteDefinition.FilterConfig filterConfig : filterConfigs) {
            if (!addFilterIfPresent(chain, filterConfig.getName())) {
                log.info("not found filter: {}", filterConfig.getName());
            }
        }
    }

    /**
     * 如果存在指定名称的过滤器，则将其加入过滤器链。
     *
     * @return 是否成功添加过滤器
     */
    private static boolean addFilterIfPresent(FilterChain chain, String filterName) {
        Filter filter = filterMap.get(filterName);
        if (null != filter) {
            chain.add(filter);
            return true;
        }
        return false;
    }
}
