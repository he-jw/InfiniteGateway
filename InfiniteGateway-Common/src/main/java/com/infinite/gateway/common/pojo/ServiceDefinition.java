package com.infinite.gateway.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务定义
 */
@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ServiceDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = -3751925283314024046L;

    /**
     * 服务名
     */
    private final String serviceName;

    /**
     * 	环境名称
     */
    private final String env;

    /**
     * 服务是否启用
     */
    private boolean enabled = true;

    /**
     * 	服务的版本号
     */
    private String version;

}
