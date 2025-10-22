package com.infinite.gateway.core.executor.spi;

import com.infinite.gateway.core.executor.GatewayJdkRejectedExecutionHandler;
import com.infinite.gateway.dynamic.thread.pool.spi.RejectedPolicyProvider;

import java.util.concurrent.RejectedExecutionHandler;

/**
 * 网关拒绝策略提供者（SPI 实现）
 * 
 * <p>通过 ServiceLoader 机制将 GatewayJdkRejectedExecutionHandler 注册到 DynamicTP 模块</p>
 * 
 * @author InfiniteGateway
 */
public class GatewayRejectedPolicyProvider implements RejectedPolicyProvider {

    @Override
    public String getPolicyName() {
        return "GatewayJdkPolicy";
    }

    @Override
    public RejectedExecutionHandler createHandler() {
        return new GatewayJdkRejectedExecutionHandler();
    }

    @Override
    public int getPriority() {
        return 50;  // 高优先级，确保覆盖默认策略
    }
}

