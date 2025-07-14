package com.infinite.gateway.register.service.imp;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.register.service.RegisterCenterProcessor;
import lombok.SneakyThrows;

public class NacosRegisterCenterProcessor implements RegisterCenterProcessor {

    private Config config;

    /**
     * 用于维护nacos服务实例
     */
    private NamingService namingService;
    /**
     * 用于维护nacos服务定义
     */
    private NamingMaintainService namingMaintainService;

    @SneakyThrows(NacosException.class)
    @Override
    public void init(Config config){
        this.config = config;
        namingService = NamingFactory.createNamingService(config.getRegisterCenter().getAddress());
        namingMaintainService = NamingMaintainFactory.createMaintainService(config.getRegisterCenter().getAddress());
    }






}
