package com.infinite.gateway.dynamic.thread.pool;

import lombok.NonNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class VariableThreadPoolExecutor extends ThreadPoolExecutor {

    private String threadPoolId;

    private AtomicLong rejectedCount = new AtomicLong(0);

    public VariableThreadPoolExecutor(@NonNull String threadPoolId,
                                      int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory,
                                      RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.threadPoolId = threadPoolId;
    }





}
