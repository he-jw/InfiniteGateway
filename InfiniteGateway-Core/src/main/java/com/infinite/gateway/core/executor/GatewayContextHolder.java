package com.infinite.gateway.core.executor;

import com.infinite.gateway.core.context.GatewayContext;

/**
 * GatewayContext的ThreadLocal持有者
 * 
 * <p>用途：</p>
 * <ul>
 *   <li>在业务线程中传递GatewayContext</li>
 *   <li>在拒绝策略中获取Context以返回错误响应</li>
 *   <li>在异步回调中获取Context</li>
 * </ul>
 * 
 * <p>注意事项：</p>
 * <ul>
 *   <li>必须在使用后调用remove()，避免内存泄漏</li>
 *   <li>使用InheritableThreadLocal支持子线程继承（可选）</li>
 * </ul>
 * 
 * @author InfiniteGateway
 */
public class GatewayContextHolder {

    /**
     * 使用ThreadLocal存储GatewayContext
     * 注意：这里使用普通ThreadLocal，不使用InheritableThreadLocal
     * 因为业务线程池是固定的，不会创建子线程
     */
    private static final ThreadLocal<GatewayContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的GatewayContext
     * 
     * @param context 网关上下文
     */
    public static void set(GatewayContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程的GatewayContext
     * 
     * @return 网关上下文，如果未设置则返回null
     */
    public static GatewayContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 移除当前线程的GatewayContext
     * 
     * <p>重要：必须在请求处理完成后调用，避免内存泄漏</p>
     */
    public static void remove() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 检查当前线程是否设置了GatewayContext
     * 
     * @return true表示已设置，false表示未设置
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}

