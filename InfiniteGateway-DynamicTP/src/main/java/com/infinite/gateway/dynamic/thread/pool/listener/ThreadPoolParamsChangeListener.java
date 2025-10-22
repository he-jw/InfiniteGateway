package com.infinite.gateway.dynamic.thread.pool.listener;

import com.infinite.gateway.dynamic.thread.pool.properties.RemoteThreadPoolExecutorProperties;

@FunctionalInterface
public interface ThreadPoolParamsChangeListener {

    /**
     * 动态线程池参数变更时调用此方法
     */
    void onThreadPoolParamsChange(RemoteThreadPoolExecutorProperties threadPoolProperties);
}