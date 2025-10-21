package com.infinite.gateway.dynamic.thread.pool.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ThreadPoolExecutorProperties {

    /**
     * 线程池唯一标识
     */
    private String threadPoolId;

    /**
     * 核心线程数
     */
    private Integer corePoolSize;

    /**
     * 最大线程数
     */
    private Integer maximumPoolSize;

    /**
     * 队列容量
     */
    private Integer queueCapacity;

    /**
     * 阻塞队列类型
     */
    private String workQueue;

    /**
     * 拒绝策略类型
     */
    private String rejectedHandler;

    /**
     * 线程空闲存活时间（单位：秒）
     */
    private Long keepAliveTime;

    /**
     * 是否允许核心线程超时
     */
    private Boolean allowCoreThreadTimeOut;
}
