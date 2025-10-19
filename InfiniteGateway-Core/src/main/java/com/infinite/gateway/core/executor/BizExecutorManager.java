package com.infinite.gateway.core.executor;

import io.netty.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务线程池管理器
 *
 */
@Slf4j
public class BizExecutorManager {

    private static volatile BizExecutorManager instance;

    /**
     * 业务线程池
     */
    private EventExecutorGroup bizEventExecutorGroup;

    /**
     * 拒绝策略处理器
     */
    private GatewayRejectedExecutionHandler rejectedHandler;

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
        if (bizEventExecutorGroup != null) {
            log.warn("BizExecutorManager already initialized, skip");
            return;
        }

        this.threadNum = threadNum;
        this.queueSize = queueSize;

        this.rejectedHandler = new GatewayRejectedExecutionHandler();

        // 使用Netty的DefaultEventExecutorGroup，便于与Netty Pipeline集成
        this.bizEventExecutorGroup = new DefaultEventExecutorGroup(
                threadNum,
                new DefaultThreadFactory("gateway-biz-executor"),
                queueSize,
                rejectedHandler
        );

        log.info("BizExecutorManager initialized with threadNum={}, queueSize={}, rejectionPolicy=FastFail",
                threadNum, queueSize);
    }

    /**
     * 获取业务线程池（用于Netty Pipeline绑定）
     */
    public EventExecutorGroup getBizEventExecutorGroup() {
        if (bizEventExecutorGroup == null) {
            throw new IllegalStateException("BizExecutorManager not initialized, please call init() first");
        }
        return bizEventExecutorGroup;
    }

    /**
     * 优雅关闭业务线程池
     */
    public void shutdown() {
        if (bizEventExecutorGroup != null) {
            log.info("Shutting down BizExecutorManager...");
            bizEventExecutorGroup.shutdownGracefully();
        }
    }
}

