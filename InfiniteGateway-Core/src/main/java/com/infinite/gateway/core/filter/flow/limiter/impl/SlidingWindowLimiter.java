package com.infinite.gateway.core.filter.flow.limiter.impl;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.LimitedException;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.flow.limiter.FlowLimiter;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindowLimiter implements FlowLimiter {

    private int limit = 1000;
    private int slidingWindowSize = 1000;
    private Deque<Long> queue = new ArrayDeque<>();

    public SlidingWindowLimiter(int capacity) {
        this.limit = capacity;
    }

    @Override
    public synchronized void limit(GatewayContext context) {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && now - queue.peek() > slidingWindowSize) {
            queue.pollFirst();
        }
        if (queue.size() <= limit) {
            queue.addLast(now);
            context.doFilter();
        } else {
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }
}
