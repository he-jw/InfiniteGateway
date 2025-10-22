package com.infinite.gateway.config.service;

import com.infinite.gateway.config.config.ConfigCenter;
import com.infinite.gateway.dynamic.thread.pool.listener.ThreadPoolParamsChangeListener;

/**
 * 配置中心处理器
 */
public interface ConfigCenterService {

    /**
     * 初始化配置中心配置
     */
    void init(ConfigCenter configCenter);

    /**
     * 订阅路由配置变更
     */
    void subscribeRoutesChange(RoutesChangeListener listener);

    /**
     * 订阅动态线程池参数变更
     */
    void subscribeThreadPoolParamsChange(ThreadPoolParamsChangeListener listener);
}
