package com.infinite.gateway.core.filter.flow.limiter.impl;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.LimitedException;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.flow.limiter.FlowLimiter;
import io.netty.channel.EventLoop;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class leakyBucketLimiter implements FlowLimiter {

    private int capacity;
    private int interval;
    private BlockingQueue<GatewayContext> taskQueue;
    private EventLoop eventLoop;

    public leakyBucketLimiter(int capacity, int interval, EventLoop eventLoop) {
        this.capacity = capacity;
        this.interval = interval;
        taskQueue = new ArrayBlockingQueue<>(capacity);
        this.eventLoop = eventLoop;
        eventLoop.scheduleAtFixedRate(() -> {
            GatewayContext context = taskQueue.poll();
            if (context != null) {
                context.getNettyCtx().channel().eventLoop().execute(context::doFilter);
            }
        }, 0,interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void limit(GatewayContext context) {
        boolean offered = taskQueue.offer(context);
        if (!offered) {
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }
}
