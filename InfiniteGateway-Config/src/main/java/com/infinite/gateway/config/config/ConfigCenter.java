package com.infinite.gateway.config.config;

import com.infinite.gateway.common.enums.ConfigCenterEnum;
import com.infinite.gateway.config.config.nacos.NacosConfig;
import lombok.Data;

/**
 * 配置中心
 */
@Data
public class ConfigCenter {

    private String address = "localhost:8848";

    private boolean enabled = true;

    private String type = ConfigCenterEnum.NACOS.getName();

    private NacosConfig nacosConfig = new NacosConfig();

}
