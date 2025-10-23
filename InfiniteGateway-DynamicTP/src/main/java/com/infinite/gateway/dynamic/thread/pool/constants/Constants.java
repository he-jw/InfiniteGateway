package com.infinite.gateway.dynamic.thread.pool.constants;

/**
 * 动态线程池常量定义
 * 
 * @author InfiniteGateway
 */
public class Constants {

    /**
     * 配置变更分隔符，用于格式化输出 "旧值 -> 新值"
     */
    public static final String CHANGE_DELIMITER = "%s -> %s";

    /**
     * 线程池配置变更日志模板
     * 参数顺序：
     * 1. 线程池ID
     * 2. 核心线程数变更
     * 3. 最大线程数变更
     * 4. 队列容量变更
     * 5. 线程存活时间变更
     * 6. 拒绝策略变更
     * 7. 是否允许核心线程超时变更
     */
    public static final String CHANGE_THREAD_POOL_TEXT = 
            "[DynamicThreadPool] 线程池配置已更新 => " +
            "线程池ID: {}, " +
            "核心线程数: {}, " +
            "最大线程数: {}, " +
            "队列容量: {}, " +
            "线程存活时间: {}, " +
            "拒绝策略: {}, " +
            "允许核心线程超时: {}";

    private Constants() {
        // 工具类，禁止实例化
    }
}

