package com.infinite.gateway.dynamic.thread.pool.properties;

import com.infinite.gateway.common.enums.ConfigFileTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class RemoteThreadPoolExecutorProperties {

    public static final String PREFIX = "infinite";

    /**
     * 是否开启动态线程池开关
     */
    private Boolean enable = Boolean.TRUE;

    /**
     * Nacos 远程配置文件格式类型
     */
    private ConfigFileTypeEnum configFileType;

    /**
     * 线程池配置集合
     */
    private List<ThreadPoolExecutorProperties> executors;
}
