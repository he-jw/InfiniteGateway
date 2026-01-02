package com.infinite.gateway.core.filter.flow.limiter.impl;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.LimitedException;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.executor.BizExecutorManager;
import com.infinite.gateway.core.filter.flow.limiter.FlowLimiter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class leakyBucketLimiter implements FlowLimiter {

    private int capacity;
    private int interval;
    private BlockingQueue<GatewayContext> taskQueue;
    /**
     * 用于从漏桶中定时取任务的固定大小线程池
     */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r);
                t.setName("leaky-bucket-scheduler");
                t.setDaemon(true);
                return t;
            });
    /**
     * 业务线程池，用于执行后续过滤器链，避免回到 Netty IO 线程
     */
    private final ThreadPoolExecutor bizExecutor = BizExecutorManager.getInstance().getBizThreadPoolExecutor();

    public leakyBucketLimiter(int capacity, int interval) {
        this.capacity = capacity;
        this.interval = interval;
        this.taskQueue = new ArrayBlockingQueue<>(capacity);
        SCHEDULER.scheduleAtFixedRate(() -> {
            GatewayContext context = taskQueue.poll();
            if (context != null) {
                bizExecutor.execute(context::doFilter);
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void limit(GatewayContext context) {
        boolean offered = taskQueue.offer(context);
        if (!offered) {
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }
}
