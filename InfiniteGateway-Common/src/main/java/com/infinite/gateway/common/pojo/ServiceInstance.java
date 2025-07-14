package com.infinite.gateway.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务实例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = -7137947815268291319L;

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 实例id：ip:port
     */
    private String instanceId;

    /**
     * 服务实例 ip
     */
    private String ip;

    /**
     * 服务实例 port
     */
    private int port;

    /**
     * 权重信息
     */
    private int weight = 1;

    /**
     * 服务实例是否启用
     */
    private boolean enabled = true;

    /**
     * 服务实例是否灰度
     */
    private boolean gray;

    /**
     * 服务实例灰度比例
     */
    private double threshold;

    /**
     * 	服务注册的时间戳：后面我们做负载均衡，warmup预热
     */
    private long registerTime;

}
