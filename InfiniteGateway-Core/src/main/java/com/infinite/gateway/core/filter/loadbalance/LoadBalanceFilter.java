package com.infinite.gateway.core.filter.loadbalance;

import cn.hutool.json.JSONUtil;
import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.NotFoundException;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.Filter;
import com.infinite.gateway.core.filter.loadbalance.strategy.LoadBalanceStrategy;
import com.infinite.gateway.core.filter.loadbalance.strategy.LoadBalanceStrategyManager;
import com.infinite.gateway.core.manager.DynamicConfigManager;
import com.infinite.gateway.core.util.FilterUtil;

import java.util.List;

import static com.infinite.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_NAME;
import static com.infinite.gateway.common.constant.FilterConstant.LOAD_BALANCE_FILTER_ORDER;

public class LoadBalanceFilter implements Filter {
    @Override
    public void doPreFilter(GatewayContext context) {
        RouteDefinition.FilterConfig filterConfig = FilterUtil.findFilterConfigByName(context, LOAD_BALANCE_FILTER_NAME);
        if (filterConfig == null) {
            filterConfig = FilterUtil.DefaultLoadBalanceFilterConfig();
        }
        RouteDefinition.LoadBalanceFilterConfig loadBalanceFilterConfig = JSONUtil.toBean(filterConfig.getConfig(),
                RouteDefinition.LoadBalanceFilterConfig.class);
        LoadBalanceStrategy strategy = LoadBalanceStrategyManager.getLoadBalanceStrategy(loadBalanceFilterConfig.getStrategyName());
        List<ServiceInstance> instances = DynamicConfigManager.getInstance().getServiceInstances(context.getRoute().getServiceName());
        if (instances == null || instances.isEmpty()) {
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        ServiceInstance instance = strategy.chooseInstance(context, instances, loadBalanceFilterConfig);
        context.getRequest().setModifyHost(instance.getInstanceId());
        context.doFilter();
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return LOAD_BALANCE_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCE_FILTER_ORDER;
    }
}
