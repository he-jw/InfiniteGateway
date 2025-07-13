package com.infinite.gateway.register.processor;

import com.alibaba.nacos.api.exception.NacosException;
import com.infinite.gateway.config.config.Config;

public interface RegisterCenterProcessor {

    void init(Config config) throws NacosException;


}
