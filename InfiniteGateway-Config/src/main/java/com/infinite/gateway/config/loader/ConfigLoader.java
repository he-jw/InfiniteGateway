package com.infinite.gateway.config.loader;

import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.utils.ConfigLoadUtil;

import static com.infinite.gateway.common.constant.ConfigConstant.CONFIG_PATH;
import static com.infinite.gateway.common.constant.ConfigConstant.CONFIG_PREFIX;

/**
 * 配置加载
 */
public class ConfigLoader {

    public static Config load(String[] args) {
        return ConfigLoadUtil.loadConfigFromYaml(CONFIG_PATH, Config.class, CONFIG_PREFIX);
    }

}
