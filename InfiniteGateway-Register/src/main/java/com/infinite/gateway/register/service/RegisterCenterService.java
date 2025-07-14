package com.infinite.gateway.register.service;

import com.infinite.gateway.common.pojo.ServiceDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.register.listener.RegisterCenterListener;

public interface RegisterCenterService {

    /**
     * 初始化
     */
    void init(Config config);

    /**
     * 注册
     */
    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 注册
     */
    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 订阅所有服务变更
     * @param registerCenterListener
     */
    void subscribeAllServices(RegisterCenterListener registerCenterListener);

}
