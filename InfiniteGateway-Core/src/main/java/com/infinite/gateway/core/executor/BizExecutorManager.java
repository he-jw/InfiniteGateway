package com.infinite.gateway.core.executor;

import com.infinite.gateway.dynamic.thread.pool.ThreadPoolExecutorBuilder;
import com.infinite.gateway.dynamic.thread.pool.VariableThreadPoolExecutor;
import com.infinite.gateway.dynamic.thread.pool.enums.BlockingQueueTypeEnum;
import com.infinite.gateway.dynamic.thread.pool.holder.ThreadPoolExecutorRegister;
import com.infinite.gateway.dynamic.thread.pool.properties.ThreadPoolExecutorProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 业务线程池管理器（使用动态线程池）
 */
@Slf4j
public class BizExecutorManager {

    private static volatile BizExecutorManager instance;

    /**
     * 线程池唯一标识
     */
    private static final String THREAD_POOL_ID = "gateway-biz-executor";

    /**
     * 业务线程池（动态线程池）
     */
    private VariableThreadPoolExecutor bizThreadPoolExecutor;

    /**
     * 拒绝策略处理器
     */
    private GatewayJdkRejectedExecutionHandler rejectedHandler;

    /**
     * 业务线程池配置
     */
    private int threadNum;
    private int queueSize;

    private BizExecutorManager() {
    }

    public static BizExecutorManager getInstance() {
        if (instance == null) {
            synchronized (BizExecutorManager.class) {
                if (instance == null) {
                    instance = new BizExecutorManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化业务线程池
     *
     * @param threadNum 线程数
     * @param queueSize 队列大小
     */
    public void init(int threadNum, int queueSize) {
        if (bizThreadPoolExecutor != null) {
            log.warn("BizExecutorManager already initialized, skip");
            return;
        }

        this.threadNum = threadNum;
        this.queueSize = queueSize;
        this.rejectedHandler = new GatewayJdkRejectedExecutionHandler();

        // 使用 ThreadPoolExecutorBuilder 构建动态线程池
        this.bizThreadPoolExecutor = (VariableThreadPoolExecutor) new ThreadPoolExecutorBuilder()
                .threadPoolId(THREAD_POOL_ID)
                .corePoolSize(threadNum)
                .maximumPoolSize(threadNum)  // 固定大小线程池
                .keepAliveTime(60L)
                .workQueueType(BlockingQueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE)
                .workQueueCapacity(queueSize)
                .threadFactory(THREAD_POOL_ID)
                .rejectedHandler(rejectedHandler)
                .dynamicPool(true)  // 启用动态线程池
                .build();

        // 注册到动态线程池管理器
        ThreadPoolExecutorProperties properties = ThreadPoolExecutorProperties.builder()
                .threadPoolId(THREAD_POOL_ID)
                .corePoolSize(threadNum)
                .maximumPoolSize(threadNum)
                .queueCapacity(queueSize)
                .workQueue(BlockingQueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE.getName())
                .rejectedHandler("GatewayJdkPolicy")  // 使用 SPI 注册的策略名称
                .keepAliveTime(60L)
                .allowCoreThreadTimeOut(false)
                .build();

        ThreadPoolExecutorRegister.putHolder(THREAD_POOL_ID, bizThreadPoolExecutor, properties);

        log.info("BizExecutorManager initialized with threadNum={}, queueSize={}, rejectionPolicy=GatewayJdkRejectedExecutionHandler",
                threadNum, queueSize);
    }

    /**
     * 获取业务线程池
     */
    public ThreadPoolExecutor getBizThreadPoolExecutor() {
        if (bizThreadPoolExecutor == null) {
            throw new IllegalStateException("BizExecutorManager not initialized, please call init() first");
        }
        return bizThreadPoolExecutor;
    }

    /**
     * 优雅关闭业务线程池
     */
    public void shutdown() {
        if (bizThreadPoolExecutor != null) {
            log.info("Shutting down BizExecutorManager...");
            bizThreadPoolExecutor.shutdown();
            try {
                if (!bizThreadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    bizThreadPoolExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                bizThreadPoolExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

