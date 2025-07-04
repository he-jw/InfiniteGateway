package com.infinite.gateway.config.service;

import com.infinite.gateway.config.pojo.RouteDefinition;

import java.util.List;

/**
 * 规则变更监听器
 */
@FunctionalInterface
public interface RoutesChangeListener {

    /**
     * 路由变更时调用此方法
     */
    void onRoutesChange(List<RouteDefinition> newRoutes);

}