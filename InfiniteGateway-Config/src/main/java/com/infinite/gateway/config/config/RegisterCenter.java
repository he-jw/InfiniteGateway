package com.infinite.gateway.config.config;

import com.infinite.gateway.common.enums.RegisterCenterEnum;
import lombok.Data;

import static com.infinite.gateway.common.constant.RegisterCenterConstant.REGISTER_CENTER_DEFAULT_ADDRESS;
import static com.infinite.gateway.common.constant.RegisterCenterConstant.REGISTER_CENTER_DEFAULT_IMPL;


/**
 * 注册中心
 */
@Data
public class RegisterCenter {

    private RegisterCenterEnum type = REGISTER_CENTER_DEFAULT_IMPL; // 注册中心实现

    private String address = REGISTER_CENTER_DEFAULT_ADDRESS; // 注册中心地址

    private NacosConfig nacosConfig = new NacosConfig();

}
