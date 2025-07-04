package com.infinite.gateway.config.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.gateway.config.config.ConfigCenter;
import com.infinite.gateway.config.config.NacosConfig;
import com.infinite.gateway.config.pojo.RouteDefinition;
import com.infinite.gateway.config.service.ConfigCenterProcessor;
import com.infinite.gateway.config.service.RoutesChangeListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

@Slf4j
public class NacosConfigCenterProcessor implements ConfigCenterProcessor {

    /**
     * 配置
     */
    private ConfigCenter configCenter;

    /**
     * Nacos提供的与配置中心进行交互的接口
     * Nacos SDK：https://nacos.io/docs/v2.3/guide/user/sdk/?spm=5238cd80.1f77ca18.0.0.4d31e37e5wjru3
     */
    private ConfigService configService;

    @Override
    @SneakyThrows(NacosException.class)
    public void init(ConfigCenter configCenter) {
        this.configCenter = configCenter;
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, configCenter.getAddress());
        // 创建ConfigService，连接Nacos
        this.configService = NacosFactory.createConfigService(properties);
    }

    @Override
    @SneakyThrows(NacosException.class)
    public void subscribeRoutesChange(RoutesChangeListener listener) {
        NacosConfig nacos = configCenter.getNacosConfig();
        // 1.首次启动时，先读取配置
        String configJson = configService.getConfig(nacos.getDataId(), nacos.getGroup(), nacos.getTimeout());
        /* configJson:
         * {
         *     "routes": [
         *         {
         *             "id": "user-service-route",
         *             "serviceId": "user-service",
         *             "paths": [
         *                  "/user/**",
         *                  "/user/private"
         *              ],
         *             "filters": []
         *         }
         *     ]
         * }
         */
        log.info("读取到 nacos 配置: \n{}", configJson);
        List<RouteDefinition> routes = JSON.parseObject(configJson).getJSONArray("routes").toJavaList(RouteDefinition.class);
        // 2.首次启动时，更新一次配置
        listener.onRoutesChange(routes);

        // 3.监听配置的更新
        configService.addListener(nacos.getDataId(), nacos.getGroup(), new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("读取到 nacos 配置: \n{}", configInfo);
                List<RouteDefinition> routes = JSON.parseObject(configInfo).getJSONArray("routes").toJavaList(RouteDefinition.class);
                listener.onRoutesChange(routes);
            }
        });
    }

}
