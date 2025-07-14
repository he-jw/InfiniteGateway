package com.infinite.gateway.config.config;


import com.infinite.gateway.config.config.http.HttpClientConfig;
import com.infinite.gateway.config.config.netty.NettyConfig;
import lombok.Data;

import static com.infinite.gateway.common.constant.ConfigConstant.*;

/**
 * 网关静态配置
 */
@Data
public class Config {

    // 服务名称
    private String name = DEFAULT_NAME;
    // 端口
    private int port = DEFAULT_PORT;
    // 环境
    private String env = DEFAULT_ENV;

    // 配置中心
    private ConfigCenter configCenter = new ConfigCenter();

    // 注册中心
    private RegisterCenter registerCenter = new RegisterCenter();

    // netty
    private NettyConfig netty = new NettyConfig();

    // http client
    private HttpClientConfig httpClient = new HttpClientConfig();

}
