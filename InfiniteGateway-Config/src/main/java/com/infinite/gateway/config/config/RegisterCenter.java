package com.infinite.gateway.config.config;

import com.infinite.gateway.common.enums.RegisterCenterEnum;
import com.infinite.gateway.config.config.nacos.NacosConfig;
import lombok.Data;

import static com.infinite.gateway.common.constant.ConfigCenterConstant.CONFIG_CENTER_DEFAULT_ENABLED;
import static com.infinite.gateway.common.constant.RegisterCenterConstant.REGISTER_CENTER_DEFAULT_ADDRESS;
import static com.infinite.gateway.common.constant.RegisterCenterConstant.REGISTER_CENTER_DEFAULT_IMPL;


/**
 * 注册中心
 */
@Data
public class RegisterCenter {

    /**
     * 注册中心地址
     */
    private String address = REGISTER_CENTER_DEFAULT_ADDRESS;

    /**
     * 是否开启注册中心
     */
    private boolean enabled = CONFIG_CENTER_DEFAULT_ENABLED;

    /**
     * 注册中心类型
     */
    private String type = RegisterCenterEnum.NACOS.getName();

    /**
     *  nacos 注册中心
     */
    private NacosConfig nacosConfig = new NacosConfig();

}
