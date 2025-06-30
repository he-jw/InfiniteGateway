package com.infinite.gateway.common.constant;

import com.infinite.gateway.common.enums.RegisterCenterEnum;

public interface RegisterCenterConstant {

    RegisterCenterEnum REGISTER_CENTER_DEFAULT_IMPL = RegisterCenterEnum.NACOS; // 注册中心默认实现

    String REGISTER_CENTER_DEFAULT_ADDRESS = "127.0.0.1:8848"; // 默认注册中心地址

}
