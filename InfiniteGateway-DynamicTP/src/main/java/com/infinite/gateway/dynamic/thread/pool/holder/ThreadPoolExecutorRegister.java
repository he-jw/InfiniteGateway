package com.infinite.gateway.dynamic.thread.pool.holder;

import com.infinite.gateway.dynamic.thread.pool.properties.ThreadPoolExecutorProperties;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolExecutorRegister {

    /**
     * 线程池持有者缓存，key 为线程池唯一标识，value 为线程池包装类
     */
    private static final Map<String, ThreadPoolExecutorHolder> HOLDER_MAP = new ConcurrentHashMap<>();

    /**
     * 注册线程池到管理器
     *
     * @param threadPoolId 线程池唯一标识
     * @param executor     线程池执行器实例
     * @param properties   线程池参数配置
     */
    public static void putHolder(String threadPoolId, ThreadPoolExecutor executor, ThreadPoolExecutorProperties properties) {
        ThreadPoolExecutorHolder executorHolder = new ThreadPoolExecutorHolder(threadPoolId, executor, properties);
        HOLDER_MAP.put(threadPoolId, executorHolder);
    }

    /**
     * 根据线程池 ID 获取对应的线程池包装对象
     *
     * @param threadPoolId 线程池唯一标识
     * @return 线程池持有者对象
     */
    public static ThreadPoolExecutorHolder getHolder(String threadPoolId) {
        return HOLDER_MAP.get(threadPoolId);
    }

    /**
     * 获取所有线程池集合
     *
     * @return 线程池集合
     */
    public static Collection<ThreadPoolExecutorHolder> getAllHolders() {
        return HOLDER_MAP.values();
    }

}
