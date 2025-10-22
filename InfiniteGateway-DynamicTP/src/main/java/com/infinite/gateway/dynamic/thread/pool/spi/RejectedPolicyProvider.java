package com.infinite.gateway.dynamic.thread.pool.spi;

import java.util.concurrent.RejectedExecutionHandler;

/**
 * 拒绝策略提供者 SPI 接口
 * 
 * <p>用于扩展自定义的拒绝策略，支持通过 ServiceLoader 机制动态加载</p>
 * 
 * <p>使用方式：</p>
 * <ol>
 *   <li>实现此接口，提供自定义的拒绝策略</li>
 *   <li>在 META-INF/services/com.infinite.gateway.dynamic.thread.pool.spi.RejectedPolicyProvider 文件中注册实现类</li>
 *   <li>系统启动时会自动加载并注册到 RejectedPolicyRegistry</li>
 * </ol>
 * 
 * @author InfiniteGateway
 */
public interface RejectedPolicyProvider {

    /**
     * 获取拒绝策略的名称（唯一标识）
     * 
     * @return 策略名称，如 "GatewayJdkPolicy"
     */
    String getPolicyName();

    /**
     * 创建拒绝策略实例
     * 
     * @return RejectedExecutionHandler 实例
     */
    RejectedExecutionHandler createHandler();

    /**
     * 获取策略的优先级（数字越小优先级越高）
     * 
     * <p>当多个 Provider 提供相同名称的策略时，优先级高的会覆盖优先级低的</p>
     * 
     * @return 优先级，默认为 100
     */
    default int getPriority() {
        return 100;
    }
}

