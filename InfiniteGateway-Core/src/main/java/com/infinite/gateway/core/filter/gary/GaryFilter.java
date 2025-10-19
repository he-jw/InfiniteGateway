package com.infinite.gateway.core.filter.gary;

import cn.hutool.core.bean.BeanUtil;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.Filter;
import com.infinite.gateway.core.filter.gary.strategy.GaryStrategyManager;
import com.infinite.gateway.core.filter.gary.strategy.GrayStrategy;
import com.infinite.gateway.core.manager.DynamicConfigManager;
import com.infinite.gateway.core.util.FilterUtil;

import java.util.List;

import static com.infinite.gateway.common.constant.FilterConstant.GRAY_FILTER_NAME;
import static com.infinite.gateway.common.constant.FilterConstant.GRAY_FILTER_ORDER;

public class GaryFilter implements Filter {

    @Override
    public void doPreFilter(GatewayContext context) {
        RouteDefinition.FilterConfig grayFilterConfig = FilterUtil.findFilterConfigByName(context, GRAY_FILTER_NAME);
        if (grayFilterConfig == null) {
            grayFilterConfig = FilterUtil.DefaultGrayFilterConfig();
        }
        List<ServiceInstance> serviceInstances = DynamicConfigManager.getInstance().getServiceInstances(context.getRoute().getServiceName())
                .stream().filter(ServiceInstance::isGray).toList();
        if (grayFilterConfig.isEnable() && !serviceInstances.isEmpty()) {
            // 灰度配置被开启了，并且存在灰度实例，接下来要按什么策略进行灰度呢？IP还是按阈值，使用策略模式 + spi机制
            GrayStrategy grayStrategy = GaryStrategyManager.getGrayStrategy(
                    BeanUtil.toBean(grayFilterConfig.getConfig(), RouteDefinition.GrayFilterConfig.class).getStrategyName());
            grayStrategy.execute(context, serviceInstances);
        }
        context.doFilter();
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return GRAY_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return GRAY_FILTER_ORDER;
    }
}
