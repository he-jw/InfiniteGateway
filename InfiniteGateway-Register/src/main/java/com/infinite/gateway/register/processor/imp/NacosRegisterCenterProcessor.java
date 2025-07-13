package com.infinite.gateway.register.processor.imp;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.register.processor.RegisterCenterProcessor;

import java.util.Properties;

public class NacosRegisterCenterProcessor implements RegisterCenterProcessor {

    private Config config;
    private NamingService namingService;

    @Override
    public void init(Config config) throws NacosException {
        this.config = config;
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, config.getRegisterCenter().getAddress());
        namingService = NacosFactory.createNamingService(properties);


    }


}
