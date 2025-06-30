package com.infinite.gateway.config.config;


import lombok.Data;

import static com.infinite.gateway.common.constant.ConfigConstant.DEFAULT_ENV;
import static com.infinite.gateway.common.constant.ConfigConstant.DEFAULT_PORT;
import static java.lang.constant.ConstantDescs.DEFAULT_NAME;

/**
 * 网关静态配置
 */
@Data
public class Config {

    // base
    private String name = DEFAULT_NAME; // 服务名称
    private int port = DEFAULT_PORT; // 端口
    private String env = DEFAULT_ENV; // 环境

    // 配置中心
    private ConfigCenter configCenter = new ConfigCenter();

    // 注册中心
    private RegisterCenter registerCenter = new RegisterCenter();

    // netty
    private NettyConfig netty = new NettyConfig();

    // http client
    private HttpClientConfig httpClient = new HttpClientConfig();

}
