package com.infinite.gateway.dynamic.thread.pool;

import cn.hutool.core.lang.Assert;
import com.infinite.gateway.dynamic.thread.pool.enums.BlockingQueueTypeEnum;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolExecutorBuilder {

    /**
     * 线程池唯一标识
     */
    private String threadPoolId;

    /**
     * 核心线程数
     */
    private Integer corePoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程数
     */
    private Integer maximumPoolSize = corePoolSize >> 1;

    /**
     * 阻塞队列类型
     */
    private BlockingQueueTypeEnum workQueueType = BlockingQueueTypeEnum.LINKED_BLOCKING_QUEUE;

    /**
     * 队列容量
     */
    private Integer workQueueCapacity = 4096;

    /**
     * 拒绝策略
     */
    private RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * 线程工厂
     */
    private ThreadFactory threadFactory;

    /**
     * 线程空闲存活时间（单位：秒）
     */
    private Long keepAliveTime = 30000L;

    /**
     * 是否允许核心线程超时
     */
    private boolean allowCoreThreadTimeOut = false;

    /**
     * 动态线程池标识
     */
    private boolean dynamicPool = false;

    private AtomicLong threadNum = new AtomicLong(0);

    /**
     * 最大等待时间
     */
    private long awaitTerminationMillis = 0L;

    /**
     * 设置构建线程池为动态线程池
     */
    public ThreadPoolExecutorBuilder dynamicPool(boolean b) {
        this.dynamicPool = b;
        return this;
    }

    /**
     * 设置线程池唯一标识
     *
     * @param threadPoolId 线程池唯一标识
     */
    public ThreadPoolExecutorBuilder threadPoolId(String threadPoolId) {
        this.threadPoolId = threadPoolId;
        return this;
    }

    /**
     * 设置核心线程数
     *
     * @param corePoolSize 核心线程数
     */
    public ThreadPoolExecutorBuilder corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    /**
     * 设置最大线程数
     *
     * @param maximumPoolSize 最大线程数
     */
    public ThreadPoolExecutorBuilder maximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        return this;
    }

    /**
     * 设置阻塞队列容量
     *
     * @param workQueueCapacity 阻塞队列容量
     */
    public ThreadPoolExecutorBuilder workQueueCapacity(int workQueueCapacity) {
        this.workQueueCapacity = workQueueCapacity;
        return this;
    }

    /**
     * 设置阻塞队列类型
     *
     * @param workQueueType 阻塞队列类型（如 LinkedBlockingQueue、ArrayBlockingQueue）
     */
    public ThreadPoolExecutorBuilder workQueueType(BlockingQueueTypeEnum workQueueType) {
        this.workQueueType = workQueueType;
        return this;
    }

    /**
     * 设置线程工厂
     *
     * @param namePrefix 线程名前缀
     */
    public ThreadPoolExecutorBuilder threadFactory(String namePrefix) {
        Assert.notBlank(namePrefix, "The name prefix cannot be blank.");
        this.threadFactory = r -> {
            Thread thread = new Thread(r);
            if (threadNum != null) {
                thread.setName(namePrefix + threadNum.incrementAndGet());
            }
            return thread;
        };
        return this;
    }

    /**
     * 设置拒绝策略
     *
     * @param rejectedHandler 拒绝策略（如 AbortPolicy、CallerRunsPolicy）
     */
    public ThreadPoolExecutorBuilder rejectedHandler(RejectedExecutionHandler rejectedHandler) {
        this.rejectedHandler = rejectedHandler;
        return this;
    }

    /**
     * 设置线程空闲存活时间
     *
     * @param keepAliveTime 存活时间（单位：秒）
     */
    public ThreadPoolExecutorBuilder keepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    /**
     * 设置是否允许核心线程超时
     *
     * @param allowCoreThreadTimeOut 是否允许核心线程超时
     */
    public ThreadPoolExecutorBuilder allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
        return this;
    }

    /**
     * 设置最大等待时间
     *
     * @param awaitTerminationMillis 最大等待时间
     */
    public ThreadPoolExecutorBuilder awaitTerminationMillis(long awaitTerminationMillis) {
        this.awaitTerminationMillis = awaitTerminationMillis;
        return this;
    }

    /**
     * 创建线程池构建器
     */
    public static ThreadPoolExecutorBuilder builder() {
        return new ThreadPoolExecutorBuilder();
    }

    /**
     * 构建线程池实例
     */
    public ThreadPoolExecutor build() {
        BlockingQueue<Runnable> blockingQueue = BlockingQueueTypeEnum.createBlockingQueue(workQueueType.getName(), workQueueCapacity);
        RejectedExecutionHandler rejectedHandler = Optional.ofNullable(this.rejectedHandler)
                .orElseGet(ThreadPoolExecutor.AbortPolicy::new);

        Assert.notNull(threadFactory, "The thread factory cannot be null.");

        ThreadPoolExecutor threadPoolExecutor;
        if (dynamicPool) {
            threadPoolExecutor = new VariableThreadPoolExecutor(
                    threadPoolId,
                    corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    TimeUnit.SECONDS,
                    blockingQueue,
                    threadFactory,
                    rejectedHandler
            );
        } else {
            threadPoolExecutor = new ThreadPoolExecutor(
                    corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    TimeUnit.SECONDS,
                    blockingQueue,
                    threadFactory,
                    rejectedHandler
            );
        }

        threadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        return threadPoolExecutor;
    }
}